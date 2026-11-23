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

package com.davils.kapi.runtime.server

import com.davils.kapi.runtime.server.fixture.PROBE
import com.davils.kapi.runtime.server.fixture.probe
import com.davils.kapi.runtime.server.fixture.probing
import com.davils.kapi.runtime.server.fixture.problemOf
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.reply.reply
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

private const val BAD_REQUEST: Int = 400

private const val URI_TOO_LONG: Int = 414

private const val HEADERS_TOO_LARGE: Int = 431

private const val QUERY_LENGTH: Int = 16

private const val FIELDS: Int = 3

private const val HEADERS: Int = 30

private const val HEADER_BYTES: Int = 512

private val reading = probe(method = "GET") { input -> input.isValid }

class HostileTest : FunSpec({
    context("a query") {
        test("longer than the server reads is refused with 414 before anything reads it") {
            probing(reading, settings = { limits { maxQueryLength = QUERY_LENGTH } }) {
                val response = client.get("$PROBE?q=${"x".repeat(QUERY_LENGTH)}")

                response.status.value shouldBe URI_TOO_LONG
                problemOf(response).code shouldBe ServerCodes.URI_TOO_LONG
                client.get("$PROBE?q=${"x".repeat(QUERY_LENGTH - 2)}").bodyAsText() shouldBe "true"
            }
        }

        test("with more fields than the server reads is refused with 400") {
            probing(reading, settings = { limits { maxQueryParameters = FIELDS } }) {
                val response = client.get("$PROBE?a=1&a=2&a=3&a=4")

                response.status.value shouldBe BAD_REQUEST
                val violation = problemOf(response).violations.single()

                violation.message shouldBe "has more than 3 fields"
                client.get("$PROBE?a=1&a=2&a=3").status.value shouldBe OK_STATUS
            }
        }

        test("that is no percent-encoded UTF-8 is refused with 400") {
            probing(reading) {
                val violation = problemOf(client.get("$PROBE?a=%C3%28")).violations.single()

                violation.path shouldBe "query"
            }
        }
    }

    context("the header fields of a request") {
        test("that are too many or too long together are refused with 431") {
            probing(reading, settings = { limits { maxHeaders = HEADERS } }) {
                val response = client.get(PROBE) {
                    repeat(HEADERS) { index -> header(key = "X-Flood-$index", value = "1") }
                }

                response.status.value shouldBe HEADERS_TOO_LARGE
                problemOf(response).code shouldBe ServerCodes.HEADERS_TOO_LARGE
            }
            probing(reading, settings = { limits { maxHeaderBytes = HEADER_BYTES } }) {
                client.get(PROBE) { header(key = "X-Long", value = "x".repeat(HEADER_BYTES)) }.status.value shouldBe
                    HEADERS_TOO_LARGE
            }
        }
    }

    context("a reply") {
        test("refuses a header field that would inject another field or split the response") {
            shouldThrow<IllegalArgumentException> {
                reply(
                    OK_STATUS,
                ) { header(name = "X-Next", value = "a\r\nSet-Cookie: x=1") }
            }
            shouldThrow<IllegalArgumentException> { reply(OK_STATUS) { header(name = "X Bad", value = "a") } }
            shouldThrow<IllegalArgumentException> { reply(OK_STATUS) { header(name = "Content-Length", value = "1") } }
            shouldThrow<IllegalArgumentException> { Reply(status = OK_STATUS, headers = listOf("X-A" to "\u0000")) }
            shouldThrow<IllegalArgumentException> { Reply(status = 99) }
        }
    }
})

private const val OK_STATUS: Int = 200
