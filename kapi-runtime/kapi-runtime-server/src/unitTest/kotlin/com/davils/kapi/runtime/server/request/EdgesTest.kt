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

import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.server.fixture.Chunked
import com.davils.kapi.runtime.server.fixture.Filter
import com.davils.kapi.runtime.server.fixture.JSON
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.fixture.PROBE
import com.davils.kapi.runtime.server.fixture.gzipped
import com.davils.kapi.runtime.server.fixture.probe
import com.davils.kapi.runtime.server.fixture.probing
import com.davils.kapi.runtime.server.fixture.problemOf
import com.davils.kapi.runtime.server.problem.ServerCodes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

private const val BAD_REQUEST: Int = 400

private const val TOO_LARGE: Int = 413

private const val UNSUPPORTED: Int = 415

private const val SMALL: Long = 8L

private const val CHUNKED: Int = 64

private const val CONTENT_TYPE: String = "Content-Type"

private const val OCTETS: String = "application/octet-stream"

private suspend fun ApplicationTestBuilder.chunked(content: ByteArray, type: String = JSON): HttpResponse =
    client.post(PROBE) { setBody(Chunked(content, ContentType.parse(type))) }

class EdgesTest : FunSpec({
    context("a body sent in chunks") {
        test("that is empty is missing for every reader") {
            val readers = listOf<Pair<String, suspend (Input) -> Any?>>(
                JSON to { input -> input.body(deserializer = Note.serializer(), isRequired = true) },
                JSON to { input -> input.bytes(isRequired = true) },
                "multipart/form-data; boundary=b" to { input -> input.parts(isRequired = true) },
            )
            readers.forEach { (type, read) ->
                probing(probe { input -> read(input) }) {
                    val violation = problemOf(chunked(ByteArray(0), type = type)).violations.single()

                    violation.code shouldBe ServerCodes.REQUIRED
                }
            }
        }

        test("that is longer than the server reads is refused without a declared length") {
            probing(
                probe { input -> input.bytes(isRequired = true) },
                settings = { limits { maxRequestBytes = SMALL } },
            ) {
                chunked(ByteArray(CHUNKED)).status.value shouldBe TOO_LARGE
            }
            probing(
                probe { input -> input.upload(isRequired = true)?.read { } },
                settings = { limits { maxUploadBytes = SMALL } },
            ) {
                chunked(ByteArray(CHUNKED)).status.value shouldBe TOO_LARGE
            }
        }

        test("that inflates to nothing streams nothing") {
            val operation = probe { input ->
                var pieces = 0
                input.upload(isRequired = true)?.read { pieces += 1 }
                pieces
            }

            probing(operation) {
                val response = client.post(PROBE) {
                    header(key = CONTENT_TYPE, value = OCTETS)
                    header(key = "Content-Encoding", value = "gzip")
                    setBody(gzipped(ByteArray(0)))
                }

                response.bodyAsText() shouldBe "0"
            }
        }
    }

    context("a reader") {
        test("of a value that is no model takes it as it is") {
            val operation = probe { input -> input.body(deserializer = String.serializer(), isRequired = true) }

            probing(operation) {
                val response = client.post(PROBE) {
                    header(key = CONTENT_TYPE, value = JSON)
                    setBody("\"plain\"")
                }

                response.bodyAsText() shouldBe "plain"
            }
        }

        test("of a parameter refuses a structure for a type that is no string") {
            val list = ParameterType(ListSerializer(Int.serializer()), isQuoted = false)
            val format = ParameterFormat("ids", ParameterStyle.Form, isExploded = true)
            val operation = probe(method = "GET") { input ->
                input.value(place = Place.Query, format = format, type = list, isRequired = true)
            }

            probing(operation) {
                client.get("$PROBE?ids=%5B1%5D").status.value shouldBe BAD_REQUEST
            }
        }

        test("of an object is nothing without a finding when it is optional and missing") {
            val deep = ParameterFormat("filter", ParameterStyle.DeepObject, isExploded = true)
            val operation = probe(method = "GET") { input ->
                input.fields(place = Place.Query, format = deep, deserializer = Filter.serializer(), isRequired = false)
            }

            probing(operation) {
                client.get(PROBE).bodyAsText() shouldBe "null"
            }
        }

        test("of the whole query takes an empty query as missing") {
            val operation = probe(method = "GET") { input ->
                input.queryString(mediaType = MediaType.Json, deserializer = Note.serializer(), isRequired = false)
            }

            probing(operation) {
                client.get("$PROBE?").bodyAsText() shouldBe "null"
            }
        }

        test("keeps the first refusal and adds the later ones as nothing") {
            val operation = probe { input ->
                input.body(deserializer = Note.serializer(), isRequired = true, mediaTypes = listOf(MediaRange.Json))
                input.accept(listOf(MediaType.Cbor))
            }

            probing(operation) {
                val response = client.post(PROBE) {
                    header(key = CONTENT_TYPE, value = "text/plain")
                    header(key = "Accept", value = "application/json")
                    setBody("a")
                }

                response.status.value shouldBe UNSUPPORTED
            }
        }
    }

    context("an empty parameter") {
        test("of a type that is no string is malformed") {
            val format = ParameterFormat("limit", ParameterStyle.Form, isExploded = true)
            val number = ParameterType(Int.serializer(), isQuoted = false)
            val operation = probe(method = "GET") { input ->
                input.value(place = Place.Query, format = format, type = number, isRequired = true)
            }

            probing(operation) {
                client.get("$PROBE?limit=").status.value shouldBe BAD_REQUEST
            }
        }
    }
})
