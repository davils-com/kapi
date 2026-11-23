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

package com.davils.kapi.runtime.client.channel

import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.CredentialPlacement
import com.davils.kapi.runtime.client.call.SecurityUse
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.channel.fixture.GONE
import com.davils.kapi.runtime.client.channel.fixture.Line
import com.davils.kapi.runtime.client.channel.fixture.PROTOCOL
import com.davils.kapi.runtime.client.channel.fixture.withServer
import com.davils.kapi.runtime.client.dpop.dpopProofs
import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.core.parameter.ParameterValue
import com.davils.krypt.core.algorithm.KeyType
import com.davils.krypt.jose.JoseKey
import com.davils.krypt.jose.jws.JwsAlgorithm
import com.davils.krypt.primitive.Crypto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val NOT_FOUND: Int = 404

private const val BEARER: String = "bearer"

private val spec: ChannelSpec<Line, Line> = ChannelSpec(
    sendSerializer = Line.serializer(),
    receiveDeserializer = Line.serializer(),
    settings = channelSettings {
        subprotocols = listOf(PROTOCOL)
        closeTimeout = 2.seconds
    },
)

class ChannelServerTest : FunSpec({
    context("a channel to a real server") {
        test("opens with the subprotocol and the credentials, exchanges messages and closes normally") {
            val call = apiCall(operation = "echo", method = "GET") {
                path("/echo")
                security(listOf(SecurityUse(BEARER, CredentialPlacement.Bearer)))
            }

            val outcome = withServer { client ->
                client.channel(call, spec) {
                    val greeting = incoming.first()
                    send(Line("hi"))
                    listOf(subprotocol, greeting.text, incoming.first().text)
                }
            }
            val completed = outcome.shouldBeInstanceOf<ChannelOutcome.Completed<List<String?>>>()

            completed.value shouldBe listOf(PROTOCOL, "Bearer t1", "hi")
            completed.close.kind shouldBe CloseKind.Normal
        }

        test("sends a DPoP proof and the query string with the handshake") {
            val crypto = Crypto()
            val pair = crypto.keys.generate(KeyType.P256)
            val proofs = dpopProofs(
                crypto = crypto,
                key = JoseKey.Private(pair.privateKey),
                publicKey = pair.publicKey,
                algorithm = JwsAlgorithm.ES256,
            )
            val call = apiCall(operation = "proof", method = "GET") {
                path("/proof")
                queryString("a b&c")
                query(name = "x", value = "y")
                security(listOf(SecurityUse(BEARER, CredentialPlacement.Bearer)))
            }

            val outcome = withServer(configure = { dpop(BEARER, proofs) }) { client ->
                client.channel(call, spec) { incoming.first().text }
            }
            val completed = outcome.shouldBeInstanceOf<ChannelOutcome.Completed<String>>()

            completed.value shouldBe "DPoP t1|true|a%20b%26c&x=y"
        }

        test("surfaces the close code of the server") {
            val call = apiCall(operation = "gone", method = "GET") { path("/gone") }
            val outcome = withServer { client -> client.channel(call, spec) { incoming.collect { _ -> } } }

            val completed = outcome.shouldBeInstanceOf<ChannelOutcome.Completed<Unit>>()

            completed.close shouldBe ChannelClose(code = GONE.toInt(), reason = "gone")
        }

        test("opens at a destination of its own with the cookies of the call") {
            val outcome = withServer { client ->
                val port = client.settings.baseUrl.substringAfterLast(':')
                val call = apiCall(operation = "echo", method = "GET") {
                    destination("ws://127.0.0.1:$port/echo")
                    cookie(
                        ParameterFormat(name = "session", style = ParameterStyle.Form, isExploded = true),
                        ParameterValue.Text("s1"),
                    )
                }
                client.channel(call, spec) { incoming.first().text }
            }

            outcome.shouldBeInstanceOf<ChannelOutcome.Completed<String>>().value shouldBe ""
        }

        test("fails as timed out when the handshake takes too long, and as unreachable without a server") {
            val slow = apiCall(operation = "slow", method = "GET") { path("/slow") }
            val outcome = withServer(timeout = 200.milliseconds) { client -> client.channel(slow, spec) { } }
            val closed = withServer { client ->
                val call = apiCall(operation = "closed", method = "GET") { destination("http://127.0.0.1:1/echo") }
                client.channel(call, spec) { }
            }

            outcome shouldBe ChannelOutcome.Failed(CallFailure.TimedOut)
            closed.shouldBeInstanceOf<ChannelOutcome.Failed>().failure.shouldBeInstanceOf<CallFailure.Unreachable>()
        }

        test("fails with the status of a server that refuses the handshake") {
            val call = apiCall(operation = "missing", method = "GET") { path("/missing") }
            val outcome = withServer { client -> client.channel(call, spec) { } }

            outcome shouldBe ChannelOutcome.Failed(CallFailure.HandshakeRefused(NOT_FOUND))
        }
    }
})
