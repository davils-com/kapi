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

import com.davils.kapi.runtime.client.channel.fixture.FakeSession
import com.davils.kapi.runtime.client.channel.fixture.Message
import com.davils.kapi.runtime.client.channel.fixture.ThrowingCodec
import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.negotiation.MediaType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val LIMIT: Long = 16

private const val GOING_AWAY: Short = 1_001

private const val APPLICATION: Int = 4_000

private const val LONGEST_REASON: Int = 123

private const val PING: String = "ping"

private val quick: ChannelSettings = channelSettings { closeTimeout = 50.milliseconds }

private val correlated: ChannelSpec<Message, Message> = ChannelSpec(
    sendSerializer = Message.serializer(),
    receiveDeserializer = Message.serializer(),
    settings = quick,
    correlation = Correlation(sent = { message: Message -> message.id }, received = { message: Message -> message.id }),
)

private fun idOf(element: JsonElement): String? {
    val id = element.jsonObject["id"] ?: return null
    return id.jsonPrimitive.content
}

private fun text(message: String, fin: Boolean = true): Frame = Frame.Text(fin, message.encodeToByteArray())

private fun specOf(settings: ChannelSettings): ChannelSpec<Message, Message> =
    ChannelSpec(sendSerializer = Message.serializer(), receiveDeserializer = Message.serializer(), settings = settings)

private suspend fun <T> running(
    session: FakeSession,
    spec: ChannelSpec<Message, Message> = correlated,
    block: suspend ChannelSession<Message, Message>.() -> T,
): ChannelOutcome<T> {
    val codec = requireNotNull(Codecs.Default.codecFor(spec.settings.mediaType))
    return ran(session = session, protocol = "chat", spec = spec, codec = codec, block = block)
}

private suspend fun closeOf(session: FakeSession, spec: ChannelSpec<Message, Message>): ChannelClose {
    val outcome = running(session, spec) { incoming.toList() }
    return outcome.shouldBeInstanceOf<ChannelOutcome.Completed<List<Message>>>().close
}

private fun closeOf(frame: Frame?): CloseReason? = (frame as? Frame.Close)?.readReason()

