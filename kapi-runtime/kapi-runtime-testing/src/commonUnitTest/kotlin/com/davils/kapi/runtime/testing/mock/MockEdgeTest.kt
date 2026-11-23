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

import com.davils.kapi.runtime.testing.fixture.Pet
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlinx.io.bytestring.ByteString

private const val HOST: String = "https://api.example.com"

private const val OK: Int = 200

private const val NOT_FOUND: Int = 404

private const val FAILED: Int = 500

private val request: RecordedRequest =
    RecordedRequest(method = "GET", url = Url(HOST), headers = Headers.Empty, body = ByteString())

private val response: RecordedResponse = RecordedResponse(status = OK, headers = Headers.Empty, body = ByteString())

private suspend fun textOf(response: HttpResponse): String = response.bodyAsText()

class MockEdgeTest : FunSpec({
    context("answers") {
        test("refuse a status outside 100 to 599") {
            shouldThrow<IllegalArgumentException> {
                RecordedResponse(
                    status = 99,
                    headers = Headers.Empty,
                    body = ByteString(),
                )
            }
            shouldThrow<IllegalArgumentException> {
                RecordedResponse(
                    status = 600,
                    headers = Headers.Empty,
                    body = ByteString(),
                )
            }
            val api = mockApi { get("/") { respond(status = 600) } }

            api.httpClient().use { http -> http.get(HOST).status.value } shouldBe FAILED
        }

        test("of a route that throws after it answered are failures") {
            val api = mockApi {
                get("/") {
                    respond(status = OK)
                    error("late")
                }
            }

            val text = api.httpClient().use { http -> http.get(HOST).bodyAsText() }

            text shouldContain "The route of the mock API failed"
        }

        test("decode no body that does not fit and keep a content type sent as a header") {
            val api = mockApi {
                post("/") { respondText(status = OK, text = body<Pet>()?.name ?: "none") }
                get("/") { respondText(status = OK, text = request.contentType ?: "none") }
            }

            val texts = api.httpClient().use { http ->
                listOf(
                    textOf(
                        http.post(HOST) {
                            contentType(ContentType.Application.Json)
                            setBody("{")
                        },
                    ),
                    textOf(http.get(HOST) { header(key = HttpHeaders.ContentType, value = "text/plain") }),
                )
            }

            texts shouldBe listOf("none", "text/plain")
        }

        test("leave paths outside the base path unanswered") {
            val api = mockApi {
                basePath = "/v1"
                get("/") { respond(status = OK) }
            }

            api.httpClient().use { http -> http.get("$HOST/other").status.value } shouldBe NOT_FOUND
        }
    }

    context("the configuration") {
        test("refuses a route without a method") {
            shouldThrow<ValidationException> { mockApi { on(method = "", path = "/") { respond(status = OK) } } }
        }
    }

    context("recorded values") {
        test("differ from every other type") {
            request.equals("request") shouldBe false
            response.equals("response") shouldBe false
            RecordedExchange(request, response).equals("exchange") shouldBe false
            request.query("a") shouldBe null
        }
    }
})
