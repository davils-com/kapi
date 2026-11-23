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

package com.davils.kapi.runtime.server

import com.davils.kapi.runtime.core.codec.multipart.Boundary
import com.davils.kapi.runtime.core.codec.multipart.MultipartLimits
import com.davils.kapi.runtime.core.destination.IpAddress
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kapi.runtime.server.channel.MessageCodec
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.fixture.certificateOf
import com.davils.kapi.runtime.server.fixture.requestOf
import com.davils.kapi.runtime.server.internal.BodySource
import com.davils.kapi.runtime.server.operation.CredentialSource
import com.davils.kapi.runtime.server.operation.SchemeUse
import com.davils.kapi.runtime.server.problem.ProblemSettings
import com.davils.kapi.runtime.server.problem.ProblemSettingsBuilder
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.reply.ReplyBody
import com.davils.kapi.runtime.server.request.BodyRefusedException
import com.davils.kapi.runtime.server.request.MultipartUpload
import com.davils.kapi.runtime.server.request.ParameterType
import com.davils.kapi.runtime.server.request.Peer
import com.davils.kapi.runtime.server.request.PeerIdentity
import com.davils.kapi.runtime.server.request.Upload
import com.davils.kapi.runtime.server.security.Authorization
import com.davils.kapi.runtime.server.security.PresentedCredential
import com.davils.kapi.runtime.server.telemetry.ServerEvent
import com.davils.kapi.runtime.server.webhook.DeliveryVerifier
import com.davils.kapi.runtime.server.webhook.SignaturePolicy
import com.davils.kapi.runtime.server.webhook.Verdict
import com.davils.kapi.runtime.server.webhook.signaturePolicy
import com.davils.kore.core.principal.principal
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.builtins.serializer
import kotlin.time.Duration.Companion.seconds

private const val OAUTH: String = "oauth"

private const val NORMAL_CLOSE: Int = 1_000

private const val OK: Int = 200

private const val PROCESS: Long = 7L

private const val TOO_LONG: Int = 1_025

private const val UPPER_STATUS: Int = 600

private val address: IpAddress = IpAddress.parse("10.0.0.1")

private fun shouldDifferIn(vararg values: Any) {
    val neighbours = values.toList().zipWithNext()
    neighbours.forEach { (first, second) -> first shouldNotBe second }
    values.forEach { value -> value shouldNotBe "other" }
}

