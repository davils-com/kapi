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

package com.davils.kapi.runtime.testing.mock

import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.framing.ServerSentEvent
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.problem.Problem
import com.davils.kapi.runtime.testing.fixture.Pet
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val HOST: String = "https://api.example.com"

private const val OK: Int = 200

private const val NOT_FOUND: Int = 404

private const val NOT_ALLOWED: Int = 405

private const val GONE: Int = 410

private const val FAILED: Int = 500

private const val THIRD: Int = 3

private const val FOURTH: Int = 4

private const val FIFTH: Int = 5

private const val SIX: Int = 6

private const val PROBLEM: String = "application/problem+json"

private const val JSON: String = "application/json"

private const val BOOM: String = "/boom"

private const val REX: String = """{"id":1,"name":"Rex"}"""

private val EVENT: String = listOf("id: 1", "data: a", "", "").joinToString(separator = "\n")

private const val PETS: String = "/pets"

private const val PET: String = "/pets/{id}"

private val rex: Pet = Pet(id = 1, name = "Rex")

private suspend fun statusOf(http: HttpClient, url: String): String {
    val status = http.get(url).status
    return status.value.toString()
}

private suspend fun <T> MockApi.using(block: suspend (HttpClient) -> T): T = httpClient().use { http -> block(http) }

