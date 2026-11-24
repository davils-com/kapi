/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kapi.runtime.server.webhook

import com.davils.kapi.runtime.core.structured.BareItem
import com.davils.kapi.runtime.core.structured.Dictionary
import com.davils.kapi.runtime.core.structured.InnerList
import com.davils.kapi.runtime.core.structured.Item
import com.davils.kapi.runtime.core.structured.Member
import com.davils.kapi.runtime.core.structured.Parameters
import com.davils.kapi.runtime.core.structured.StructuredFields
import com.davils.kapi.runtime.server.internal.SignatureBase
import com.davils.kapi.runtime.server.request.Request
import com.davils.kore.core.failure.attempted
import com.davils.krypt.core.algorithm.MacAlgorithm
import com.davils.krypt.core.algorithm.SignatureAlgorithm
import com.davils.krypt.primitive.Crypto
import com.davils.krypt.primitive.signature.SignatureFormat
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Instant

private const val SIGNATURE_INPUT: String = "signature-input"

private const val SIGNATURE: String = "signature"

/**
 * Verifies HTTP message signatures of RFC 9421 on a delivery, with the keys of a [SignatureKeyResolver] and the
 * primitives of krypt.
 *
 * `Signature-Input` and `Signature` are read as Dictionaries of RFC 9651 within the limits of [policy]. A signature is
 * accepted when its covered components are unique, in lower case and without parameters, are the derived components
 * `@method`, `@target-uri`, `@authority`, `@scheme`, `@request-target`, `@path` and `@query` or header fields the
 * request carries, and include every component the policy demands; when it meets the times and the tag of the
 * policy; when its `keyid` resolves to a key and its `alg`, if any, names the algorithm of that key; and when the
 * signature over the signature base of RFC 9421 §2.5 verifies, a MAC in constant time. A delivery is accepted when one
 * of its signatures is. Every other component, such as `@query-param` or a field with the parameter `sf`, makes a
 * signature fail rather than be read in a way the sender did not mean. Unless [SignaturePolicy.isContentBound] is off,
 * a delivery with content also needs a `Content-Digest` that matches its bytes, checked like [ContentDigestVerifier]
 * does, and a signature that covers it, so the signature binds the body. A [SignaturePolicy.replays] guard then
 * refuses a signature it has seen before.
 *
 * @param keys finds the key of a `keyid`.
 * @param crypto the crypto of the verification, whose policy may refuse an algorithm or a key.
 * @param policy what a signature must meet besides being valid.
 * @since 1.0.0
 */
