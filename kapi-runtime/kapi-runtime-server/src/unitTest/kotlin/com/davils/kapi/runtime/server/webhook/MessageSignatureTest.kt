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

import com.davils.kapi.runtime.server.fixture.crypto
import com.davils.kapi.runtime.server.fixture.requestOf
import com.davils.kapi.runtime.server.request.Request
import com.davils.kore.core.validation.report.ValidationException
import com.davils.krypt.core.SecretBytes
import com.davils.krypt.core.algorithm.KeyType
import com.davils.krypt.core.algorithm.SignatureAlgorithm
import com.davils.krypt.primitive.key.SecretKey
import com.davils.krypt.primitive.signature.SignatureFormat
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private const val AUTHORITY: String = """"@authority""""

private const val CREATED: Long = 1_618_884_473L

private const val KEY_ID: String = "test-shared-secret"

private const val SHARED: String =
    "uzvJfB4u3N0Jy4T7NZ75MDVcr8zSTInedJtkgcu46YW4XByzNJjxBdtjUkdJPBtbmHhIDi6pcl8jsasjlTMtDQ=="

private const val INPUT: String = """sig-b25=("date" "@authority" "content-type")""" +
    """;created=1618884473;keyid="test-shared-secret""""

private const val SIGNATURE: String = "sig-b25=:pxcQw6G3AjtMBQjwo8XzkZf/bws5LelbaMk5rGIGtE8=:"

private const val HOUR: Long = 3_600L

private const val CREATED_BY_KEY: String = ";created=$CREATED;keyid=\"$KEY_ID\""

private val shared: VerificationKey.Shared = VerificationKey.Shared(
    SecretKey.of(SecretBytes.copyOf(Base64.decode(SHARED))),
)

private fun clockAt(seconds: Long): Clock = object : Clock {
    override fun now(): Instant = Instant.fromEpochSeconds(seconds)
}

private fun policyAt(seconds: Long = CREATED, configure: SignaturePolicyBuilder.() -> Unit = {}): SignaturePolicy =
    signaturePolicy {
        clock = clockAt(seconds)
        requiredComponents = setOf("@authority")
        configure()
    }

private fun verifier(policy: SignaturePolicy = policyAt(), key: VerificationKey? = shared): MessageSignatureVerifier =
    MessageSignatureVerifier(
        keys = { id -> key.takeIf { id == KEY_ID } },
        crypto = crypto,
        policy = policy,
    )

private fun delivery(input: String? = INPUT, signature: String? = SIGNATURE, digest: String? = null): Request {
    val headers = mapOf(
        "Date" to listOf("Tue, 20 Apr 2021 02:07:55 GMT"),
        "Content-Type" to listOf("application/json"),
        "Content-Digest" to listOfNotNull(digest),
        "Signature-Input" to listOfNotNull(input),
        "Signature" to listOfNotNull(signature),
    )
    return requestOf(
        headers = headers.filterValues(List<String>::isNotEmpty),
        path = "/foo",
        rawQuery = "param=Value&Pet=dog",
    )
}

private suspend fun signed(components: String, parameters: String, base: String, digest: String? = null): Request {
    val signature = crypto.macs.mac(shared.key, base.encodeToByteString())
    val encoded = Base64.encode(signature.toByteArray())
    return delivery(input = "s=($components)$parameters", signature = "s=:$encoded:", digest = digest)
}

private suspend fun bound(digest: String): Request {
    val parameters = CREATED_BY_KEY
    val components = "$AUTHORITY \"content-digest\""
    val base = lines(
        """"@authority": example.com""",
        """"content-digest": $digest""",
        """"@signature-params": ($components)$parameters""",
    )
    return signed(components = components, parameters = parameters, base = base, digest = digest)
}

private fun remembering(seen: MutableSet<ByteString>, until: MutableList<Instant>): MessageSignatureVerifier {
    val guard = ReplayGuard { signature, instant ->
        until += instant
        seen.add(signature)
    }
    return verifier(
        policyAt {
            isCreatedRequired = false
            replays = guard
        },
    )
}

private const val TARGET: String = """"@target-uri": https://example.com/foo?param=Value&Pet=dog"""

private fun lines(vararg lines: String): String = lines.joinToString("\n")

private fun authorityBase(parameters: String): String =
    lines(""""@authority": example.com""", """"@signature-params": ("@authority")$parameters""")

