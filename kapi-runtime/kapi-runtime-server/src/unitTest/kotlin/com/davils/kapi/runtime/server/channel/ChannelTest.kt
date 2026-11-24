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

package com.davils.kapi.runtime.server.channel

import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.fixture.problemOf
import com.davils.kapi.runtime.server.fixture.serve
import com.davils.kapi.runtime.server.mount
import com.davils.kapi.runtime.server.operation.CredentialSource
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.operation.Routes
import com.davils.kapi.runtime.server.operation.SchemeUse
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.security.ApiKeyAuthorizer
import com.davils.kore.core.principal.currentPrincipal
import com.davils.kore.core.principal.principal
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.builtins.serializer
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets

private const val LIMIT_VIOLATIONS: Int = 4

private const val ROOM: String = "/rooms/1"

private const val ROOM_PATH: String = "/rooms/{roomId}"

private const val UPGRADE_REQUIRED: Int = 426

private const val HEADERS_TOO_LARGE: Int = 431

private const val SMALL: Int = 16

private const val PROTOCOL: String = "chat.v1"

private const val KEY_HEADER: String = "X-Key"

private const val SMALL_FRAME: Int = 1_024

private val notes: MessageCodec<Note> = MessageCodec(Note.serializer())

private fun echo(
    protocols: List<String> = emptyList(),
    limits: ChannelLimits = ChannelLimits.Default,
    security: List<List<SchemeUse>> = emptyList(),
    handler: suspend ChannelSession<Note, Note>.() -> Unit = {
        val note = receive()
        note?.let { found -> send(Note("${found.text}:${protocol.orEmpty()}:${currentPrincipal()?.id.orEmpty()}")) }
    },
): Routes = object : Routes {
    override val operations: List<Operation> = emptyList()
    override val channels: List<ChannelEndpoint<*, *>> = listOf(
        ChannelEndpoint(
            name = "room",
            path = ROOM_PATH,
            receive = notes,
            send = notes,
            protocols = protocols,
            security = security,
            limits = limits,
            handler = handler,
        ),
    )
}

private fun ApplicationTestBuilder.sockets(): HttpClient = createClient { install(ClientWebSockets) }

private suspend fun ApplicationTestBuilder.talk(
    vararg headers: Pair<String, String>,
    block: suspend DefaultClientWebSocketSession.() -> Unit,
) {
    sockets().use { client ->
        client.webSocket(ROOM, request = { headers.forEach { (name, value) -> header(key = name, value = value) } }) {
            block()
        }
    }
}

private suspend fun closeCodeOf(session: DefaultClientWebSocketSession): Short? = session.closeReason.await()?.code

