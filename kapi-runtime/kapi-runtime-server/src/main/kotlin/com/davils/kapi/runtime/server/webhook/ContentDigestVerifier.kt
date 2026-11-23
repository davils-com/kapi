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

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.digest.DigestAlgorithm
import com.davils.kapi.runtime.core.digest.DigestFields
import com.davils.kapi.runtime.server.request.Request
import com.davils.krypt.core.ConstantTime
import com.davils.krypt.primitive.Crypto
import kotlinx.io.bytestring.ByteString
import com.davils.krypt.core.algorithm.DigestAlgorithm as KryptDigest

private const val CONTENT_DIGEST: String = "content-digest"

/**
 * Verifies `Content-Digest` of RFC 9530 §2 against the exact bytes of a delivery, with the digests of krypt.
 *
 * The field is read within [limits]; the digest of the strongest active algorithm, SHA-512 before SHA-256, is
 * computed and compared in constant time. A delivery is refused when the field is missing or malformed, when it names
 * only deprecated or unknown algorithms, such as MD5 or SHA-1, which RFC 9530 §5 marks as insecure, and when the digest
 * differs. Combined with [MessageSignatureVerifier] that covers `content-digest`, it binds a signature to the body.
 *
 * @param crypto the crypto of the digests.
 * @param limits the limits of the field.
 * @since 1.0.0
 */
public class ContentDigestVerifier(
    private val crypto: Crypto,
    private val limits: FieldLimits = FieldLimits.Default,
) : DeliveryVerifier {
    override suspend fun verify(request: Request, content: ByteString): Verdict {
        val values = request.headers.all(CONTENT_DIGEST)
        val text = values.joinToString(", ").takeIf { values.isNotEmpty() }
        val digests = text
            ?.let { text -> DigestFields.parseDigests(text, limits) }
        val strongest = digests?.let(DigestFields::strongest) ?: return Verdict.Refused(MISSING)
        val (algorithm, expected) = strongest
        val actual = crypto.digests.digest(content, kryptOf(algorithm))
        return Verdict.Accepted.takeIf { ConstantTime.equals(first = expected, second = actual) } ?: Verdict.Refused(
            MISMATCH,
        )
    }

    override fun toString(): String = "ContentDigestVerifier"

    private companion object {
        const val MISSING: String = "The delivery carries no Content-Digest of an active algorithm"
        const val MISMATCH: String = "The Content-Digest does not match the content"

        fun kryptOf(algorithm: DigestAlgorithm): KryptDigest {
            if (algorithm == DigestAlgorithm.Sha512) return KryptDigest.Sha512
            return KryptDigest.Sha256
        }
    }
}
