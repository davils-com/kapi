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

package com.davils.kapi.runtime.server.request

import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.core.problem.Problem
import com.davils.kapi.runtime.core.problem.ProblemViolation
import com.davils.kapi.runtime.server.fixture.Filter
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.fixture.PROBE
import com.davils.kapi.runtime.server.fixture.probe
import com.davils.kapi.runtime.server.fixture.probing
import com.davils.kapi.runtime.server.fixture.problemOf
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.problem.ServerCodes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.builtins.serializer

private const val FILTER: String = "filter"

private const val COOKIE: String = "Cookie"

private const val NAME: String = "limit"

private const val FIVE: String = "5"

private const val LONG_ID: Int = 1_025

private const val BAD_REQUEST: Int = 400

private const val MALFORMED: String = "is not what the document declares"

private val number: ParameterType<Int> = ParameterType(Int.serializer(), isQuoted = false)

private val text: ParameterType<String> = ParameterType(String.serializer(), isQuoted = true)

private val form: ParameterFormat = ParameterFormat(NAME, ParameterStyle.Form, isExploded = true)

private val simple: ParameterFormat = ParameterFormat(NAME, ParameterStyle.Simple, isExploded = false)

private fun Input.required(place: Place, format: ParameterFormat, type: ParameterType<*>): Any? =
    value(place = place, format = format, type = type, isRequired = true)

private fun violation(path: String, code: String, message: String): ProblemViolation =
    ProblemViolation(path = path, code = code, message = message)