class ChannelTest : FunSpec({
    context("a channel") {
        test("receives and sends typed messages over the negotiated subprotocol as the authorized caller") {
            val keyed = listOf(listOf(SchemeUse("key", CredentialSource.Header(KEY_HEADER))))
            val keys = ApiKeyAuthorizer { key -> principal("member").takeIf { key.reveal() == "k" } }
            serve(
                settings = { authorizer("key", keys) },
                routes = { mount(echo(protocols = listOf(PROTOCOL, "chat.v0"), security = keyed)) },
            ) {
                talk(KEY_HEADER to "k", "Sec-WebSocket-Protocol" to "chat.v0, $PROTOCOL") {
                    send(Frame.Text("""{"text":"hi"}"""))
                    val answer = incoming.receive() as Frame.Text

                    answer.readText() shouldBe """{"text":"hi:$PROTOCOL:member"}"""
                    closeCodeOf(this) shouldBe CloseReason.Codes.NORMAL.code
                }
            }
        }

        test("reads its messages as a flow until the client closes it") {
            val seen = mutableListOf<Note>()
            serve(routes = {
                mount(
                    echo(handler = {
                        incoming.collect { note ->
                            seen += note
                            send(note)
                        }
                    }),
                )
            }) {
                talk {
                    send(Frame.Text("""{"text":"a"}"""))
                    incoming.receive()
                    send(Frame.Binary(true, """{"text":"b"}""".encodeToByteArray()))
                    incoming.receive()
                    close()
                    closeReason.await()
                }
            }

            seen shouldBe listOf(Note("a"), Note("b"))
        }

        test("is closed with 1007 for a message its codec cannot read") {
            serve(routes = { mount(echo()) }) {
                talk {
                    send(Frame.Text("{"))
                    closeCodeOf(this) shouldBe ChannelSession.INVALID_PAYLOAD
                }
            }
        }

        test("is closed with 1009 for a message larger than its limits, in one frame or in many") {
            val limits = channelLimits { maxMessageBytes = SMALL }
            serve(routes = { mount(echo(limits = limits)) }) {
                talk {
                    send(Frame.Text(fin = false, data = """{"text":""".encodeToByteArray()))
                    send(Frame.Text(fin = true, data = "\"0123456789\"}".encodeToByteArray()))
                    closeCodeOf(this) shouldBe ChannelSession.TOO_BIG
                }
            }
        }

        test("assembles a message of several frames within its limits") {
            serve(routes = { mount(echo()) }) {
                talk {
                    send(Frame.Text(fin = false, data = """{"text":""".encodeToByteArray()))
                    send(Frame.Text(fin = true, data = "\"x\"}".encodeToByteArray()))
                    (incoming.receive() as Frame.Text).readText() shouldContain "x::"
                }
            }
        }

        test("is closed with 1011 when its handler fails, without a word of why") {
            serve(routes = { mount(echo(handler = { error("secret") })) }) {
                talk {
                    val reason = closeReason.await()

                    reason?.code shouldBe ChannelSession.INTERNAL_ERROR
                    reason?.message.orEmpty() shouldBe ""
                }
            }
        }

        test("closes with the code and reason the handler chooses, and refuses a reason too long to send") {
            val failures = mutableListOf<Throwable>()
            serve(
                routes = {
                    mount(
                        echo(handler = {
                            failures += shouldThrow<IllegalArgumentException> { close(reason = "x".repeat(LONG)) }
                            close(ChannelSession.POLICY_VIOLATION, "bye")
                            close(ChannelSession.GOING_AWAY, "again")
                        }),
                    )
                },
            ) {
                talk {
                    val reason = closeReason.await()

                    reason?.code shouldBe ChannelSession.POLICY_VIOLATION
                    reason?.message shouldBe "bye"
                }
            }

            failures.size shouldBe 1
        }

        test("sends binary frames for a media type that is no text") {
            val cbor = MessageCodec(Note.serializer(), MediaType.Cbor)
            val routes = object : Routes {
                override val operations: List<Operation> = emptyList()
                override val channels: List<ChannelEndpoint<*, *>> = listOf(
                    ChannelEndpoint(name = "binary", path = ROOM_PATH, receive = notes, send = cbor) {
                        send(Note("a"))
                    },
                )
            }
            serve(routes = { mount(routes) }) {
                talk {
                    (incoming.receive() is Frame.Binary) shouldBe true
                }
            }
        }
    }

    context("a channel mounted on a server") {
        test("is refused when the WebSockets plugin reads frames larger than the channel allows") {
            val small = channelLimits { maxFrameBytes = SMALL_FRAME }
            serve(routes = { shouldThrow<IllegalArgumentException> { mount(echo(limits = small)) } }) {
                startApplication()
            }
        }
    }

    context("a request for a channel") {
        test("that is no upgrade is refused with 426") {
            serve(routes = { mount(echo()) }) {
                val response = client.get(ROOM)

                response.status.value shouldBe UPGRADE_REQUIRED
                response.headers["Upgrade"] shouldBe "websocket"
                problemOf(response).code shouldBe ServerCodes.UPGRADE_REQUIRED
            }
        }
    }

    context("the parts of a channel") {
        test("refuse a name, a path or a subprotocol that cannot be served, and show what they are") {
            shouldThrow<IllegalArgumentException> {
                ChannelEndpoint(name = " ", path = "/a", receive = notes, send = notes) { }
            }
            shouldThrow<IllegalArgumentException> {
                ChannelEndpoint(name = "a", path = "a", receive = notes, send = notes) { }
            }
            shouldThrow<IllegalArgumentException> {
                ChannelEndpoint(name = "a", path = "/a", receive = notes, send = notes, protocols = listOf("a b")) { }
            }
            ChannelEndpoint(name = "a", path = "/a", receive = notes, send = notes) { }.toString() shouldBe
                "ChannelEndpoint(a, /a)"
            notes.toString() shouldBe "MessageCodec(com.davils.kapi.runtime.server.fixture.Note, application/json)"
            notes shouldBe MessageCodec(Note.serializer())
            notes.hashCode() shouldBe MessageCodec(Note.serializer()).hashCode()
            MessageCodec(String.serializer(), MediaType.TextPlain).isText shouldBe true
            MessageCodec(String.serializer(), MediaType.parse("application/problem+json")).isText shouldBe true
            ChannelLimits.Default.toString() shouldContain "frameBytes=65536"
        }

        test("limits refuse values outside their bounds") {
            shouldThrow<ValidationException> {
                channelLimits {
                    maxFrameBytes = 0
                    maxMessageBytes = ChannelLimits.HARD_MESSAGE_BYTES + 1
                    pingInterval = kotlin.time.Duration.ZERO
                    timeout = kotlin.time.Duration.ZERO
                }
            }.report.violations.size shouldBe LIMIT_VIOLATIONS
        }
    }

    context("a channel that cannot write its messages or read its request") {
        test("closes with 1011 when no codec of the server writes a message") {
            val unknown = MessageCodec(Note.serializer(), MediaType.parse("application/x-unknown"))
            val routes = object : Routes {
                override val operations: List<Operation> = emptyList()
                override val channels: List<ChannelEndpoint<*, *>> = listOf(
                    ChannelEndpoint(name = "mute", path = ROOM_PATH, receive = notes, send = unknown) {
                        send(Note("a"))
                    },
                )
            }
            serve(routes = { mount(routes) }) {
                talk { closeReason.await()?.code shouldBe ChannelSession.INTERNAL_ERROR }
            }
        }

        test("refuses a request whose header fields are too many before the upgrade") {
            serve(settings = { limits { maxHeaders = 2 } }, routes = { mount(echo()) }) {
                val response = client.get(ROOM) {
                    header(key = "X-A", value = "1")
                    header(key = "X-B", value = "2")
                    header(key = "X-C", value = "3")
                }

                response.status.value shouldBe HEADERS_TOO_LARGE
            }
        }
    }
})

private const val LONG: Int = 124