class MessageSignatureTest : FunSpec({
    context("the signature of RFC 9421 §B.2.5") {
        test("verifies with the shared secret of the example") {
            verifier().verify(delivery(), ByteString()) shouldBe Verdict.Accepted
        }

        test("is refused with another key, a changed field, no key or a key the resolver fails on") {
            val other = VerificationKey.Shared(crypto.keys.generateSecret())
            val failing = MessageSignatureVerifier(keys = { error("store down") }, crypto = crypto, policy = policyAt())

            verifier(key = other).verify(delivery(), ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
            verifier(key = null).verify(delivery(), ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
            failing.verify(delivery(), ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
            verifier().verify(
                delivery(input = INPUT.replace(oldValue = "test-shared", newValue = "test-other")),
                ByteString(),
            ) shouldBe
                Verdict.Refused(UNVERIFIED)
        }

        test("is refused when it is too old, from the future, expired, untagged or lacks a demanded component") {
            val expired = INPUT + ";expires=${CREATED + 1}"

            verifier(policyAt(CREATED + HOUR)).verify(delivery(), ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
            verifier(policyAt(CREATED - HOUR)).verify(delivery(), ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
            verifier(policyAt(CREATED + HOUR) { maxAge = Duration.INFINITE.div(2) }).verify(
                delivery(input = expired, signature = SIGNATURE),
                ByteString(),
            ) shouldBe Verdict.Refused(UNVERIFIED)
            verifier(policyAt { tag = "app" }).verify(delivery(), ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
            verifier(policyAt { requiredComponents = setOf("@method") }).verify(delivery(), ByteString()) shouldBe
                Verdict.Refused(UNVERIFIED)
        }

        test("is refused when its label is not the one the policy names, and found when it is") {
            verifier(policyAt { label = "other" }).verify(delivery(), ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
            verifier(policyAt { label = "sig-b25" }).verify(delivery(), ByteString()) shouldBe Verdict.Accepted
        }

        test("is refused when the fields are missing or malformed") {
            verifier().verify(delivery(input = null), ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
            verifier().verify(delivery(signature = null), ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
            verifier().verify(delivery(input = "sig-b25=("), ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
            verifier().verify(delivery(signature = "sig-b25=\"text\""), ByteString()) shouldBe
                Verdict.Refused(UNVERIFIED)
            verifier().verify(delivery(input = "sig-b25=\"item\""), ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
        }
    }

    context("a signature over derived components") {
        test("covers the method, the target, the scheme, the request target, the path and the query") {
            val components = """"@method" "@target-uri" "@scheme" "@request-target" "@path" "@query""""
            val parameters = """;created=$CREATED;keyid="$KEY_ID";alg="hmac-sha256";tag="app""""
            val base = lines(
                """"@method": POST""",
                TARGET,
                """"@scheme": https""",
                """"@request-target": /foo?param=Value&Pet=dog""",
                """"@path": /foo""",
                """"@query": ?param=Value&Pet=dog""",
                """"@signature-params": ($components)$parameters""",
            )
            val request = signed(components, parameters, base)

            val policy = policyAt {
                tag = "app"
                requiredComponents = setOf("@method")
            }

            verifier(policy).verify(request, ByteString()) shouldBe Verdict.Accepted
        }

        test("is refused for a component it does not read, one with parameters, one named twice or in upper case") {
            val parameters = CREATED_BY_KEY
            listOf(
                """"@authority" "@query-param";name="a"""",
                """"@authority" "date";sf""",
                """"@authority" "@authority"""",
                """"@authority" "Date"""",
                """"@authority" "x-missing"""",
            ).forEach { components ->
                val request = signed(components, parameters, "anything")

                verifier().verify(request, ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
            }
        }

        test("is refused when its algorithm is not the one of its key") {
            val parameters = """;created=$CREATED;keyid="$KEY_ID";alg="ed25519""""
            val base = authorityBase(parameters)

            verifier().verify(signed(AUTHORITY, parameters, base), ByteString()) shouldBe
                Verdict.Refused(UNVERIFIED)
        }

        test("without created is accepted only when the policy allows it") {
            val parameters = ";keyid=\"$KEY_ID\""
            val base = authorityBase(parameters)
            val request = signed(AUTHORITY, parameters, base)

            verifier().verify(request, ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
            verifier(policyAt { isCreatedRequired = false }).verify(request, ByteString()) shouldBe Verdict.Accepted
        }
    }

    context("a signature with a public key") {
        listOf(
            Triple(KeyType.Ed25519, MessageSignatureAlgorithm.Ed25519, SignatureAlgorithm.Ed25519),
            Triple(KeyType.P256, MessageSignatureAlgorithm.EcdsaP256Sha256, SignatureAlgorithm.EcdsaSha256),
        ).forEach { (type, algorithm, primitive) ->
            test("verifies with ${algorithm.id}") {
                val pair = crypto.keys.generate(type)
                val key = VerificationKey.Public(pair.publicKey, algorithm)
                val parameters = CREATED_BY_KEY
                val base = authorityBase(parameters)
                val signature = crypto.signatures.sign(
                    key = pair.privateKey,
                    data = base.encodeToByteString(),
                    algorithm = primitive,
                    format = SignatureFormat.Raw,
                )
                val request = delivery(
                    input = "s=(\"@authority\")$parameters",
                    signature = "s=:${Base64.encode(signature.toByteArray())}:",
                )

                verifier(key = key).verify(request, ByteString()) shouldBe Verdict.Accepted
                key.toString() shouldBe "Public(${algorithm.id})"
            }
        }

        test("refuses to stand for an HMAC") {
            val pair = crypto.keys.generate(KeyType.Ed25519)

            shouldThrow<IllegalArgumentException> {
                VerificationKey.Public(pair.publicKey, MessageSignatureAlgorithm.HmacSha256)
            }
        }
    }

    context("a policy") {
        test("refuses a maximum age that is not positive and a clock skew that is negative") {
            val failure = shouldThrow<ValidationException> {
                signaturePolicy {
                    maxAge = Duration.ZERO
                    clockSkew = Duration.INFINITE.unaryMinus()
                }
            }

            failure.report.violations.size shouldBe 2
        }

        test("shows its demands") {
            SignaturePolicy.Default.toString() shouldContain "@target-uri"
            verifier().toString() shouldContain "MessageSignatureVerifier"
            shared.toString() shouldBe "Shared(512 bits)"
            shared.algorithm shouldBe MessageSignatureAlgorithm.HmacSha256
        }
    }

    context("a signature over a delivery with content") {
        test("is accepted only when it covers a Content-Digest that matches the content") {
            val content = BODY.encodeToByteString()
            val request = bound(DIGEST)

            verifier().verify(request, content) shouldBe Verdict.Accepted
            verifier().verify(request, OTHER_BODY.encodeToByteString()) shouldBe Verdict.Refused(UNVERIFIED)
            verifier().verify(delivery(digest = DIGEST), content) shouldBe Verdict.Refused(UNVERIFIED)
            verifier(policyAt { isContentBound = false }).verify(delivery(), content) shouldBe Verdict.Accepted
        }
    }

    context("a signature the policy remembers") {
        test("is accepted once and refused when it comes again or the guard fails") {
            val seen = mutableSetOf<ByteString>()
            val until = mutableListOf<Instant>()
            val failing = verifier(policyAt { replays = ReplayGuard { _, _ -> error("cache down") } })

            remembering(seen, until).verify(delivery(), ByteString()) shouldBe Verdict.Accepted
            remembering(seen, until).verify(delivery(), ByteString()) shouldBe Verdict.Refused(REPLAYED)
            failing.verify(delivery(), ByteString()) shouldBe Verdict.Refused(REPLAYED)
            until.first() shouldBe Instant.fromEpochSeconds(CREATED) + SIX_MINUTES.minutes
        }

        test("is remembered until it expires, or for good when it names no time") {
            val seen = mutableSetOf<ByteString>()
            val until = mutableListOf<Instant>()
            val expiring = ";created=$CREATED;expires=${CREATED + 1};keyid=\"$KEY_ID\""
            val timeless = ";keyid=\"$KEY_ID\""

            val expiringRequest = signed(components = AUTHORITY, parameters = expiring, base = authorityBase(expiring))
            val timelessRequest = signed(components = AUTHORITY, parameters = timeless, base = authorityBase(timeless))

            remembering(seen, until).verify(expiringRequest, ByteString()) shouldBe Verdict.Accepted
            remembering(seen, until).verify(timelessRequest, ByteString()) shouldBe Verdict.Accepted
            until shouldBe listOf(
                Instant.fromEpochSeconds(CREATED + 1) + 1.minutes,
                Instant.DISTANT_FUTURE + 1.minutes,
            )
        }
    }

    context("a signature with an expiry and a signature that is no byte sequence") {
        test("is accepted before its expiry and refused when the signature is a list") {
            val parameters = ";created=$CREATED;expires=${CREATED + HOUR};keyid=\"$KEY_ID\""
            val base = authorityBase(parameters)
            val request = signed(AUTHORITY, parameters, base)

            verifier().verify(request, ByteString()) shouldBe Verdict.Accepted
            verifier().verify(delivery(signature = "sig-b25=(1 2)"), ByteString()) shouldBe Verdict.Refused(UNVERIFIED)
        }
    }
})

private const val UNVERIFIED: String = "No signature of the delivery verifies under the policy"

private const val REPLAYED: String = "The signature of the delivery was accepted before"

private const val BODY: String = """{"hello": "world"}"""

private const val OTHER_BODY: String = """{"hello": "there"}"""

private const val DIGEST: String = "sha-256=:X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=:"

private const val SIX_MINUTES: Int = 6
