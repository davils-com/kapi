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

import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.CredentialPlacement
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.RequestAttributes
import com.davils.kapi.runtime.client.call.SecurityUse
import com.davils.kapi.runtime.client.call.Timeout
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.Note
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.core.framing.FramingFailure
import com.davils.kapi.runtime.core.framing.framingLimits
import com.davils.kapi.runtime.core.negotiation.MediaType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.io.IOException
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Duration.Companion.milliseconds

private const val LIMIT: Int = 4

private const val LINE: String = "\n"

private const val NOTE: String = """{"text":"a"}"""

private const val MALFORMED: String = "An item of the stream is not what the document declares"

private val records: String = listOf(NOTE, "", """{"text":"b"}""", "nope", "").joinToString(LINE)

private val secured: Call = pets.change { security(listOf(SecurityUse("bearer", CredentialPlacement.Bearer))) }

private val unreachable: Exchange = Exchange.Failed(CallFailure.Unreachable("IOException"))

private fun <T> ended(events: List<StreamEvent<T>>): Exchange =
    events.last().shouldBeInstanceOf<StreamEvent.Ended>().exchange

private fun <T> items(events: List<StreamEvent<T>>): List<T> =
    events.filterIsInstance<StreamEvent.Item<T>>().map { item -> item.value }

private fun failed(failure: CallFailure): List<StreamEvent<Nothing>> =
    listOf(StreamEvent.Ended(Exchange.Failed(failure)))

class StreamsTest : FunSpec({
    context("the lines of a call") {
        test("are every line the server sends and then how the call ended") {
            val events = Exchanges { respond(listOf("a\r", "b", "c").joinToString(LINE)) }.withClient { client ->
                client.lines(pets).toList()
            }

            items(events) shouldBe listOf("a", "b", "c")
            ended(events).shouldBeInstanceOf<Exchange.Answered>().status shouldBe HttpStatusCode.OK.value
        }

        test("are none when the server refuses, which ends with what it answered") {
            val events = Exchanges { respond("gone\n", HttpStatusCode.NotFound) }.withClient { client ->
                client.lines(pets).toList()
            }
            val answered = ended(events).shouldBeInstanceOf<Exchange.Answered>()

            events.size shouldBe 1
            answered.content.decodeToString() shouldBe "gone\n"
        }

        test("are not cut by the time a request may take") {
            val exchanges = Exchanges(configure = { timeout = Timeout(request = 20.milliseconds) }) {
                delay(100.milliseconds)
                respond("late\n")
            }

            exchanges.withClient { client -> client.lines(pets).toList() }.first() shouldBe StreamEvent.Item("late")
        }

        test("end as too large with a line longer than the limit, after the lines before it") {
            val exchanges = Exchanges(configure = { framing = framingLimits { maxRecordBytes = LIMIT } }) {
                respond(listOf("ok", "12345", "later", "").joinToString(LINE))
            }
            val events = exchanges.withClient { client -> client.lines(pets).toList() }

            items(events) shouldBe listOf("ok")
            ended(events) shouldBe Exchange.Failed(CallFailure.TooLarge(LIMIT.toLong()))
        }

        test("end at once for a call that was refused, has no credential or reaches no server") {
            val refused = apiCall(operation = "x", method = "GET") { header(name = "X", value = "\n") }
            val exchanges = Exchanges { throw IOException("refused") }

            exchanges.withClient { client ->
                client.lines(refused).toList() shouldBe failed(CallFailure.ParameterRefused("X"))
                client.lines(secured).toList() shouldBe failed(CallFailure.CredentialMissing)
                client.lines(pets).toList() shouldBe listOf(StreamEvent.Ended(unreachable))
            }
        }
    }

    context("the bytes of a call") {
        test("are every chunk the server sends and then how the call ended") {
            val events = Exchanges { respond("abc\ndef") }.withClient { client -> client.bytes(pets).toList() }

            items(events).joinToString("") { chunk -> chunk.decodeToString() } shouldBe "abc\ndef"
            ended(events).shouldBeInstanceOf<Exchange.Answered>().content shouldBe ByteString()
        }

        test("are read after the server switched the protocol, and the switch reaches the engine") {
            val attach = apiCall(operation = "attach", method = "POST") {
                path("/attach")
                upgrade("tcp", flowOf("in".encodeToByteString()))
            }
            val exchanges = Exchanges { respond(ByteReadChannel("raw"), HttpStatusCode.SwitchingProtocols) }
            val events = exchanges.withClient { client -> client.bytes(attach).toList() }

            val chunk = items(events).single()
            chunk.decodeToString() shouldBe "raw"
            exchanges.requests.single().attributes[RequestAttributes.upgrade] shouldBe attach.upgrade
        }
    }

    context("an upload") {
        test("sends its chunks one after another with its content type") {
            val exchanges = Exchanges()
            val chunks = flowOf("con".encodeToByteString(), "text".encodeToByteString())
            val upload = apiCall(operation = "build", method = "POST") {
                path("/build")
                upload(MediaType.parse("application/x-tar"), chunks)
            }

            exchanges.execute(upload)
            val request = exchanges.requests.single()
            val body = request.body.shouldBeInstanceOf<OutgoingContent.WriteChannelContent>()

            body.contentType?.toString() shouldBe "application/x-tar"
            body.toByteArray().decodeToString() shouldBe "context"
        }
    }

    context("the records of a call") {
        test("are each line that is not blank as the type the document declares, and what is none of it") {
            val events = Exchanges { respond(records) }.withClient { client ->
                client.records(Note.serializer(), pets).toList()
            }

            events.dropLast(1) shouldBe listOf(
                StreamEvent.Item(Note("a")),
                StreamEvent.Item(Note("b")),
                StreamEvent.Malformed(MALFORMED),
            )
        }

        test("are read from a JSON text sequence, skipping a record cut short") {
            val sequence = "\u001E$NOTE\n\u001E" + """{"text":"b"}""" + "\n\u001E" + """{"text":"""
            val events = Exchanges { respond(sequence) }.withClient { client ->
                client.records(Note.serializer(), pets, RecordFraming.JsonSequence).toList()
            }

            items(events) shouldBe listOf(Note("a"), Note("b"))
            ended(events).shouldBeInstanceOf<Exchange.Answered>()
        }

        test("end with the framing failure of a record longer than the limit") {
            val exchanges = Exchanges(configure = { framing = framingLimits { maxRecordBytes = LIMIT } }) {
                respond("$NOTE\n")
            }
            val events = exchanges.withClient { client -> client.records(Note.serializer(), pets).toList() }

            events shouldBe failed(CallFailure.Framing(FramingFailure.RecordTooLong))
        }
    }

    context("the values of a stream") {
        test("are equal by their parts and show escaped text") {
            val item = StreamEvent.Item("a\nb", id = "1", type = "add")

            item shouldBe StreamEvent.Item("a\nb", id = "1", type = "add")
            item.hashCode() shouldBe StreamEvent.Item("a\nb", id = "1", type = "add").hashCode()
            item.toString() shouldBe "Item(a\\nb, id=1, type=add)"
            StreamEvent.Item(null).toString() shouldBe "Item(null, id=none, type=none)"
            StreamEvent.Malformed("x").toString() shouldBe "Malformed(x)"
            StreamEvent.Malformed("x").hashCode() shouldBe "x".hashCode()
            StreamEvent.Ended(unreachable).toString() shouldBe "Ended($unreachable)"
            StreamEvent.Ended(unreachable).hashCode() shouldBe unreachable.hashCode()
            RecordFraming.entries.size shouldBe 2
        }
    }
})
