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

package com.davils.kapi.runtime.client.stream

import com.davils.kapi.runtime.client.ClientEvent
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.Note
import com.davils.kapi.runtime.client.fixture.collecting
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.core.framing.FramingFailure
import com.davils.kapi.runtime.core.framing.ServerSentEvent
import com.davils.kapi.runtime.core.framing.framingLimits
import com.davils.konfetti.core.eventBus
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.io.IOException
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private const val LINE: String = "\n"

private const val CONTROL: Char = '\u0001'

private const val NOTE: String = """{"text":"a"}"""

private const val LAST_EVENT_ID: String = "Last-Event-ID"

private const val RECONNECTS: Int = 2

private const val LIMIT: Int = 8

private const val MANY_RECONNECTS: Int = 150

private const val DEFAULT_RECONNECTS: Int = 5

private const val HARD_RECONNECTS: Int = 1_000

private const val TOO_LONG: String = "data: 123456789\n\n"

private val events: String = listOf("event: add", "id: 1", "data: $NOTE", "", "data: oops", "", "").joinToString(LINE)

private val quick: Reconnection = reconnection {
    maxReconnects = RECONNECTS
    delay = 1.milliseconds
}

private fun MockRequestHandleScope.stream(
    content: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData = respond(
    content = content,
    status = status,
    headers = headersOf(name = HttpHeaders.ContentType, value = "text/event-stream"),
)

private fun lines(vararg fields: String): String = (fields.toList() + "" + "").joinToString(LINE)

private fun <T> items(events: List<StreamEvent<T>>): List<StreamEvent.Item<T>> =
    events.filterIsInstance<StreamEvent.Item<T>>()

private fun tooLong(): Exchanges = Exchanges(configure = { framing = framingLimits { maxEventBytes = LIMIT } }) {
    stream(TOO_LONG)
}

class EventsTest : FunSpec({
    context("the events of a call") {
        test("are each event the server sends with its ID and type, and the data that is none of the type") {
            val read = Exchanges { stream(events) }.withClient { client ->
                client.events(Note.serializer(), pets).toList()
            }

            read.dropLast(1) shouldBe listOf(
                StreamEvent.Item(Note("a"), id = "1", type = "add"),
                StreamEvent.Malformed("An item of the stream is not what the document declares"),
            )
            read.last().shouldBeInstanceOf<StreamEvent.Ended>()
        }

        test("are none for an answer that is no event stream, which ends with the answer") {
            val read = Exchanges { respond(content = "data: x\n\n") }.withClient { client ->
                client.events(pets).toList()
            }
            val ended = read.single().shouldBeInstanceOf<StreamEvent.Ended>()

            ended.exchange.shouldBeInstanceOf<Exchange.Answered>()
        }

        test("end with the framing failure of an event longer than the limit") {
            tooLong().withClient { client -> client.events(pets).toList() } shouldBe
                listOf(StreamEvent.Ended(Exchange.Failed(CallFailure.Framing(FramingFailure.EventTooLong))))
        }
    }

    context("a stream that reconnects") {
        test("resumes after the last event with Last-Event-ID, waiting the retry of the server, and reports it") {
            val seen = mutableListOf<String?>()
            coroutineScope {
                val bus = eventBus(this)
                bus.declare(ClientEvent.TOPIC)
                val exchanges = Exchanges(configure = { this.events = bus }) { request ->
                    seen += request.headers[LAST_EVENT_ID]
                    val first = stream(lines("retry: 1", "id: 7", "data: a")).takeIf { seen.size == 1 }
                    first ?: stream("data: b\n\n", HttpStatusCode.NoContent)
                }
                val reported = collecting(this, bus[ClientEvent.TOPIC], 1)
                val read = exchanges.withClient { client -> client.events(pets, quick).toList() }
                val ended = read.last().shouldBeInstanceOf<StreamEvent.Ended>()

                items(read).map { item -> item.value.data } shouldBe listOf("a")
                seen shouldBe listOf(null, "7")
                ended.exchange.shouldBeInstanceOf<Exchange.Answered>()
                val reconnecting = "Reconnecting(api, listPets, attempt=1, delay=1ms, isResumed=true)"
                reported.await().map(ClientEvent::toString) shouldBe listOf(reconnecting)
                bus.close()
            }
        }

        test("reconnects after a broken connection, at most as often as it may") {
            val exchanges = Exchanges { throw IOException("refused") }
            val read = exchanges.withClient { client -> client.events(pets, quick).toList() }

            read shouldBe listOf(StreamEvent.Ended(Exchange.Failed(CallFailure.Unreachable("IOException"))))
            exchanges.requests.size shouldBe RECONNECTS + 1
        }

        test("keeps reconnecting past the attempts one retry of kontinuity takes, and reports every reconnection") {
            val many = reconnection {
                maxReconnects = MANY_RECONNECTS
                delay = 1.milliseconds
            }
            coroutineScope {
                val bus = eventBus(this)
                bus.declare(ClientEvent.TOPIC)
                val exchanges = Exchanges(configure = { this.events = bus }) { throw IOException("refused") }
                val reported = collecting(this, bus[ClientEvent.TOPIC], MANY_RECONNECTS)

                exchanges.withClient { client -> client.events(pets, many).toList() }

                exchanges.requests.size shouldBe MANY_RECONNECTS + 1
                val attempts = reported.await().map { event -> (event as ClientEvent.Reconnecting).attempt }
                attempts shouldBe (1..MANY_RECONNECTS).toList()
                bus.close()
            }
        }

        test("ends for a server that refuses or ends the stream with no content") {
            val refused = Exchanges { stream("", HttpStatusCode.NotFound) }

            refused.withClient { client -> client.events(pets, quick).toList() }.size shouldBe 1
            tooLong().withClient { client -> client.events(pets, quick).toList() }.size shouldBe 1
            refused.requests.size shouldBe 1
        }

        test("never names a last event ID a header field cannot carry") {
            val seen = mutableListOf<String?>()
            val exchanges = Exchanges { request ->
                seen += request.headers[LAST_EVENT_ID]
                stream(lines("id: a" + CONTROL + "b", "data: x"))
            }

            exchanges.withClient { client -> client.events(pets, quick).toList() }

            seen shouldBe listOf(null, null, null)
        }
    }

    context("a reconnection") {
        test("reconnects five times three seconds apart by default, and refuses limits out of bounds") {
            val defaults = reconnection()

            defaults.maxReconnects shouldBe DEFAULT_RECONNECTS
            defaults.toString() shouldBe "Reconnection(maxReconnects=5, delay=3s, maximumDelay=1m)"
            shouldThrow<ValidationException> { reconnection { maxReconnects = 0 } }
            shouldThrow<ValidationException> { reconnection { delay = 2.minutes } }
            shouldThrow<ValidationException> { reconnection { maximumDelay = 2.hours } }
            Reconnection.HARD_RECONNECTS shouldBe HARD_RECONNECTS
            ServerSentEvent("x").type shouldBe "message"
        }
    }
})
