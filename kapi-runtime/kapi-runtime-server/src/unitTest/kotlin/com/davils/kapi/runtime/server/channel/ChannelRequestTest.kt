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

import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.fixture.onCio
import com.davils.kapi.runtime.server.fixture.serve
import com.davils.kapi.runtime.server.fixture.statusLineOf
import com.davils.kapi.runtime.server.mount
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.operation.Routes
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.request.ParameterType
import com.davils.kapi.runtime.server.request.Place
import com.davils.kapi.runtime.server.request.Request
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.builtins.serializer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets

private const val PATH: String = "/rooms/{roomId}"

private const val ROOM: String = "/rooms/1"

private val roomId: ParameterFormat = ParameterFormat("roomId", ParameterStyle.Simple, isExploded = false)

private const val OPEN: String = "open"

private const val FIRST: String = "1:ping"

private const val PING: String = """{"text":"1:ping"}"""

private const val CRLF: String = "\r\n"

private const val CLOSED: String = "Closed"

private val handshake: String = listOf(
    "GET /rooms/1 HTTP/1.1",
    "Host: localhost",
    "Upgrade: websocket",
    "Connection: close",
    "",
    "",
).joinToString(CRLF)

private val notes: MessageCodec<Note> = MessageCodec(Note.serializer())

private val waiting: Duration = 5.seconds

private val short: Duration = 50.milliseconds

private val byPrefix: Correlation<Note, Note> = Correlation(
    sent = { note -> note.text.substringBefore(':', missingDelimiterValue = "").ifEmpty { null } },
    received = { note -> note.text.substringBefore(':', missingDelimiterValue = "").ifEmpty { null } },
)

private val limit: ParameterFormat = ParameterFormat("limit", ParameterStyle.Form, isExploded = true)

private val number: ParameterType<Int> = ParameterType(Int.serializer(), isQuoted = false)

private fun room(
    correlation: Correlation<Note, Note>? = byPrefix,
    limits: ChannelLimits = ChannelLimits.Default,
    admission: suspend (Request) -> Reply? = { null },
    handler: suspend ChannelSession<Note, Note>.() -> Unit,
): Routes = object : Routes {
    override val operations: List<Operation> = emptyList()
    override val channels: List<ChannelEndpoint<*, *>> = listOf(
        ChannelEndpoint(
            name = "room",
            path = PATH,
            receive = notes,
            send = notes,
            limits = limits,
            correlation = correlation,
            admission = admission,
            handler = handler,
        ),
    )
}

private fun limited(request: Request): Reply? {
    val input = request.input()
    input.value(place = Place.Query, format = limit, type = number, isRequired = true)
    return input.refused().takeUnless { input.isValid }
}

private fun inRoomOne(request: Request): Reply? {
    val input = request.input()
    val room = input.value(place = Place.Path, format = roomId, type = number, isRequired = true)
    return input.refused().takeUnless { input.isValid && room == 1 }
}

private fun ChannelReply<Note>.shown(): String {
    val received = this as? ChannelReply.Received ?: return toString()
    return received.message.text
}

private fun ApplicationTestBuilder.sockets(): HttpClient = createClient { install(ClientWebSockets) }

private suspend fun ApplicationTestBuilder.talk(
    path: String = ROOM,
    block: suspend DefaultClientWebSocketSession.() -> Unit,
) {
    sockets().use { client -> client.webSocket(path) { block() } }
}

private suspend fun textOf(session: DefaultClientWebSocketSession): String {
    val frame = session.incoming.receive() as Frame.Text
    return frame.readText()
}

private suspend fun statusOf(routes: Routes): String {
    var status = ""
    onCio(routes = { mount(routes) }) { port -> status = statusLineOf(port, handshake) }
    return status
}

class ChannelRequestTest : FunSpec({
    context("the admission of a channel") {
        test("refuses an invalid handshake with its problem before the upgrade") {
            statusOf(room(admission = ::limited) { send(Note(OPEN)) }) shouldStartWith "HTTP/1.1 400"
        }

        test("upgrades a valid handshake") {
            serve(routes = { mount(room(admission = ::limited) { send(Note(OPEN)) }) }) {
                talk("$ROOM?limit=5") { textOf(this) shouldBe """{"text":"$OPEN"}""" }
            }
        }

        test("reads the parameters of the path from the path, never from a query field of the same name") {
            serve(routes = { mount(room(admission = ::inRoomOne) { send(Note(OPEN)) }) }) {
                talk("$ROOM?roomId=2") { textOf(this) shouldBe """{"text":"$OPEN"}""" }
            }
        }

        test("answers with 500 and reveals nothing when it fails") {
            statusOf(room(admission = { error("secret") }) { send(Note(OPEN)) }) shouldStartWith "HTTP/1.1 500"
        }
    }

    context("a request the server sends") {
        test("waits for the reply with its correlation id and holds every other message for later") {
            serve(
                routes = {
                    mount(
                        room {
                            val reply = request(Note(FIRST), waiting)
                            val held = receive()
                            send(Note(reply.shown() + "|" + held?.text))
                        },
                    )
                },
            ) {
                talk {
                    textOf(this) shouldBe PING
                    send(Frame.Text("""{"text":"9:other"}"""))
                    send(Frame.Text("""{"text":"1:pong"}"""))
                    textOf(this) shouldBe """{"text":"1:pong|9:other"}"""
                }
            }
        }

        test("is refused without a correlation or a correlation id, and times out without a reply") {
            val replies = mutableListOf<String>()
            serve(
                routes = {
                    mount(
                        room(correlation = null) {
                            replies += request(Note(FIRST), waiting).shown()
                        },
                    )
                },
            ) {
                talk { closeReason.await() }
            }
            serve(
                routes = {
                    mount(
                        room {
                            replies += request(Note("no id"), waiting).shown()
                            replies += request(Note("2:ping"), short).shown()
                        },
                    )
                },
            ) {
                talk {
                    textOf(this) shouldBe """{"text":"2:ping"}"""
                    closeReason.await()
                }
            }

            replies shouldBe listOf("Refused", "Refused", "TimedOut")
        }

        test("ends as closed when the channel closes or holds too many other messages") {
            val replies = mutableListOf<String>()
            val held = channelLimits { maxHeldMessages = 1 }
            serve(
                routes = {
                    mount(
                        room(limits = held) {
                            replies += request(Note(FIRST), waiting).shown()
                            replies += request(Note("2:ping"), waiting).shown()
                        },
                    )
                },
            ) {
                talk {
                    textOf(this) shouldBe PING
                    send(Frame.Text("""{"text":"8:a"}"""))
                    send(Frame.Text("""{"text":"9:b"}"""))
                    closeReason.await()?.code shouldBe ChannelSession.POLICY_VIOLATION
                }
            }
            val ended = CompletableDeferred<String>()
            serve(routes = { mount(room { ended.complete(request(Note(FIRST), waiting).shown()) }) }) {
                talk {
                    textOf(this) shouldBe PING
                    close()
                    ended.await() shouldBe CLOSED
                }
            }

            replies shouldBe listOf(CLOSED, CLOSED)
        }
    }
})
