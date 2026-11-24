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

package com.davils.kapi.runtime.client

import com.davils.kapi.runtime.client.call.Body
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.CredentialPlacement
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.SecurityUse
import com.davils.kapi.runtime.client.call.Timeout
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.channel.ChannelClose
import com.davils.kapi.runtime.client.channel.ChannelOutcome
import com.davils.kapi.runtime.client.channel.ChannelReply
import com.davils.kapi.runtime.client.conditional.ConditionalOutcome
import com.davils.kapi.runtime.client.conditional.Validators
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.client.oauth.ClientAuthentication
import com.davils.kapi.runtime.client.oauth.Grant
import com.davils.kapi.runtime.client.range.RangeOutcome
import com.davils.kapi.runtime.client.retry.Attempt
import com.davils.kapi.runtime.client.stream.StreamEvent
import com.davils.kapi.runtime.core.conditional.EntityTag
import com.davils.kapi.runtime.core.conditional.HttpDate
import com.davils.kapi.runtime.core.destination.DestinationRefusal
import com.davils.kapi.runtime.core.framing.FramingFailure
import com.davils.kapi.runtime.core.header.IdempotencyKey
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.range.ContentRange
import com.davils.kapi.runtime.core.range.ResolvedRange
import com.davils.kapi.runtime.core.response.headersOf
import com.davils.kapi.runtime.core.security.Secret
import com.davils.krypt.core.algorithm.KeyType
import com.davils.krypt.jose.JoseKey
import com.davils.krypt.jose.jws.JwsAlgorithm
import com.davils.krypt.primitive.Crypto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Duration.Companion.seconds

private const val OK: Int = 200

private const val OTHER_STATUS: Int = 201

private const val DATE: Long = 1_700_000_000

private const val CODE: Int = 4_000

private const val LENGTH: Long = 10

private const val LIMIT: Long = 4

private val json: MediaType = MediaType.Json

private fun answered(status: Int = OK, tag: String = "a", content: String = "x"): Exchange.Answered = Exchange.Answered(
    status = status,
    headers = headersOf(mapOf("etag" to listOf(tag))),
    content = content.encodeToByteString(),
)

private fun distinct(base: Any, same: Any, vararg others: Any?) {
    base shouldBe same
    base.hashCode() shouldBe same.hashCode()
    base.equals("other") shouldBe false
    others.forEach { other -> base shouldNotBe other }
}