public class MessageSignatureVerifier(
    private val keys: SignatureKeyResolver,
    private val crypto: Crypto,
    private val policy: SignaturePolicy = SignaturePolicy.Default,
) : DeliveryVerifier {
    override suspend fun verify(request: Request, content: ByteString): Verdict {
        val inputs = dictionaryOf(request, SIGNATURE_INPUT)
        val signatures = dictionaryOf(request, SIGNATURE)
        val candidates = inputs?.entries.orEmpty().filter { (label, _) ->
            policy.label == null || label == policy.label
        }
        val hasContent = policy.isContentBound && content.size > 0
        val isBound = !hasContent || isDigestMatching(request, content)
        val signed = signatures?.takeIf { isBound }?.let { found ->
            candidates.firstNotNullOfOrNull { (label, member) ->
                signedOf(request = request, member = member, signature = found[label], hasContent = hasContent)
            }
        }
        val isFresh = signed?.let { found -> isFirstUse(found) } != false
        val reason = UNVERIFIED.takeIf { signed == null } ?: REPLAYED.takeUnless { isFresh }
        return reason?.let(Verdict::Refused) ?: Verdict.Accepted
    }

    override fun toString(): String = "MessageSignatureVerifier($policy)"

    private fun dictionaryOf(request: Request, name: String): Dictionary? {
        val values = request.headers.all(name)
        val text = values.joinToString(", ").takeIf { values.isNotEmpty() }
        return text?.let { value -> StructuredFields.parseDictionaryOrNull(value, policy.limits) }
    }

    private suspend fun isDigestMatching(request: Request, content: ByteString): Boolean =
        ContentDigestVerifier(crypto, policy.limits).verify(request, content) == Verdict.Accepted

    private suspend fun isFirstUse(signed: Signed): Boolean {
        val guard = policy.replays ?: return true
        return attempted { guard.isFirstUse(signed.bytes, signed.until) }.getOrNull() == true
    }

    private suspend fun signedOf(request: Request, member: Member, signature: Member?, hasContent: Boolean): Signed? {
        val covered = (member as? InnerList)?.takeIf { list -> isAllowed(list, hasContent) }
        val bytes = ((signature as? Item)?.value as? BareItem.Bytes)?.value
        val keyId = covered?.let { list -> text(list.parameters, KEY_ID) }
        val base = covered?.let { list -> SignatureBase.of(request, list) }
        val isComplete = bytes != null && keyId != null && base != null
        val key = keyId?.takeIf { isComplete }?.let { id -> attempted { keys.resolve(id) }.getOrNull() }
        val algorithm = covered?.let { list -> text(list.parameters, ALGORITHM) }
        val isMatching = key != null && (algorithm == null || algorithm == key.algorithm.id)
        val signed = bytes ?: ByteString()
        val isVerified = key?.takeIf { isMatching }?.let { found ->
            val content = base.orEmpty().encodeToByteString()
            attempted { isSigned(key = found, base = content, signature = signed) }.getOrNull()
        }
        val until = covered?.takeIf { isVerified == true }?.let { list -> untilOf(list.parameters) }
        return until?.let { instant -> Signed(bytes = signed, until = instant) }
    }

    private fun isAllowed(covered: InnerList, hasContent: Boolean): Boolean {
        val names = covered.items.map(SignatureBase::nameOf)
        val parameters = covered.parameters
        val now = policy.clock.now().epochSeconds
        val skew = policy.clockSkew.inWholeSeconds
        val created = number(parameters, CREATED)
        val expires = number(parameters, EXPIRES)
        val isCreatedValid = created == null || (created <= now + skew && now - created <= policy.maxAge.inWholeSeconds)
        val isCreatedPresent = created != null || !policy.isCreatedRequired
        val isUnexpired = expires == null || expires + skew >= now
        val isTagged = policy.tag == null || text(parameters, TAG) == policy.tag
        val isCovering = names.containsAll(policy.requiredComponents) && (!hasContent || CONTENT_DIGEST in names)
        return isCreatedValid && isCreatedPresent && isUnexpired && isTagged && isCovering
    }

    private fun untilOf(parameters: Parameters): Instant {
        val createdAt = number(parameters, CREATED)?.let(Instant::fromEpochSeconds)
        val created = createdAt?.plus(policy.maxAge)
        val expires = number(parameters, EXPIRES)?.let(Instant::fromEpochSeconds)
        val latest = listOfNotNull(created, expires).minOrNull() ?: Instant.DISTANT_FUTURE
        return latest + policy.clockSkew
    }

    private data class Signed(val bytes: ByteString, val until: Instant)

    private suspend fun isSigned(key: VerificationKey, base: ByteString, signature: ByteString): Boolean {
        val verification = when (key) {
            is VerificationKey.Shared -> {
                crypto.macs.verify(key = key.key, data = base, tag = signature, algorithm = MacAlgorithm.HmacSha256)
            }

            is VerificationKey.Public -> {
                crypto.signatures.verify(
                    key = key.key,
                    data = base,
                    signature = signature,
                    algorithm = signatureAlgorithmOf(key.algorithm),
                    format = SignatureFormat.Raw,
                )
            }
        }
        return verification.isValid
    }

    private companion object {
        const val UNVERIFIED: String = "No signature of the delivery verifies under the policy"
        const val REPLAYED: String = "The signature of the delivery was accepted before"
        const val CONTENT_DIGEST: String = "content-digest"
        const val KEY_ID: String = "keyid"
        const val ALGORITHM: String = "alg"
        const val CREATED: String = "created"
        const val EXPIRES: String = "expires"
        const val TAG: String = "tag"

        val algorithms: Map<MessageSignatureAlgorithm, SignatureAlgorithm> = mapOf(
            MessageSignatureAlgorithm.RsaPssSha512 to SignatureAlgorithm.RsaPssSha512,
            MessageSignatureAlgorithm.RsaV15Sha256 to SignatureAlgorithm.RsaPkcs1Sha256,
            MessageSignatureAlgorithm.EcdsaP256Sha256 to SignatureAlgorithm.EcdsaSha256,
            MessageSignatureAlgorithm.EcdsaP384Sha384 to SignatureAlgorithm.EcdsaSha384,
            MessageSignatureAlgorithm.Ed25519 to SignatureAlgorithm.Ed25519,
        )

        fun signatureAlgorithmOf(algorithm: MessageSignatureAlgorithm): SignatureAlgorithm =
            algorithms.getValue(algorithm)

        fun text(parameters: Parameters, key: String): String? = (parameters[key] as? BareItem.Text)?.value

        fun number(parameters: Parameters, key: String): Long? = (parameters[key] as? BareItem.Integer)?.value
    }
}