class ValuesTest : FunSpec({
    context("the values of a request") {
        test("are equal by their parts and show no secret") {
            val chain = listOf(certificateOf("a"))

            Peer(address, chain, PeerIdentity("u")) shouldBe Peer(address, chain, PeerIdentity("u"))
            Peer(address).hashCode() shouldBe Peer(address).hashCode()
            shouldDifferIn(Peer(address), Peer(), Peer(address, chain))
            Peer().toString() shouldBe "Peer(address=none, certificates=0, identity=none)"
            PeerIdentity("u", PROCESS) shouldBe PeerIdentity("u", PROCESS)
            PeerIdentity("u").hashCode() shouldBe PeerIdentity("u").hashCode()
            shouldDifferIn(PeerIdentity("u"), PeerIdentity("v"), PeerIdentity("v", PROCESS))
            PeerIdentity("u").toString() shouldBe "PeerIdentity(u, none)"
            ParameterType(Int.serializer(), isQuoted = false) shouldBe ParameterType(Int.serializer(), isQuoted = false)
            ParameterType(Int.serializer(), isQuoted = false).hashCode() shouldBe
                ParameterType(Int.serializer(), isQuoted = false).hashCode()
            shouldDifferIn(ParameterType(Int.serializer(), isQuoted = false), ParameterType(Int.serializer(), true))
            ParameterType(Int.serializer(), isQuoted = false).toString() shouldBe
                "ParameterType(kotlin.Int, isQuoted=false)"
        }

        test("of uploads show their type and length, never their content") {
            val source = BodySource.buffered("secret".encodeToByteString(), ServerLimits.Default)
            val upload = Upload(contentType = MediaType.OctetStream, length = null, source = source, limit = 1L)
            val sized = Upload(contentType = MediaType.OctetStream, length = 6L, source = source, limit = 1L)

            upload.toString() shouldBe "Upload(application/octet-stream, unknown bytes)"
            sized.toString() shouldBe "Upload(application/octet-stream, 6 bytes)"
            shouldThrow<BodyRefusedException> { sized.read { } }.code shouldBe ServerCodes.TOO_LARGE
            MultipartUpload(
                boundary = Boundary("b"),
                limits = MultipartLimits.Default,
                source = source,
                limit = 1L,
            ).toString() shouldBe
                "MultipartUpload(b)"
            val input = requestOf().input()

            input.toString() shouldBe "Input(probe, violations=0)"
            requestOf().contentType shouldBe null
        }
    }

    context("the values of an operation") {
        test("are equal by their parts and show their names escaped") {
            CredentialSource.Header("X-Key") shouldBe CredentialSource.Header("x-key")
            CredentialSource.Header("X-Key").hashCode() shouldBe CredentialSource.Header("x-key").hashCode()
            CredentialSource.Query("k") shouldBe CredentialSource.Query("k")
            CredentialSource.Query("k").hashCode() shouldBe "k".hashCode()
            CredentialSource.Cookie("k") shouldBe CredentialSource.Cookie("k")
            CredentialSource.Cookie("k").hashCode() shouldBe "k".hashCode()
            shouldDifferIn(
                CredentialSource.Header("k"),
                CredentialSource.Query("k"),
                CredentialSource.Cookie("k"),
                CredentialSource.Query("j"),
            )
            shouldDifferIn(CredentialSource.Cookie("k"), CredentialSource.Cookie("j"))
            shouldDifferIn(CredentialSource.Header("k"), CredentialSource.Header("j"))
            listOf(
                CredentialSource.Bearer,
                CredentialSource.Basic,
                CredentialSource.Header("h"),
                CredentialSource.Query("q"),
                CredentialSource.Cookie("c"),
                CredentialSource.Certificate,
            ).map(Any::toString) shouldBe listOf("Bearer", "Basic", "Header(h)", "Query(q)", "Cookie(c)", "Certificate")
            val use = SchemeUse(OAUTH, CredentialSource.Bearer, setOf("a"))

            use shouldBe SchemeUse(OAUTH, CredentialSource.Bearer, setOf("a"))
            use.hashCode() shouldBe SchemeUse(OAUTH, CredentialSource.Bearer, setOf("a")).hashCode()
            shouldDifferIn(use, SchemeUse(OAUTH, CredentialSource.Bearer))
            use.toString() shouldBe "SchemeUse(oauth, Bearer, [a])"
        }
    }

    context("the values of security") {
        test("are equal by their parts") {
            val chain = listOf(certificateOf("a"))

            PresentedCredential.Token(Secret("t")) shouldBe PresentedCredential.Token(Secret("t"))
            PresentedCredential.Token(Secret("t")).hashCode() shouldBe Secret("t").hashCode()
            PresentedCredential.Key(Secret("k")) shouldBe PresentedCredential.Key(Secret("k"))
            PresentedCredential.Key(Secret("k")).hashCode() shouldBe Secret("k").hashCode()
            PresentedCredential.Basic("u", Secret("p")) shouldBe PresentedCredential.Basic("u", Secret("p"))
            PresentedCredential.Certificates(chain) shouldBe PresentedCredential.Certificates(chain)
            PresentedCredential.Certificates(chain).hashCode() shouldBe chain.hashCode()
            PresentedCredential.Peer(PeerIdentity("u")).hashCode() shouldBe PeerIdentity("u").hashCode()
            shouldDifferIn(
                PresentedCredential.Token(Secret("t")),
                PresentedCredential.Key(Secret("t")),
                PresentedCredential.Basic("t", Secret("t")),
                PresentedCredential.Certificates(chain),
                PresentedCredential.Peer(PeerIdentity("t")),
                PresentedCredential.Token(Secret("u")),
            )
            shouldDifferIn(
                PresentedCredential.Key(Secret("t")),
                PresentedCredential.Key(Secret("u")),
                PresentedCredential.Peer(PeerIdentity("t")),
                PresentedCredential.Peer(PeerIdentity("u")),
                PresentedCredential.Certificates(emptyList()),
            )
            shouldDifferIn(
                Authorization.Granted(principal("a")),
                Authorization.Granted(principal("b")),
                Authorization.Denied("e"),
                Authorization.Denied(error = "e", description = "d"),
                Authorization.Forbidden(setOf("a")),
                Authorization.Forbidden(),
            )
            Authorization.Denied("e").toString() shouldBe "Denied(e, none)"
            Authorization.Denied(null, "d").toString() shouldBe "Denied(none, d)"
        }
    }

    context("the values of a reply") {
        test("are equal by their parts") {
            val bytes = ReplyBody.Bytes(MediaType.Json, ByteString())

            bytes shouldBe ReplyBody.Bytes(MediaType.Json, ByteString())
            bytes.hashCode() shouldBe ReplyBody.Bytes(MediaType.Json, ByteString()).hashCode()
            shouldDifferIn(bytes, ReplyBody.Bytes(MediaType.Cbor, ByteString()), ReplyBody.Empty)
            shouldDifferIn(Reply(OK), Reply(OK, listOf("A" to "b")), Reply(OK, body = bytes), Reply(OK + 1))
            ReplyBody.Empty.toString() shouldBe "Empty"
            shouldThrow<IllegalArgumentException> { Reply(UPPER_STATUS) }
        }

        test("of channels are equal by their parts") {
            val codec = MessageCodec(Note.serializer())

            shouldDifferIn(codec, MessageCodec(Note.serializer(), MediaType.Cbor), MessageCodec(String.serializer()))
            codec.isText shouldBe true
            MessageCodec(Note.serializer(), MediaType.Cbor).isText shouldBe false
        }
    }

    context("the settings of a server") {
        test("refuse a base of problem types that is no URI of visible characters, and show it") {
            ProblemSettings.Default.toString() shouldBe "ProblemSettings(typeBase=none)"
            listOf("", "a b", "a\"b", "a\\b", "é", "x".repeat(TOO_LONG)).forEach { base ->
                shouldThrow<ValidationException> { problemsWith(base) }
            }
            problemsWith("urn:p:").toString() shouldBe
                "ProblemSettings(typeBase=urn:p:)"
        }

        test("refuse limits outside their bounds and show them") {
            shouldThrow<ValidationException> {
                limitsWith(requestBytes = 0L, queryLength = ServerLimits.HARD_QUERY_LENGTH + 1)
            }.report.violations.size shouldBe 2
            ServerLimits.Default.toString() shouldContain "requestBytes=4194304"
            isRealm("") shouldBe false
            isRealm("a\u0001") shouldBe false
            isRealm("x".repeat(TOO_LONG)) shouldBe false
            val plain = ServerSettingsBuilder().build()

            plain.toString() shouldContain "verifier=false"
            isRealm("é") shouldBe false
            val verified = ServerSettingsBuilder().apply { verifier = DeliveryVerifier { _, _ -> Verdict.Accepted } }

            verified.build().toString() shouldContain "verifier=true"
        }

        test("of signatures and events show what they hold") {
            SignaturePolicy.Default.toString() shouldContain "label=any"
            signaturePolicy { label = "s" }
                .toString() shouldContain "label=s"
            val served = ServerEvent.Served(operation = "op", method = "GET", status = OK, duration = 1.seconds)
            val failed = ServerEvent.Failed(operation = "op", failureType = "Boom")

            served.toString() shouldBe "Served(op, GET, 200, 1s)"
            failed.toString() shouldBe "Failed(op, Boom)"
            ServerEvent.ChannelOpened("c", null).toString() shouldBe "ChannelOpened(c, none)"
            ServerEvent.ChannelClosed("c", NORMAL_CLOSE, 1.seconds).toString() shouldBe "ChannelClosed(c, 1000, 1s)"
        }
    }
})

private fun problemsWith(base: String): ProblemSettings {
    val builder = ProblemSettingsBuilder()
    builder.typeBase = base
    return builder.build()
}

private fun isRealm(realm: String): Boolean {
    val builder = ServerSettingsBuilder()
    builder.realm = realm
    return builder.validate().isValid
}

private fun limitsWith(requestBytes: Long, queryLength: Int): ServerLimits {
    val builder = ServerLimitsBuilder()
    builder.maxRequestBytes = requestBytes
    builder.maxQueryLength = queryLength
    return builder.build()
}