class InputTest : FunSpec({
    context("a parameter of a request") {
        test("is read from the path, the query, a header and a cookie as the type the document declares") {
            val operation = probe(path = "/items/{limit}", method = "GET") { input ->
                Place.entries.map { place ->
                    val format = simple.takeIf { place == Place.Path || place == Place.Header } ?: form
                    input.required(place = place, format = format, type = number)
                }
            }

            probing(operation) {
                val response = client.get("/items/5?limit=5") {
                    header(key = NAME, value = FIVE)
                    header(key = COOKIE, value = "other=1; limit=5")
                }

                response.bodyAsText() shouldBe "[5, 5, 5, 5]"
            }
        }

        test("is read from the path only, never from a query field of the same name") {
            val operation = probe(path = "/items/{limit}", method = "GET") { input ->
                listOf(input.required(Place.Path, simple, number), input.required(Place.Query, form, number))
            }

            probing(operation) {
                client.get("/items/5?limit=9").bodyAsText() shouldBe "[5, 9]"
            }
        }

        test("is a text as it came, even when it looks like something else") {
            val operation = probe(method = "GET") { input -> input.required(Place.Query, form, text) }

            probing(operation) {
                client.get("$PROBE?limit=%225%22+true").bodyAsText() shouldBe "\"5\" true"
            }
        }

        test("is nothing without a finding when it is optional and missing") {
            val operation = probe(method = "GET") { input ->
                input.value(place = Place.Query, format = form, type = number, isRequired = false)
            }

            probing(operation) {
                client.get(PROBE).bodyAsText() shouldBe "null"
            }
        }

        test("is a list of its items in the style of its format") {
            val pipes = ParameterFormat(NAME, ParameterStyle.PipeDelimited, isExploded = false)
            val operation = probe(method = "GET") { input ->
                val exploded = input.items(place = Place.Query, format = form, type = number, isRequired = true)
                val delimited = ParameterFormat("ids", ParameterStyle.PipeDelimited, isExploded = false)
                val listed = input.items(place = Place.Query, format = delimited, type = text, isRequired = true)
                val absent = input.items(place = Place.Header, format = pipes, type = text, isRequired = false)
                listOf(exploded, listed, absent)
            }

            probing(operation) {
                client.get("$PROBE?limit=1&limit=2&ids=a%7Cb").bodyAsText() shouldBe "[[1, 2], [a, b], null]"
            }
        }

        test("is an object of its properties in the deep object style") {
            val deep = ParameterFormat(FILTER, ParameterStyle.DeepObject, isExploded = true)
            val operation = probe(method = "GET") { input ->
                input.fields(place = Place.Query, format = deep, deserializer = Filter.serializer(), isRequired = true)
            }

            probing(operation) {
                client.get("$PROBE?filter%5Bcolor%5D=red&filter%5Bsize%5D=2").bodyAsText() shouldBe
                    Filter(color = "red", size = 2).toString()
            }
        }

        test("is an object of an exploded form cookie whose properties the document names") {
            val cookie = ParameterFormat(FILTER, ParameterStyle.Form, isExploded = true)
            val operation = probe(method = "GET") { input ->
                input.fields(
                    place = Place.Cookie,
                    format = cookie,
                    deserializer = Filter.serializer(),
                    isRequired = true,
                    fields = setOf("color", "size"),
                )
            }

            probing(operation) {
                val response = client.get(PROBE) { header(key = COOKIE, value = "color=blue; size=3; other=x") }

                response.bodyAsText() shouldBe Filter(color = "blue", size = 3).toString()
            }
        }

        test("keeps the reserved characters of a query value that allows them") {
            val reserved = ParameterFormat(
                name = "next",
                style = ParameterStyle.Form,
                isExploded = true,
                isReservedAllowed = true,
            )
            val operation = probe(method = "GET") { input -> input.required(Place.Query, reserved, text) }

            probing(operation) {
                client.get("$PROBE?next=/a/b:c@d!").bodyAsText() shouldBe "/a/b:c@d!"
            }
        }

        test("is read from a cookie of the cookie style of OpenAPI 3.2 without decoding") {
            val cookie = ParameterFormat("token", ParameterStyle.Cookie, isExploded = false)
            val operation = probe(method = "GET") { input -> input.required(Place.Cookie, cookie, text) }

            probing(operation) {
                client.get(PROBE) { header(key = COOKIE, value = "token=a%20b") }.bodyAsText() shouldBe "a%20b"
            }
        }
    }

    context("the query of OpenAPI 3.2 that is one parameter") {
        test("is read as JSON when its media type is JSON") {
            val operation = probe(method = "GET") { input ->
                input.queryString(mediaType = MediaType.Json, deserializer = Note.serializer(), isRequired = true)
            }

            probing(operation) {
                client.get("$PROBE?%7B%22text%22%3A%22a%22%7D").bodyAsText() shouldBe Note("a").toString()
            }
        }

        test("is read as a form when its media type is a form") {
            val operation = probe(method = "GET") { input ->
                input.queryString(
                    mediaType = MediaType.FormUrlEncoded,
                    deserializer = Note.serializer(),
                    isRequired = true,
                )
            }

            probing(operation) {
                client.get("$PROBE?text=b").bodyAsText() shouldBe Note("b").toString()
            }
        }

        test("is refused when it is missing or no value of its media type") {
            val operation = probe(method = "GET") { input ->
                input.queryString(mediaType = MediaType.Json, deserializer = Note.serializer(), isRequired = true)
            }

            probing(operation) {
                problemOf(client.get(PROBE)).violations shouldBe
                    listOf(violation(path = "query", code = ServerCodes.REQUIRED, message = "is required"))
                problemOf(client.get("$PROBE?%7B")).violations shouldBe
                    listOf(violation(path = "query", code = ServerCodes.MALFORMED, message = MALFORMED))
            }
        }
    }

    context("a request that breaks its document") {
        test("is refused for a parameter that came more often than its style allows") {
            val joined = ParameterFormat(NAME, ParameterStyle.Form, isExploded = false)
            val operation = probe(method = "GET") { input -> input.required(Place.Query, joined, number) }

            probing(operation) {
                client.get("$PROBE?limit=1&limit=2").status.value shouldBe BAD_REQUEST
            }
        }

        test("is refused once, with every parameter that is missing or malformed") {
            val ids = ParameterFormat("ids", ParameterStyle.Form, isExploded = true)
            val deep = ParameterFormat(FILTER, ParameterStyle.DeepObject, isExploded = true)
            val operation = probe(method = "GET") { input ->
                input.required(Place.Query, form, number)
                input.items(place = Place.Query, format = ids, type = number, isRequired = true)
                input.required(Place.Header, simple, number)
                input.fields(place = Place.Query, format = deep, deserializer = Filter.serializer(), isRequired = true)
                input.required(Place.Query, ParameterFormat("list", ParameterStyle.Form, isExploded = true), number)
            }

            probing(operation) {
                val response = client.get("$PROBE?limit=many&ids=1&ids=x&filter%5Bsize%5D=x&list=%5B1%5D")
                val problem = problemOf(response)

                response.status.value shouldBe Problem.INVALID_STATUS
                problem.code shouldBe Problem.INVALID_CODE
                problem.violations shouldBe listOf(
                    violation(path = "query.limit", code = ServerCodes.MALFORMED, message = MALFORMED),
                    violation(path = "query.ids", code = ServerCodes.MALFORMED, message = MALFORMED),
                    violation(path = "header.limit", code = ServerCodes.REQUIRED, message = "is required"),
                    violation(path = "query.filter", code = ServerCodes.MALFORMED, message = MALFORMED),
                    violation(path = "query.list", code = ServerCodes.MALFORMED, message = MALFORMED),
                )
            }
        }

        test("is refused for a path parameter in a style its value breaks") {
            val label = ParameterFormat("id", ParameterStyle.Label, isExploded = false)
            val operation = probe(path = "/items/{id}") { input -> input.required(Place.Path, label, number) }

            probing(operation) {
                client.post("/items/5").status.value shouldBe BAD_REQUEST
                client.post("/items/.5").bodyAsText() shouldBe FIVE
            }
        }
    }

    context("the request as the handler sees it") {
        test("names its method and its operation, and nothing it carries") {
            val seen = mutableListOf<Request>()
            val operation = probe(path = "/notes/{id}") { input -> input.isValid }
            val recording = Operation(
                name = operation.name,
                method = operation.method,
                path = operation.path,
            ) { request ->
                seen += request
                operation.handler(request)
            }

            probing(recording) {
                client.post("/notes/a%20b?tag=x&tag=y") {
                    header(key = "Last-Event-ID", value = "seven")
                    header(key = "Content-Type", value = "application/json; charset=utf-8")
                    header(key = "Authorization", value = "Bearer secret")
                    header(key = "Host", value = "Example.COM:80")
                }
            }
            val request = seen.single()

            request.toString() shouldBe "Request(POST, probe)"
            request.pathParameters shouldBe mapOf("id" to "a b")
            request.query shouldBe listOf("tag" to "x", "tag" to "y")
            request.rawQuery shouldBe "tag=x&tag=y"
            request.path shouldBe "/notes/a%20b"
            request.scheme shouldBe "http"
            request.authority shouldBe "example.com"
            request.lastEventId shouldBe "seven"
            request.contentType shouldBe MediaType.parse("application/json; charset=utf-8")
            request.hasBody shouldBe false
            request.headers.toString().contains("secret") shouldBe false
        }

        test("drops a last event id that no event id can hold") {
            val operation = probe(method = "GET") { input -> input.isValid }
            val ids = mutableListOf<String?>()
            val recording = Operation(
                name = "ids",
                method = "GET",
                path = PROBE,
            ) { request ->
                ids += request.lastEventId
                operation.handler(request)
            }

            probing(recording) {
                client.get(PROBE) { header(key = "Last-Event-ID", value = "x".repeat(LONG_ID)) }
                client.get(PROBE)
            }

            ids shouldBe listOf(null, null)
        }
    }
})
