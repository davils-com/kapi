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

import com.davils.kapi.runtime.core.destination.IpAddress
import com.davils.kapi.runtime.server.channel.ChannelEndpoint
import com.davils.kapi.runtime.server.channel.MessageCodec
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.fixture.certificateOf
import com.davils.kapi.runtime.server.fixture.onCio
import com.davils.kapi.runtime.server.fixture.statusLineOf
import com.davils.kapi.runtime.server.operation.CredentialSource
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.operation.SchemeUse
import com.davils.kapi.runtime.server.reply.reply
import com.davils.kapi.runtime.server.request.Peer
import com.davils.kapi.runtime.server.request.PeerIdentity
import com.davils.kapi.runtime.server.security.CertificateAuthorizer
import com.davils.kore.core.principal.principal
import com.davils.krypt.x509.Certificate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install

private const val SELF: String = "GET /nodes/self"

private const val HOST: String = "Host: localhost"

private val tls: List<List<SchemeUse>> = listOf(listOf(SchemeUse("tls", CredentialSource.Certificate)))

private const val OK: Int = 200

private const val WORKER: String = "worker"

private const val CRLF: String = "\r\n"

private val UPGRADE: String = wire(
    "GET /rooms/1 HTTP/1.1",
    HOST,
    "Upgrade: websocket",
    "Connection: Upgrade",
    "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==",
    "Sec-WebSocket-Version: 13",
)

private fun wire(vararg lines: String): String = lines.joinToString("") { line -> line + CRLF }

private val notes: MessageCodec<Note> = MessageCodec(Note.serializer())

private fun recording(seen: MutableList<Peer>, isGuarded: Boolean): Operation = Operation(
    name = "describeNode",
    method = "GET",
    path = "/nodes/self",
    security = tls.takeIf { isGuarded }.orEmpty(),
) { request ->
    seen += request.peer
    reply(OK)
}

private fun Application.transport(chain: List<Certificate>?, identity: PeerIdentity? = null) {
    val plugin = createApplicationPlugin("tls-transport") {
        onCall { call ->
            chain?.let { certificates -> call.attributes.put(PeerAttributes.certificates, certificates) }
            identity?.let { peer -> call.attributes.put(PeerAttributes.identity, peer) }
        }
    }
    install(plugin)
}

private fun request(line: String): String = wire("$line HTTP/1.1", HOST, "Connection: close", "")

class SocketTest : FunSpec({
    context("a real server") {
        test("refuses a query that is no percent-encoding as it came over the wire") {
            val reading = Operation(name = "read", method = "GET", path = "/read") { reply(OK) }

            onCio(routes = { operation(reading) }) { port ->
                statusLineOf(port, request("GET /read?a=%zz")) shouldContain "400"
                statusLineOf(port, request("GET /read?a=1")) shouldContain "200"
            }
        }

        test("serves the method QUERY and a method of its own as they came over the wire") {
            val query = Operation(name = "search", method = "QUERY", path = "/pets") { reply(OK) }
            val purge = Operation(name = "purge", method = "PURGE", path = "/pets") { reply(OK) }

            onCio(routes = {
                operation(query)
                operation(purge)
            }) { port ->
                statusLineOf(port, request("QUERY /pets")) shouldContain "200"
                statusLineOf(port, request("PURGE /pets")) shouldContain "200"
            }
        }

        test("refuses an upgrade without credentials or a subprotocol it speaks before it upgrades") {
            val keyed = listOf(listOf(SchemeUse("key", CredentialSource.Header("X-Key"))))
            val guarded = ChannelEndpoint(
                name = "room",
                path = "/rooms/{roomId}",
                receive = notes,
                send = notes,
                security = keyed,
            ) { }
            val spoken = ChannelEndpoint(
                name = "room",
                path = "/rooms/{roomId}",
                receive = notes,
                send = notes,
                protocols = listOf("chat.v1"),
            ) { }

            onCio(routes = { channel(guarded) }) { port ->
                statusLineOf(port, UPGRADE + CRLF) shouldContain "401"
            }
            onCio(routes = { channel(spoken) }) { port ->
                statusLineOf(port, UPGRADE + wire("Sec-WebSocket-Protocol: other", "")) shouldContain "400"
                statusLineOf(port, UPGRADE + wire("Sec-WebSocket-Protocol: chat.v1", "")) shouldContain "101"
            }
        }
    }

    context("an operation that requires mutual TLS") {
        val authorizer = CertificateAuthorizer { chain ->
            principal(WORKER).takeIf { chain.first().subject.commonName == WORKER }
        }

        test("sees the address of the peer and the chain the transport verified, and lets in a known chain") {
            val seen = mutableListOf<Peer>()
            val worker = listOf(certificateOf(WORKER))

            onCio(
                settings = { authorizer("tls", authorizer) },
                transport = { transport(worker) },
                routes = { operation(recording(seen, isGuarded = true)) },
            ) { port ->
                statusLineOf(port, request(SELF)) shouldContain "200"
            }

            seen shouldBe listOf(Peer(IpAddress.parse("127.0.0.1"), worker))
        }

        test("refuses a stranger and a peer without a chain, whose handler never runs") {
            val seen = mutableListOf<Peer>()
            val stranger = listOf(certificateOf("stranger"))

            onCio(
                settings = { authorizer("tls", authorizer) },
                transport = { transport(stranger) },
                routes = { operation(recording(seen, isGuarded = true)) },
            ) { port ->
                statusLineOf(port, request(SELF)) shouldContain "401"
            }
            onCio(
                settings = { authorizer("tls", authorizer) },
                transport = { transport(null) },
                routes = { operation(recording(seen, isGuarded = true)) },
            ) { port ->
                statusLineOf(port, request(SELF)) shouldContain "401"
            }

            seen shouldBe emptyList()
        }

        test("lets an open operation see the identity the transport verified") {
            val seen = mutableListOf<Peer>()
            val owner = PeerIdentity(WORKER, process = 7L)

            onCio(
                transport = { transport(chain = null, identity = owner) },
                routes = { operation(recording(seen, isGuarded = false)) },
            ) { port ->
                statusLineOf(port, request(SELF)) shouldContain "200"
            }

            seen shouldBe listOf(Peer(IpAddress.parse("127.0.0.1"), identity = owner))
            seen.single().toString() shouldBe
                "Peer(address=127.0.0.1, certificates=0, identity=PeerIdentity(worker, 7))"
        }
    }

    context("a body of no declared length") {
        test("is read when it is chunked, and refused without a media type") {
            val reading = Operation(name = "read", method = "POST", path = "/read") { request ->
                val input = request.input()
                val bytes = input.bytes(isRequired = true)
                input.refused().takeUnless { input.isValid } ?: reply(
                    OK,
                ) { text(bytes?.run { size.toString() }.orEmpty()) }
            }
            val chunked = wire(
                "POST /read HTTP/1.1",
                HOST,
                "Connection: close",
                "Transfer-Encoding: chunked",
            )
            val content = wire("", "3", "abc", "0", "")

            onCio(routes = { operation(reading) }) { port ->
                statusLineOf(
                    port,
                    chunked + wire("Content-Type: text/plain") + content,
                ) shouldContain "200"
                statusLineOf(port, chunked + content) shouldContain "415"
            }
        }
    }
})
