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
import com.davils.kapi.runtime.core.conditional.RepresentationState
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.server.fixture.PROBE
import com.davils.kapi.runtime.server.fixture.problemOf
import com.davils.kapi.runtime.server.fixture.serve
import com.davils.kapi.runtime.server.operation
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.problem.ServerCodes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.io.bytestring.encodeToByteString

private const val FIRST_TWO: String = "bytes=0-1"

private const val CONTENT_RANGE: String = "Content-Range"

private const val OK: Int = 200

private const val PARTIAL: Int = 206

private const val NOT_SATISFIABLE: Int = 416

private const val CONTENT: String = "0123456789"

private val state: RepresentationState = RepresentationState(entityTag = EntityTag("v1"))

private fun ranged(method: String): Operation = Operation(name = "download", method = method, path = PROBE) { request ->
    request.ranged(content = CONTENT.encodeToByteString(), mediaType = MediaType.TextPlain, state = state)
}

private suspend fun ApplicationTestBuilder.fetch(
    range: String?,
    ifRange: String? = null,
    method: String = "GET",
): HttpResponse = client.request(PROBE) {
    this.method = HttpMethod(method)
    range?.let { value -> header("Range", value) }
    ifRange?.let { value -> header("If-Range", value) }
}

class RangeTest : FunSpec({
    context("a request for ranges of a representation") {
        test("gets the whole representation without a Range, with Accept-Ranges and the validators") {
            serve(routes = { operation(ranged("GET")) }) {
                val response = fetch(null)

                response.status.value shouldBe OK
                response.headers["Accept-Ranges"] shouldBe "bytes"
                response.headers["ETag"] shouldBe "\"v1\""
                response.bodyAsText() shouldBe CONTENT
            }
        }

        test("gets one range with Content-Range") {
            serve(routes = { operation(ranged("GET")) }) {
                val response = fetch("bytes=2-4")

                response.status.value shouldBe PARTIAL
                response.headers[CONTENT_RANGE] shouldBe "bytes 2-4/10"
                response.bodyAsText() shouldBe "234"
                fetch("bytes=-3").bodyAsText() shouldBe "789"
            }
        }

        test("gets several ranges as multipart/byteranges, overlapping ranges coalesced") {
            serve(routes = { operation(ranged("GET")) }) {
                val response = fetch("bytes=0-1, 5-6")
                val body = response.bodyAsText()

                response.status.value shouldBe PARTIAL
                response.headers["Content-Type"].orEmpty() shouldStartWith "multipart/byteranges; boundary="
                body shouldContain "Content-Range: bytes 0-1/10"
                body shouldContain "Content-Range: bytes 5-6/10"
                fetch("bytes=0-3, 2-5").headers[CONTENT_RANGE] shouldBe "bytes 0-5/10"
            }
        }

        test("is refused with 416 when no range lies inside the representation") {
            serve(routes = { operation(ranged("GET")) }) {
                val response = fetch("bytes=20-30")

                response.status.value shouldBe NOT_SATISFIABLE
                response.headers[CONTENT_RANGE] shouldBe "bytes */10"
                problemOf(response).code shouldBe ServerCodes.RANGE_NOT_SATISFIABLE
            }
        }

        test("gets the whole representation for a malformed Range, a stale or malformed If-Range or a method but GET") {
            serve(routes = { operation(ranged("GET")) }) {
                fetch("pages=1-2").status.value shouldBe OK
                fetch(FIRST_TWO, ifRange = "\"v0\"").status.value shouldBe OK
                fetch(FIRST_TWO, ifRange = "not a validator").status.value shouldBe OK
                fetch(FIRST_TWO, ifRange = "\"v1\"").status.value shouldBe PARTIAL
            }
            serve(routes = { operation(ranged("POST")) }) {
                fetch(FIRST_TWO, method = "POST").status.value shouldBe OK
            }
        }
    }
})
