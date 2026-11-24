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
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.channel.fixture.FakeSession
import com.davils.kapi.runtime.client.channel.fixture.Message
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.websocket.Frame
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val BEYOND: Int = 5_000

private const val PING: String = "ping"

private val quick: ChannelSettings = channelSettings { closeTimeout = 50.milliseconds }

private fun text(message: String): Frame = Frame.Text(true, message.encodeToByteArray())

private fun specOf(
    settings: ChannelSettings = quick,
    correlation: Correlation<Message, Message>? = null,
): ChannelSpec<Message, Message> = ChannelSpec(
    sendSerializer = Message.serializer(),
    receiveDeserializer = Message.serializer(),
    settings = settings,
    correlation = correlation,
)

private suspend fun <T> running(
    session: FakeSession,
    spec: ChannelSpec<Message, Message>,
    block: suspend ChannelSession<Message, Message>.() -> T,
): ChannelOutcome<T> {
    val codec = requireNotNull(Codecs.Default.codecFor(spec.settings.mediaType))
    return ran(session = session, protocol = null, spec = spec, codec = codec, block = block)
}

class ChannelEdgeTest : FunSpec({
    context("a channel of text") {
        test("sends a JSON suffix as text, and refuses a message its codec cannot write") {
            val suffixed = FakeSession()
            val plain = FakeSession()
            val vendor = channelSettings { mediaType = MediaType.parse("application/vnd.pets+json") }
            val text = channelSettings { mediaType = MediaType.parse("text/plain") }

            running(suffixed, specOf(vendor)) { send(Message("a")) }
            val refused = running(plain, specOf(text)) {
                listOf(send(Message("a")), toString())
            }

            val frames = suffixed.frames()

            frames.first().shouldBeInstanceOf<Frame.Text>()
            refused.shouldBeInstanceOf<ChannelOutcome.Completed<List<Any>>>().value shouldBe
                listOf(false, "ChannelSession(no subprotocol)")
        }
    }

    context("the frames of a peer") {
        test("pass a pong by, and read a close without a reason as no status") {
            val session = FakeSession()
            session.inbound.send(Frame.Pong(byteArrayOf()))
            session.inbound.send(text("""{"text":"a"}"""))
            session.inbound.send(Frame.Close())

            val outcome = running(session, specOf()) { incoming.toList() }
            val completed = outcome.shouldBeInstanceOf<ChannelOutcome.Completed<List<Message>>>()

            completed.value shouldBe listOf(Message("a"))
            completed.close.kind shouldBe CloseKind.NoStatus
        }
    }

    context("a correlation that fails") {
        test("refuses the request, and hands the message over as incoming") {
            val session = FakeSession()
            val failing = Correlation<Message, Message>(
                sent = { _ -> error("no id") },
                received = { _ -> error("no id") },
            )
            session.inbound.send(text("""{"text":"a","id":"1"}"""))
            session.inbound.close()

            val outcome = running(session, specOf(correlation = failing)) {
                listOf(request(Message(text = PING, id = "1"), 1.seconds), ChannelReply.Received(incoming.toList()))
            }

            outcome.shouldBeInstanceOf<ChannelOutcome.Completed<List<Any>>>().value shouldBe listOf(
                ChannelReply.Refused,
                ChannelReply.Received(listOf(Message(text = "a", id = "1"))),
            )
        }
    }

    context("the requests of a channel") {
        test("end as closed when the request cannot be sent, and are refused beyond the pending limit") {
            val session = FakeSession()
            val correlation = Correlation<Message, Message>(sent = { message -> message.id }, received = { m -> m.id })
            val text = channelSettings {
                mediaType = MediaType.parse("text/plain")
                closeTimeout = 50.milliseconds
            }
            val single = channelSettings {
                maxPendingRequests = 1
                closeTimeout = 50.milliseconds
            }

            running(session, specOf(settings = text, correlation = correlation)) {
                request(Message(text = PING, id = "1"), 1.seconds)
            }.shouldBeInstanceOf<ChannelOutcome.Completed<ChannelReply<Message>>>().value shouldBe ChannelReply.Closed
            running(FakeSession(), specOf(settings = single, correlation = correlation)) {
                val first = async(start = CoroutineStart.UNDISPATCHED) {
                    request(Message(text = PING, id = "1"), 20.milliseconds)
                }
                listOf(request(Message(text = PING, id = "2"), 20.milliseconds), first.await())
            }.shouldBeInstanceOf<ChannelOutcome.Completed<List<ChannelReply<Message>>>>().value shouldBe
                listOf(ChannelReply.Refused, ChannelReply.TimedOut)
        }
    }

    context("a close") {
        test("refuses a code beyond the application range, and is sent only once") {
            val session = FakeSession()

            running(session, specOf()) {
                shouldThrow<IllegalArgumentException> { close(code = BEYOND) }
                close()
                close()
            }

            session.frames().size shouldBe 1
        }
    }

    context("the settings of a channel") {
        test("refuse every limit and time out of bounds") {
            val refused = listOf<ChannelSettingsBuilder.() -> Unit>(
                { maxFrameBytes = ChannelSettings.HARD_FRAME_BYTES + 1 },
                { maxMessageBytes = 0 },
                { maxMessageBytes = ChannelSettings.HARD_MESSAGE_BYTES + 1 },
                { incomingCapacity = ChannelSettings.HARD_COUNT + 1 },
                { maxPendingRequests = 0 },
                { maxPendingRequests = ChannelSettings.HARD_COUNT + 1 },
                { pingInterval = (-1).seconds },
                { pongTimeout = Duration.ZERO },
                { closeTimeout = 2.hours },
            )

            refused.forEach { configure -> shouldThrow<ValidationException> { channelSettings(configure) } }
        }
    }

    context("a channel that cannot open") {
        test("fails with the refusal of its call") {
            val call = apiCall(operation = "chat", method = "GET") { header(name = "a b", value = "x") }
            val exchanges = Exchanges(http = {
                followRedirects = false
                install(WebSockets) { maxFrameSize = ChannelSettings.Default.maxFrameBytes }
            })

            val outcome = exchanges.withClient { client -> client.channel(call, specOf()) { } }

            outcome.shouldBeInstanceOf<ChannelOutcome.Failed>().failure shouldBe CallFailure.ParameterRefused("a b")
        }
    }
})