class EqualityTest : FunSpec({
    context("the values of a call") {
        test("are equal only with every part equal") {
            val body = "{}".encodeToByteString()
            distinct(Body(json, body), Body(json, body), Body(MediaType.TextPlain, body), Body(json, ByteString()))
            distinct(
                SecurityUse("a", CredentialPlacement.Bearer),
                SecurityUse("a", CredentialPlacement.Bearer),
                SecurityUse("b", CredentialPlacement.Bearer),
                SecurityUse("a", CredentialPlacement.Basic),
            )
            distinct(
                Timeout(request = 1.seconds, connect = 1.seconds, socket = 1.seconds),
                Timeout(request = 1.seconds, connect = 1.seconds, socket = 1.seconds),
                Timeout(connect = 1.seconds, socket = 1.seconds),
                Timeout(request = 1.seconds, socket = 1.seconds),
                Timeout(request = 1.seconds, connect = 1.seconds),
            )
            distinct(CredentialPlacement.Header("a"), CredentialPlacement.Header("a"), CredentialPlacement.Header("b"))
            distinct(CredentialPlacement.Query("a"), CredentialPlacement.Query("a"), CredentialPlacement.Query("b"))
            distinct(CredentialPlacement.Cookie("a"), CredentialPlacement.Cookie("a"), CredentialPlacement.Cookie("b"))
            distinct(
                answered(),
                answered(),
                answered(status = OTHER_STATUS),
                answered(tag = "b"),
                answered(content = "y"),
            )
            distinct(
                Exchange.Failed(CallFailure.TimedOut),
                Exchange.Failed(CallFailure.TimedOut),
                Exchange.Failed(CallFailure.CrossOrigin),
            )
            val keyed = pets.change { idempotencyKey(IdempotencyKey("k")) }
            distinct(
                base = keyed,
                same = pets.change { idempotencyKey(IdempotencyKey("k")) },
                others = arrayOf(pets, apiCall(operation = "x", method = "GET")),
            )
            distinct(
                Attempt(pets, answered()),
                Attempt(pets, answered()),
                Attempt(keyed, answered()),
                Attempt(pets, answered(tag = "b")),
            )
        }

        test("are failures equal only with the same value") {
            listOf(
                listOf(CallFailure.Unreachable("a"), CallFailure.Unreachable("a"), CallFailure.Unreachable("b")),
                listOf(CallFailure.TooLarge(1), CallFailure.TooLarge(1), CallFailure.TooLarge(2)),
                listOf(
                    CallFailure.CredentialMalformed("a"),
                    CallFailure.CredentialMalformed("a"),
                    CallFailure.CredentialMalformed("b"),
                ),
                listOf(CallFailure.PathRefused("a"), CallFailure.PathRefused("a"), CallFailure.PathRefused("b")),
                listOf(
                    CallFailure.ParameterRefused("a"),
                    CallFailure.ParameterRefused("a"),
                    CallFailure.ParameterRefused("b"),
                ),
                listOf(CallFailure.Aborted("a"), CallFailure.Aborted("a"), CallFailure.Aborted("b")),
                listOf(CallFailure.Rejected("a"), CallFailure.Rejected("a"), CallFailure.Rejected("b")),
                listOf(
                    CallFailure.DestinationRefused(DestinationRefusal.Url),
                    CallFailure.DestinationRefused(DestinationRefusal.Url),
                    CallFailure.DestinationRefused(DestinationRefusal.Scheme),
                ),
                listOf(
                    CallFailure.TooManyRedirects(1),
                    CallFailure.TooManyRedirects(1),
                    CallFailure.TooManyRedirects(2),
                ),
                listOf(
                    CallFailure.Framing(FramingFailure.NotUtf8),
                    CallFailure.Framing(FramingFailure.NotUtf8),
                    CallFailure.Framing(FramingFailure.RecordTooLong),
                ),
                listOf(
                    CallFailure.ChannelFramesUnbounded(1L),
                    CallFailure.ChannelFramesUnbounded(1L),
                    CallFailure.ChannelFramesUnbounded(2L),
                ),
                listOf(
                    CallFailure.HandshakeRefused(1),
                    CallFailure.HandshakeRefused(1),
                    CallFailure.HandshakeRefused(2),
                ),
            ).forEach { (base, same, other) -> distinct(base, same, other) }
        }
    }

    context("the values of streams, conditions and ranges") {
        test("are equal only with every part of the value equal") {
            val tag = EntityTag("v")
            val date = HttpDate(DATE)
            val range = ContentRange.Satisfied(ResolvedRange(first = 0, last = LENGTH - 1), completeLength = LENGTH)
            val other = ContentRange.Satisfied(ResolvedRange(first = 1, last = LENGTH - 1), completeLength = LENGTH)
            val content = "x".encodeToByteString()

            distinct(
                StreamEvent.Item("a", id = "1", type = "t"),
                StreamEvent.Item("a", id = "1", type = "t"),
                StreamEvent.Item("b", id = "1", type = "t"),
                StreamEvent.Item("a", id = "2", type = "t"),
                StreamEvent.Item("a", id = "1", type = "u"),
            )
            StreamEvent.Item("a").hashCode() shouldBe StreamEvent.Item("a").hashCode()
            StreamEvent.Item(null).hashCode() shouldBe StreamEvent.Item(null).hashCode()
            distinct(StreamEvent.Malformed("a"), StreamEvent.Malformed("a"), StreamEvent.Malformed("b"))
            distinct(
                StreamEvent.Ended(Exchange.Failed(CallFailure.TimedOut)),
                StreamEvent.Ended(Exchange.Failed(CallFailure.TimedOut)),
                StreamEvent.Ended(Exchange.Failed(CallFailure.CrossOrigin)),
            )
            distinct(Validators(tag, date), Validators(tag, date), Validators(null, date), Validators(tag, null))
            Validators(tag).isEmpty shouldBe false
            Validators(lastModified = date).isEmpty shouldBe false
            Validators().hashCode() shouldBe Validators().hashCode()
            distinct(
                ConditionalOutcome.Modified(answered(), Validators(tag)),
                ConditionalOutcome.Modified(answered(), Validators(tag)),
                ConditionalOutcome.Modified(answered(tag = "b"), Validators(tag)),
                ConditionalOutcome.Modified(answered(), Validators()),
            )
            distinct(
                ConditionalOutcome.NotModified(Validators(tag)),
                ConditionalOutcome.NotModified(Validators(tag)),
                ConditionalOutcome.NotModified(Validators()),
            )
            distinct(
                ConditionalOutcome.PreconditionFailed(answered()),
                ConditionalOutcome.PreconditionFailed(answered()),
                ConditionalOutcome.PreconditionFailed(answered(tag = "b")),
            )
            distinct(
                ConditionalOutcome.Other(answered()),
                ConditionalOutcome.Other(answered()),
                ConditionalOutcome.Other(answered(tag = "b")),
            )
            distinct(
                RangeOutcome.Partial(range, content),
                RangeOutcome.Partial(range, content),
                RangeOutcome.Partial(other, content),
                RangeOutcome.Partial(range, ByteString()),
            )
            distinct(
                RangeOutcome.Ranges(answered(), emptyList()),
                RangeOutcome.Ranges(answered(), emptyList()),
                RangeOutcome.Ranges(answered(tag = "b"), emptyList()),
            )
            distinct(
                RangeOutcome.Full(answered()),
                RangeOutcome.Full(answered()),
                RangeOutcome.Full(answered(tag = "b")),
            )
            distinct(
                RangeOutcome.Unsatisfiable(LENGTH),
                RangeOutcome.Unsatisfiable(LENGTH),
                RangeOutcome.Unsatisfiable(null),
            )
            RangeOutcome.Unsatisfiable(null).hashCode() shouldBe RangeOutcome.Unsatisfiable(null).hashCode()
            distinct(
                RangeOutcome.Malformed(answered()),
                RangeOutcome.Malformed(answered()),
                RangeOutcome.Malformed(answered(tag = "b")),
            )
            distinct(
                RangeOutcome.Other(answered()),
                RangeOutcome.Other(answered()),
                RangeOutcome.Other(answered(tag = "b")),
            )
        }
    }

    context("the values of channels and OAuth") {
        test("are equal only with each of their parts equal") {
            val pair = Crypto().keys.generate(KeyType.P256)
            val key = JoseKey.Private(pair.privateKey)
            val otherKey = JoseKey.Private(Crypto().keys.generate(KeyType.P256).privateKey)
            val close = ChannelClose(code = CODE, reason = "a", isLocal = true)

            distinct(
                base = close,
                same = ChannelClose(code = CODE, reason = "a", isLocal = true),
                others = arrayOf(
                    ChannelClose(code = CODE + 1, reason = "a", isLocal = true),
                    ChannelClose(code = CODE, reason = "b", isLocal = true),
                    ChannelClose(code = CODE, reason = "a", isLocal = false),
                ),
            )
            distinct(ChannelReply.Received("a"), ChannelReply.Received("a"), ChannelReply.Received("b"))
            distinct(
                ChannelOutcome.Completed(1, close),
                ChannelOutcome.Completed(1, close),
                ChannelOutcome.Completed(2, close),
                ChannelOutcome.Completed(1, ChannelClose(code = CODE)),
            )
            distinct(ChannelOutcome.Failed(CallFailure.TimedOut), ChannelOutcome.Failed(CallFailure.TimedOut))
            distinct(
                ClientAuthentication.SecretBasic(Secret("a")),
                ClientAuthentication.SecretBasic(Secret("a")),
                ClientAuthentication.SecretBasic(Secret("b")),
            )
            distinct(
                ClientAuthentication.SecretPost(Secret("a")),
                ClientAuthentication.SecretPost(Secret("a")),
                ClientAuthentication.SecretPost(Secret("b")),
            )
            distinct(
                ClientAuthentication.SecretJwt(Secret("a")),
                ClientAuthentication.SecretJwt(Secret("a")),
                ClientAuthentication.SecretJwt(Secret("b")),
                ClientAuthentication.SecretJwt(Secret("a"), JwsAlgorithm.HS512),
            )
            distinct(
                ClientAuthentication.PrivateKeyJwt(key, JwsAlgorithm.ES256),
                ClientAuthentication.PrivateKeyJwt(key, JwsAlgorithm.ES256),
                ClientAuthentication.PrivateKeyJwt(otherKey, JwsAlgorithm.ES256),
                ClientAuthentication.PrivateKeyJwt(key, JwsAlgorithm.ES384),
            )
            ClientAuthentication.PrivateKeyJwt(key, JwsAlgorithm.ES256).toString() shouldBe "PrivateKeyJwt(ES256)"
            distinct(Grant.RefreshToken(Secret("a")), Grant.RefreshToken(Secret("a")), Grant.RefreshToken(Secret("b")))
            CallFailure.TooLarge(LIMIT).hashCode() shouldBe LIMIT.hashCode()
        }
    }
})
