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

import com.davils.kapi.runtime.core.conditional.EntityTag
import com.davils.kapi.runtime.core.conditional.EntityTagCondition
import com.davils.kapi.runtime.core.conditional.HttpDate
import com.davils.kapi.runtime.core.conditional.RepresentationState
import com.davils.kapi.runtime.server.fixture.PROBE
import com.davils.kapi.runtime.server.fixture.problemOf
import com.davils.kapi.runtime.server.fixture.requestOf
import com.davils.kapi.runtime.server.fixture.serve
import com.davils.kapi.runtime.server.operation
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.problem.ServerCodes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.server.testing.ApplicationTestBuilder

private const val MODIFIED: String = "Sun, 06 Nov 1994 08:49:37 GMT"

private const val STALE: String = """"v1""""

private const val OK: Int = 200

private const val PUT: String = "PUT"

private const val IF_MATCH: String = "If-Match"

private const val EARLY_HINTS: Int = 103

private const val NOT_FOUND: Int = 404

private const val NOT_MODIFIED: Int = 304

private const val PRECONDITION_FAILED: Int = 412

private const val EPOCH: Long = 784_111_777L

private const val CURRENT: String = "\"v2\""

private val state: RepresentationState = RepresentationState(
    entityTag = EntityTag("v2"),
    lastModified = HttpDate(EPOCH),
)

private fun conditional(method: String, status: Int = OK): Operation =
    Operation(name = "conditional", method = method, path = PROBE) { request ->
        request.conditional(state) { reply(status) { text("current") } }
    }

private suspend fun ApplicationTestBuilder.ask(method: String, vararg headers: Pair<String, String>): HttpResponse =
    client.request(PROBE) {
        this.method = HttpMethod(method)
        headers.forEach { (name, value) -> header(name, value) }
    }

class ConditionalTest : FunSpec({
    context("a conditional request") {
        test("is answered with the representation and its validators when every precondition holds") {
            serve(routes = { operation(conditional("GET")) }) {
                val response = ask("GET", "If-None-Match" to STALE)

                response.status.value shouldBe OK
                response.headers["ETag"] shouldBe CURRENT
                response.headers["Last-Modified"] shouldBe MODIFIED
                response.bodyAsText() shouldBe "current"
            }
        }

        test("is answered with 304 and the validators when the representation did not change") {
            serve(routes = { operation(conditional("GET")) }) {
                val tagged = ask("GET", "If-None-Match" to """W/"v2", "v0"""")
                val dated = ask("GET", "If-Modified-Since" to MODIFIED)

                tagged.status.value shouldBe NOT_MODIFIED
                tagged.headers["ETag"] shouldBe CURRENT
                dated.status.value shouldBe NOT_MODIFIED
            }
        }

        test("is refused with 412 when a precondition of a change fails, and the change never runs") {
            serve(routes = { operation(conditional(PUT)) }) {
                val response = ask(PUT, IF_MATCH to STALE)

                response.status.value shouldBe PRECONDITION_FAILED
                problemOf(response).code shouldBe ServerCodes.PRECONDITION_FAILED
                ask(PUT, "If-Unmodified-Since" to "Sat, 05 Nov 1994 08:49:37 GMT").status.value shouldBe
                    PRECONDITION_FAILED
                ask(PUT, IF_MATCH to CURRENT).status.value shouldBe OK
            }
        }

        test("leaves an answer that is no success as it is") {
            serve(routes = { operation(conditional("GET", NOT_FOUND)) }) {
                ask("GET").headers["ETag"] shouldBe null
            }
        }

        test("reads every precondition of its fields within the limits of the server") {
            val seen = mutableListOf<EntityTagCondition?>()
            val reading = Operation(name = "read", method = "GET", path = PROBE) { request ->
                seen += request.preconditions().ifMatch
                reply(OK)
            }

            serve(routes = { operation(reading) }) {
                ask("GET", IF_MATCH to "\"a\"", IF_MATCH to "\"b\"")
                ask("GET")
            }

            seen shouldBe listOf(EntityTagCondition.Tags(listOf(EntityTag("a"), EntityTag("b"))), null)
        }
    }

    context("a representation known only by its last modification") {
        test("is answered with Last-Modified and without ETag") {
            val dated = Operation(name = "dated", method = "GET", path = PROBE) { request ->
                request.conditional(RepresentationState(lastModified = HttpDate(EPOCH))) { reply(OK) }
            }

            serve(routes = { operation(dated) }) {
                val response = ask("GET")

                response.headers["ETag"] shouldBe null
                response.headers["Last-Modified"] shouldBe MODIFIED
            }
        }
    }

    context("an informational answer") {
        test("is left without validators") {
            val informational = requestOf(method = "GET").conditional(state) { Reply(EARLY_HINTS) }

            informational.headers shouldBe emptyList()
        }
    }
})