class ChannelSessionTest : FunSpec({
    context("an open channel") {
        test("hands over the messages of the peer in their order and sends the messages of the client") {
            val session = FakeSession()
            session.inbound.send(text("""{"text":"a"}"""))
            session.inbound.send(text("""{"text":"b"}"""))
            session.inbound.send(Frame.Ping(byteArrayOf()))
            session.inbound.send(Frame.Close(CloseReason(GOING_AWAY, "bye")))

            val outcome = running(session) {
                subprotocol shouldBe "chat"
                send(Message("hello")) shouldBe true
                incoming.toList()
            }
            val completed = outcome.shouldBeInstanceOf<ChannelOutcome.Completed<List<Message>>>()
            val frames = session.frames()
            val sent = frames.first().shouldBeInstanceOf<Frame.Text>()

            completed.value shouldBe listOf(Message("a"), Message("b"))
            completed.close shouldBe ChannelClose(code = GOING_AWAY.toInt(), reason = "bye")
            completed.close.kind shouldBe CloseKind.GoingAway
            sent.readText() shouldBe """{"text":"hello"}"""
        }

        test("assembles a message from its fragments") {
            val session = FakeSession()
            session.inbound.send(text("""{"te""", fin = false))
            session.inbound.send(text("""xt":"a"}"""))
            session.inbound.close()

            val outcome = running(session) { incoming.first() }

            outcome.shouldBeInstanceOf<ChannelOutcome.Completed<Message>>().value shouldBe Message("a")
        }

        test("closes normally after its block, and as abnormal when the connection ends without a close") {
            val normal = FakeSession()
            val broken = FakeSession()
            broken.inbound.close()

            val closed = running(normal) { "done" }.shouldBeInstanceOf<ChannelOutcome.Completed<String>>()
            val ended = closeOf(broken, correlated)

            closed.close shouldBe ChannelClose(code = CloseKind.Normal.code, isLocal = true)
            closeOf(normal.frames().last())?.code shouldBe CloseKind.Normal.code.toShort()
            ended.kind shouldBe CloseKind.Abnormal
            closed.toString() shouldBe "Completed(ChannelClose(1000, Normal, , isLocal=true))"
        }

        test("closes with a code of the application, and refuses a code no client may send") {
            val session = FakeSession()

            running(session) {
                shouldThrow<IllegalArgumentException> { close(code = CloseKind.Abnormal.code) }
                val reason = "x".repeat(LONGEST_REASON + 1)
                shouldThrow<IllegalArgumentException> { close(code = APPLICATION, reason = reason) }
                close(code = APPLICATION, reason = "done")
                send(Message("late")) shouldBe false
            }

            closeOf(session.frames().first())?.code shouldBe APPLICATION.toShort()
        }
    }

    context("a peer that breaks the limits of a channel") {
        test("gets a close with 1009 for a frame larger than the client reads") {
            val session = FakeSession()
            session.inbound.send(text("""{"text":"far too long for the limit"}"""))

            closeOf(session, specOf(channelSettings { maxFrameBytes = LIMIT })).kind shouldBe CloseKind.TooBig
            closeOf(session.frames().single())?.code shouldBe CloseKind.TooBig.code.toShort()
        }

        test("gets a close with 1009 for a message larger than the client assembles") {
            val session = FakeSession()
            session.inbound.send(text("""{"text":"0123""", fin = false))
            session.inbound.send(text("""4567890123456789"}"""))

            closeOf(session, specOf(channelSettings { maxMessageBytes = LIMIT })).kind shouldBe CloseKind.TooBig
        }

        test("gets a close with 1007 for a message the codec cannot read") {
            val session = FakeSession()
            session.inbound.send(text("not json"))

            closeOf(session, correlated) shouldBe ChannelClose(code = CloseKind.InvalidPayload.code, isLocal = true)
        }

        test("gets a close with 1007 when the codec of the application throws while it reads") {
            val session = FakeSession()
            session.inbound.send(text("""{"text":"a"}"""))

            val outcome = ran(session = session, protocol = null, spec = correlated, codec = ThrowingCodec()) {
                incoming.toList()
            }

            val completed = outcome.shouldBeInstanceOf<ChannelOutcome.Completed<List<Message>>>()
            completed.close.kind shouldBe CloseKind.InvalidPayload
        }
    }

    context("a channel of binary messages") {
        test("sends and reads binary frames through the codec of its media type") {
            val session = FakeSession()
            val codec = requireNotNull(Codecs.Default.codecFor(MediaType.Cbor))
            val bytes = codec.encode(Message.serializer(), Message("a"))
            session.inbound.send(Frame.Binary(true, bytes.toByteArray()))
            session.inbound.close()

            val outcome = running(session, specOf(channelSettings { mediaType = MediaType.Cbor })) {
                send(Message("b"))
                incoming.toList()
            }

            outcome.shouldBeInstanceOf<ChannelOutcome.Completed<List<Message>>>().value shouldBe listOf(Message("a"))
            val frames = session.frames()
            frames.first().shouldBeInstanceOf<Frame.Binary>()
        }
    }

    context("a request on a channel") {
        test("gets the message with its correlation id, while the others go to the incoming messages") {
            val session = FakeSession()

            val outcome = running(session) {
                val reply = async(start = CoroutineStart.UNDISPATCHED) {
                    request(Message(text = PING, id = "1"), 5.seconds)
                }
                session.inbound.send(text("""{"text":"other"}"""))
                session.inbound.send(text("""{"text":"pong","id":"1"}"""))
                listOf(reply.await(), ChannelReply.Received(incoming.first()))
            }

            outcome.shouldBeInstanceOf<ChannelOutcome.Completed<List<ChannelReply<Message>>>>().value shouldBe listOf(
                ChannelReply.Received(Message(text = "pong", id = "1")),
                ChannelReply.Received(Message("other")),
            )
        }

        test("pairs a reply by the correlation id in the JSON of the messages") {
            val session = FakeSession()
            val byJson = ChannelSpec(
                sendSerializer = Message.serializer(),
                receiveDeserializer = Message.serializer(),
                settings = quick,
                correlation = Correlation.json(sent = ::idOf),
            )

            val outcome = running(session, byJson) {
                val reply = async(start = CoroutineStart.UNDISPATCHED) {
                    request(Message(text = PING, id = "7"), 5.seconds)
                }
                session.inbound.send(text("""{"text":"other","id":"8"}"""))
                session.inbound.send(text("""{"text":"pong","id":"7"}"""))
                val refused = request(Message(PING), 10.milliseconds)
                listOf(reply.await(), ChannelReply.Received(incoming.first()), refused)
            }

            outcome.shouldBeInstanceOf<ChannelOutcome.Completed<List<ChannelReply<Message>>>>().value shouldBe listOf(
                ChannelReply.Received(Message(text = "pong", id = "7")),
                ChannelReply.Received(Message(text = "other", id = "8")),
                ChannelReply.Refused,
            )
        }

        test("finds no id in JSON a correlation cannot read") {
            val session = FakeSession()
            val failing = ChannelSpec(
                sendSerializer = Message.serializer(),
                receiveDeserializer = Message.serializer(),
                settings = quick,
                correlation = Correlation.json(sent = { error("broken") }),
            )

            val normal = ChannelClose(code = CloseKind.Normal.code, isLocal = true)

            running(session, failing) { request(Message(text = PING, id = "1"), 10.milliseconds) } shouldBe
                ChannelOutcome.Completed(ChannelReply.Refused, normal)
        }

        test("times out, is refused without an id or a correlation, and ends when the channel closes") {
            val session = FakeSession()
            val normal = ChannelClose(code = CloseKind.Normal.code, isLocal = true)

            running(session) {
                request(Message(text = PING, id = "1"), 10.milliseconds) shouldBe ChannelReply.TimedOut
                request(Message(PING), 10.milliseconds) shouldBe ChannelReply.Refused
                val waiting = async(start = CoroutineStart.UNDISPATCHED) {
                    request(Message(text = PING, id = "2"), 5.seconds)
                }
                request(Message(text = "again", id = "2"), 10.milliseconds) shouldBe ChannelReply.Refused
                session.inbound.close()
                waiting.await() shouldBe ChannelReply.Closed
                request(Message(text = "late", id = "3"), 10.milliseconds) shouldBe ChannelReply.Refused
            }
            running(FakeSession(), specOf(quick)) { request(Message(text = PING, id = "1"), 10.milliseconds) } shouldBe
                ChannelOutcome.Completed(ChannelReply.Refused, normal)
        }
    }
})
