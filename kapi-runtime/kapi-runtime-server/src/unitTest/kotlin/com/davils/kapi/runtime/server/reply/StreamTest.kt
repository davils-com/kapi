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

package com.davils.kapi.runtime.server.reply

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.codecs
import com.davils.kapi.runtime.core.framing.ServerSentEvent
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.fixture.PROBE
import com.davils.kapi.runtime.server.fixture.serve
import com.davils.kapi.runtime.server.fixture.serveOne
import com.davils.kapi.runtime.server.operation
import com.davils.kapi.runtime.server.operation.CredentialSource
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.operation.SchemeUse
import com.davils.kapi.runtime.server.security.ApiKeyAuthorizer
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.principal.currentPrincipal
import com.davils.kore.core.principal.principal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val OK: Int = 200

private const val PAUSE: Long = 120L

private const val KEY: String = "key"

private const val KEY_HEADER: String = "X-Key"

private val beat: Duration = 20.milliseconds

private val pretty: Codecs = codecs { json = Json { prettyPrint = true } }

private fun records(vararg lines: String): String = lines.joinToString("") { line -> line + "\n" }

class StreamTest : FunSpec({
    context("a streamed reply") {
        test("sends its chunks as they come, chunked or with the length it names") {
            val chunks = flowOf("ab".encodeToByteString(), "cd".encodeToByteString())

            serveOne(handler = { reply(OK) { stream(MediaType.OctetStream, chunks) } }) {
                client.post(PROBE).bodyAsText() shouldBe "abcd"
            }
            serveOne(handler = { reply(OK) { stream(MediaType.OctetStream, chunks, length = 4) } }) {
                client.post(PROBE).headers["Content-Length"] shouldBe "4"
            }
        }

        test("of records is JSON Lines, newline-delimited JSON or a JSON text sequence") {
            val records = flowOf(Note("a"), Note("b"))

            serveOne(handler = { reply(OK) { lines(Note.serializer(), records) } }) {
                val response = client.post(PROBE)

                response.headers["Content-Type"] shouldBe "application/x-ndjson"
                response.bodyAsText() shouldBe records("""{"text":"a"}""", """{"text":"b"}""")
            }
            serveOne(handler = { reply(OK) { lines(Note.serializer(), records, MediaType.JsonSequence) } }) {
                val response = client.post(PROBE)

                response.bodyAsText() shouldBe records("\u001E" + """{"text":"a"}""", "\u001E" + """{"text":"b"}""")
            }
        }
    }

    context("the producer of a streamed reply") {
        test("runs as the caller the server authorized") {
            val keyed = listOf(listOf(SchemeUse(KEY, CredentialSource.Header(KEY_HEADER))))
            val keys = ApiKeyAuthorizer { key -> principal("member").takeIf { key.reveal() == KEY } }
            val probe = Operation(name = "probe", method = "POST", path = PROBE, security = keyed) {
                reply(OK) { events { send(ServerSentEvent(data = currentPrincipal()?.id.orEmpty())) } }
            }

            serve(settings = { authorizer(KEY, keys) }, routes = { operation(probe) }) {
                val response = client.post(PROBE) { header(key = KEY_HEADER, value = KEY) }

                response.bodyAsText() shouldBe records("data: member", "")
            }
        }

        test("that fails breaks the response instead of ending it") {
            val failing = flow {
                emit("ab".encodeToByteString())
                error("the producer broke")
            }

            serveOne(handler = { reply(OK) { stream(MediaType.OctetStream, failing) } }) {
                val read = attempted { client.post(PROBE).bodyAsText() }

                read.getOrNull() shouldBe null
            }
        }
    }

    context("a stream of server-sent events") {
        test("sends its events, names the last event a client saw and is not cached") {
            val seen = mutableListOf<String?>()

            serveOne(
                handler = { request ->
                    seen += request.lastEventId
                    reply(OK) {
                        events {
                            send(ServerSentEvent(data = "one\ntwo", type = "note", id = "8"))
                            comment("kept")
                        }
                    }
                },
            ) {
                val response = client.post(PROBE) { header(key = "Last-Event-ID", value = "7") }

                response.headers["Content-Type"] shouldBe "text/event-stream; charset=utf-8"
                response.headers["Cache-Control"] shouldBe "no-cache"
                response.bodyAsText() shouldBe records("event: note", "id: 8", "data: one", "data: two", "", ":kept")
            }

            seen shouldBe listOf("7")
        }

        test("sends heartbeat comments while the producer waits, in the scope of the response") {
            serveOne(
                handler = {
                    reply(OK) {
                        events(heartbeat = beat) {
                            delay(PAUSE)
                            send(ServerSentEvent(data = "late"))
                        }
                    }
                },
            ) {
                val body = client.post(PROBE).bodyAsText()

                body shouldContain ":\n"
                body shouldContain "data: late\n\n"
            }
        }

        test("of typed items writes each item as the JSON of the codecs of the server") {
            val items = flowOf(Note("a"), Note("b\nc"))

            serveOne(handler = { reply(OK) { events(Note.serializer(), items) } }) {
                val response = client.post(PROBE)

                response.headers["Cache-Control"] shouldBe "no-cache"
                response.bodyAsText() shouldBe
                    records("""data: {"text":"a"}""", "", """data: {"text":"b\nc"}""", "")
            }
            serveOne(
                handler = { reply(OK) { events(Note.serializer(), items) } },
                settings = { codecs = pretty },
            ) {
                client.post(PROBE).bodyAsText() shouldContain """data:     "text": "a""""
            }
            shouldThrow<IllegalArgumentException> { reply(OK) { events(Note.serializer(), items, Duration.ZERO) } }
        }

        test("refuses a heartbeat that never comes and a comment that would break a line") {
            shouldThrow<IllegalArgumentException> { reply(OK) { events(heartbeat = Duration.ZERO) { } } }
            serveOne(handler = { reply(OK) { events { comment("a\nb") } } }) {
                attempted { client.post(PROBE).bodyAsText() }.getOrNull() shouldBe null
            }
        }
    }
})
