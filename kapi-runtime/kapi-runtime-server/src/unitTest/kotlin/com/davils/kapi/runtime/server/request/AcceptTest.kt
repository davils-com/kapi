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
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.fixture.PROBE
import com.davils.kapi.runtime.server.fixture.probing
import com.davils.kapi.runtime.server.fixture.problemOf
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.reply
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.server.testing.ApplicationTestBuilder

private const val CONTENT_TYPE: String = "Content-Type"

private const val JSON_TYPE: String = "application/json"

private const val NOT_ACCEPTABLE: Int = 406

private val offered: List<MediaType> = listOf(MediaType.Json, MediaType.Cbor)

private val negotiating: Operation = Operation(name = "negotiate", method = "GET", path = PROBE) { request ->
    val input = request.input()
    val chosen = input.accept(offered)
    val reply = chosen?.let { type -> reply(OK_STATUS) { body(type, Note.serializer(), Note("a")) } }
    reply ?: input.refused()
}

private val single: Operation = Operation(name = "single", method = "GET", path = "/single") { request ->
    val input = request.input()
    input.accept(listOf(MediaType.Json))
    input.refused().takeUnless { input.isValid } ?: reply(OK_STATUS)
}

private suspend fun ApplicationTestBuilder.accepting(accept: String?, path: String = PROBE): HttpResponse =
    client.get(path) { accept?.let { value -> header("Accept", value) } }

class AcceptTest : FunSpec({
    context("the media type of a reply") {
        test("is the one the client weighs highest among what the operation offers, and the reply varies by it") {
            probing(negotiating) {
                val cbor = accepting("application/json;q=0.5, application/cbor")
                val json = accepting("application/*")

                cbor.headers[CONTENT_TYPE] shouldBe "application/cbor"
                cbor.headers["Vary"] shouldBe "accept"
                json.headers[CONTENT_TYPE] shouldBe JSON_TYPE
            }
        }

        test("is the first offer for a request without Accept or with a malformed one") {
            probing(negotiating) {
                accepting(null).headers[CONTENT_TYPE] shouldBe JSON_TYPE
                accepting("application/json;q=7").headers[CONTENT_TYPE] shouldBe JSON_TYPE
            }
        }

        test("is refused with 406 when no offer is acceptable") {
            probing(negotiating) {
                val response = accepting("text/html")

                response.status.value shouldBe NOT_ACCEPTABLE
                problemOf(response).code shouldBe ServerCodes.NOT_ACCEPTABLE
                response.headers[CONTENT_TYPE] shouldBe "application/problem+json"
            }
        }

        test("does not vary when the operation offers one media type") {
            probing(single) {
                accepting(JSON_TYPE, path = "/single").headers["Vary"] shouldBe null
            }
        }
    }
})

private const val OK_STATUS: Int = 200