class MockApiTest : FunSpec({
    context("routes") {
        test("answer by method and path template, the most literal first") {
            val api = mockApi {
                on(
                    method = "get",
                    path = PET,
                ) { respond(OK, Pet(id = pathParameters.getValue("id").toInt(), name = "Any")) }
                get("/pets/mine") { respondText(OK, "mine") }
            }

            val texts = api.using { http ->
                listOf(http.get("$HOST/pets/7").bodyAsText(), http.get("$HOST/pets/mine").bodyAsText())
            }

            texts shouldBe listOf("""{"id":7,"name":"Any"}""", "mine")
            val routed = api.exchangesOf(method = "GET", path = PET).single()
            routed.request.path shouldBe "/pets/7"
            api.exchanges.map(RecordedExchange::route) shouldBe listOf(PET, "/pets/mine")
        }

        test("decode the path parameters and below the base path only") {
            val api = mockApi {
                basePath = "/v1"
                get("/files/{name}.{kind}") { respondText(OK, "${pathParameters["name"]}|${pathParameters["kind"]}") }
                get("/") { respondText(OK, "root") }
            }

            val answers = api.using { http ->
                listOf(
                    http.get("$HOST/v1/files/a%20b.txt").bodyAsText(),
                    http.get("$HOST/v1").bodyAsText(),
                    statusOf(http, "$HOST/v1x/files/a.txt"),
                    statusOf(http, "$HOST/v1/files/.txt"),
                )
            }

            answers shouldBe listOf("a b|txt", "root", "404", "404")
        }

        test("answer 404 and 405 with problems when nothing matches") {
            val api = mockApi { get(PETS) { respond(status = OK) } }

            val statuses = api.using { http ->
                listOf(http.get("$HOST/other"), http.post("$HOST$PETS")).map { response ->
                    response.status.value to response.headers[HttpHeaders.ContentType]
                }
            }

            statuses shouldBe listOf(NOT_FOUND to PROBLEM, NOT_ALLOWED to PROBLEM)
            api.exchanges.map(RecordedExchange::route) shouldBe listOf(null, null)
        }

        test("that fail or set no response answer 500 and keep the failure") {
            val api = mockApi {
                get(BOOM) { error("boom") }
                get("/silent") { header(key = "X-Seen", value = "yes") }
                get("/bad") { respond(status = 99) }
            }

            val bodies = api.using { http ->
                listOf(BOOM, "/silent", "/bad").map { path -> http.get(HOST + path).bodyAsText() }
            }

            bodies[0] shouldContain "The route of the mock API failed"
            bodies[1] shouldContain "The route of the mock API set no response"
            api.failures shouldHaveSize 2
            api.failures.first().message shouldBe "boom"
        }
    }

    context("answers") {
        test("write JSON, problems, records, events, bytes and headers") {
            val api = mockApi {
                get("/json") { respond(OK, rex) }
                get("/problem") { respondProblem(Problem(title = "Gone", status = 410)) }
                get("/default") { respondProblem(Problem(title = "Unknown")) }
                get("/ndjson") { respondRecords(Pet.serializer(), listOf(rex, rex)) }
                get("/seq") {
                    respondRecords(
                        serializer = Pet.serializer(),
                        records = listOf(rex),
                        mediaType = MediaType.JsonSequence,
                    )
                }
                get("/events") { respondEvents(listOf(ServerSentEvent(data = "a", id = "1"))) }
                get("/bytes") {
                    header(key = "X-One", value = "1")
                    respond(status = OK, content = "raw".encodeToByteString(), mediaType = MediaType.OctetStream)
                }
            }

            val answers = api.using { http ->
                listOf("/json", "/problem", "/default", "/ndjson", "/seq", "/events", "/bytes").map { path ->
                    val response = http.get(HOST + path)
                    Triple(
                        first = response.status.value,
                        second = response.headers[HttpHeaders.ContentType],
                        third = response.bodyAsText(),
                    )
                }
            }

            answers[0] shouldBe Triple(first = OK, second = JSON, third = REX)
            answers[1].first shouldBe GONE
            answers[1].third shouldContain """"title":"Gone""""
            answers[2].first shouldBe FAILED
            answers[THIRD].third shouldBe "$REX\n$REX\n"
            answers[FOURTH].third shouldBe "\u001E$REX\n"
            answers[FIFTH] shouldBe Triple(first = OK, second = "text/event-stream", third = EVENT)
            answers.last().third shouldBe "raw"
            val last = api.exchanges.last()
            last.response.headers["X-One"] shouldBe "1"
        }

        test("refuse a media type without a codec") {
            val api = mockApi {
                get("/x") {
                    respond(
                        status = OK,
                        serializer = Pet.serializer(),
                        value = rex,
                        mediaType = MediaType.EventStream,
                    )
                }
            }

            api.using { http -> http.get("$HOST/x").status.value } shouldBe FAILED
            api.failures.single().shouldBeInstanceOf<IllegalArgumentException>()
        }
    }

    context("recorded requests") {
        test("keep the method, the URL, the headers and the body, which a route can decode") {
            val decoded = mutableListOf<Pet?>()
            val api = mockApi {
                post(PETS) {
                    decoded += body<Pet>()
                    body(Pet.serializer()).shouldBeInstanceOf<Decoding.Decoded<Pet>>()
                    respond(201, request.query("dry") ?: "none")
                }
            }

            api.using { http ->
                http.post("$HOST$PETS?dry=yes") {
                    header(key = "X-Trace", value = "t")
                    contentType(ContentType.Application.Json)
                    setBody("""{"id":2,"name":"Fido"}""")
                }
            }
            val request = api.requests.single()

            decoded shouldBe listOf(Pet(id = 2, name = "Fido"))
            request.method shouldBe "POST"
            request.header("X-Trace") shouldBe "t"
            request.contentType shouldBe JSON
            request.text shouldBe """{"id":2,"name":"Fido"}"""
            val answered = api.exchanges.single().response
            answered.text shouldBe """"yes""""
            answered.contentType shouldBe JSON
        }

        test("are forgotten by clear") {
            val api = mockApi { get(BOOM) { error("boom") } }
            api.using { http -> http.get(HOST + BOOM) }

            api.clear()

            api.exchanges shouldBe emptyList()
            api.failures shouldBe emptyList()
        }

        test("are values that never show headers or bodies") {
            val request = RecordedRequest(
                method = "GET",
                url = Url("$HOST/a"),
                headers = headersOf(name = HttpHeaders.Authorization, value = "Bearer secret"),
                body = ByteString(),
            )
            val response = RecordedResponse(status = OK, headers = Headers.Empty, body = "secret".encodeToByteString())
            val exchange = RecordedExchange(request = request, response = response, route = "/a")
            val twin = RecordedExchange(
                request = RecordedRequest(
                    method = "GET",
                    url = Url("$HOST/a"),
                    headers = headersOf(name = HttpHeaders.Authorization, value = "Bearer secret"),
                    body = ByteString(),
                ),
                response = RecordedResponse(status = OK, headers = Headers.Empty, body = "secret".encodeToByteString()),
                route = "/a",
            )

            exchange shouldBe twin
            exchange.hashCode() shouldBe twin.hashCode()
            exchange shouldNotBe RecordedExchange(request = request, response = response)
            request shouldNotBe RecordedRequest(
                method = "PUT",
                url = request.url,
                headers = request.headers,
                body = request.body,
            )
            response shouldNotBe RecordedResponse(status = 201, headers = Headers.Empty, body = response.body)
            exchange.toString() shouldBe
                "RecordedExchange(RecordedRequest(GET https://api.example.com/a, 1 headers, 0 bytes), " +
                "RecordedResponse(200, 0 headers, 6 bytes), route=\"/a\")"
            RecordedExchange(request, response).toString() shouldContain "route=none"
        }
    }

    context("the configuration") {
        test("refuses bad templates, methods, base paths and duplicates") {
            val failure = shouldThrow<ValidationException> {
                mockApi {
                    basePath = "v1/"
                    get("pets") { respond(status = OK) }
                    on(method = "GE T", path = PETS) { respond(status = OK) }
                    get("/a/{") { respond(status = OK) }
                    get("/a/{}") { respond(status = OK) }
                    get(PETS) { respond(status = OK) }
                    on(method = "get", path = PETS) { respond(status = OK) }
                }
            }

            failure.report.violations shouldHaveSize SIX
        }

        test("shows the routes and the base path") {
            val api = mockApi { basePath = "/v1" }

            api.toString() shouldBe "MockApi(routes=0, basePath=\"/v1\")"
            api.basePath shouldBe "/v1"
            api.codecs shouldNotBe null
        }
    }
})
