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

package com.davils.kapi.runtime.testing.contract

import com.davils.kapi.runtime.core.framing.ServerSentEvent
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.problem.Problem
import com.davils.kapi.runtime.testing.fixture.BASE_URL
import com.davils.kapi.runtime.testing.fixture.PETSTORE
import com.davils.kapi.runtime.testing.fixture.Pet
import com.davils.kapi.runtime.testing.fixture.documentOf
import com.davils.kapi.runtime.testing.mock.MockApi
import com.davils.kapi.runtime.testing.mock.MockApiBuilder
import com.davils.kapi.runtime.testing.mock.mockApi
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.serialization.builtins.ListSerializer

private const val ACCEPT: String = "Accept"

private const val REQUEST_ID: String = "X-Request-Id"

private const val TOTAL: String = "X-Total"

private const val JSON: String = "application/json"

private const val CREATED: Int = 201

private const val OK: Int = 200

private const val NO_CONTENT: Int = 204

private const val TEAPOT: Int = 418

private const val NOT_FOUND: Int = 404

private const val PETS: String = "/pets"

private const val PET: String = "/pets/{id}"

private const val ONE: String = "$BASE_URL/pets/1"

private const val LOWER_GET: String = "get"

private val rex: Pet = Pet(id = 1, name = "Rex")

private fun petstore(configure: MockApiBuilder.() -> Unit): MockApi = mockApi {
    basePath = "/v1"
    configure()
}

private suspend fun MockApi.calls(block: suspend (HttpClient) -> Unit): MockApi {
    httpClient().use { http -> block(http) }
    return this
}

private suspend fun reportOf(api: MockApi): ContractReport = contractCheck(documentOf(PETSTORE)).check(api.exchanges)

class ContractCheckTest : FunSpec({
    context("exchanges that keep the contract") {
        test("conform, with the paths of the servers stripped") {
            val api = petstore {
                get(PETS) {
                    header(key = TOTAL, value = "1")
                    respond(status = OK, serializer = ListSerializer(Pet.serializer()), value = listOf(rex))
                }
                post(PETS) { respond(CREATED, body<Pet>()) }
                get(PET) { respond(OK, rex) }
                delete(PET) { respond(status = NO_CONTENT) }
                get("/pets/{id}/events") { respondEvents(listOf(ServerSentEvent(data = "born"))) }
                get("/pets/records") { respondRecords(Pet.serializer(), listOf(rex, rex)) }
            }.calls { http ->
                http.get("$BASE_URL/pets?limit=10&tags=a&tags=b") { header(key = ACCEPT, value = JSON) }
                http.post("$BASE_URL/pets") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"id":2,"name":"Fido"}""")
                }
                http.get(ONE) { header(key = REQUEST_ID, value = "abc") }
                http.delete(ONE)
                http.get("$BASE_URL/pets/1/events")
                http.get("$BASE_URL/pets/records")
            }

            val report = reportOf(api)

            report.render() shouldBe "6 exchanges checked, 0 findings"
            report.isConforming shouldBe true
            report.exchanges shouldBe api.exchanges.size
        }

        test("pass assertConforms of the mock API") {
            val api = petstore { delete(PET) { respond(status = NO_CONTENT) } }
                .calls { http -> http.delete("$BASE_URL/pets/7") }

            api.assertConforms(documentOf(PETSTORE)).isConforming shouldBe true
        }
    }

    context("exchanges that break the contract") {
        test("match the methods of the document case-sensitively, so get is no GET") {
            val api = petstore { on(method = LOWER_GET, path = PETS) { respond(OK, listOf(rex)) } }
                .calls { http -> http.request(BASE_URL + PETS) { method = HttpMethod(LOWER_GET) } }

            reportOf(api).codes shouldBe listOf(ContractCodes.METHOD_UNDECLARED)
        }

        test("are reported with their codes, places and locations") {
            val api = petstore {
                get(PETS) { respond(OK, listOf(Pet(id = 0, name = ""))) }
                post(PETS) { respondText(status = CREATED, text = "{", mediaType = MediaType.Json) }
                get(PET) { respond(TEAPOT, rex) }
                put(PET) { respond(status = NO_CONTENT) }
                get("/unknown") { respond(status = OK) }
                delete(PET) { respondText(status = NO_CONTENT, text = "gone") }
            }.calls { http ->
                http.get("$BASE_URL/pets?limit=1000") { header(key = ACCEPT, value = JSON) }
                http.post("$BASE_URL/pets") {
                    contentType(ContentType.Text.Plain)
                    setBody("Rex")
                }
                http.get("$BASE_URL/pets/zero")
                http.put(ONE)
                http.get("$BASE_URL/unknown")
                http.delete(ONE)
            }

            val report = reportOf(api)

            report.codes shouldBe listOf(
                ContractCodes.PARAMETER_INVALID,
                ContractCodes.HEADER_MISSING,
                ContractCodes.BODY_INVALID,
                ContractCodes.BODY_INVALID,
                ContractCodes.CONTENT_TYPE_UNDECLARED,
                ContractCodes.BODY_MALFORMED,
                ContractCodes.PARAMETER_INVALID,
                ContractCodes.PARAMETER_MISSING,
                ContractCodes.CONTENT_TYPE_UNDECLARED,
                ContractCodes.METHOD_UNDECLARED,
                ContractCodes.OPERATION_UNKNOWN,
                ContractCodes.BODY_UNDECLARED,
            )
            val first = report.findings.first()
            first.exchange shouldBe 0
            first.side shouldBe ExchangeSide.Request
            first.operation shouldBe "GET /pets"
            first.subject shouldBe "query parameter limit"
            val location = first.location.shouldNotBeNull()
            location.pointer.text shouldBe "/paths/~1pets/get/parameters/0"
            report.findings[2].instance?.text shouldBe "/0/id"
            report.render() shouldContain "#0 request GET /pets, query parameter limit: [kapi.testing.contract."
            shouldThrow<AssertionError> { report.assertConforms() }.message shouldContain "12 findings"
        }

        test("conform when a problem answers within a documented range") {
            val api = petstore {
                get(PET) { respondProblem(Problem(title = "Gone", status = NOT_FOUND)) }
            }.calls { http -> http.get("$BASE_URL/pets/3") { header(key = REQUEST_ID, value = "abcd") } }

            reportOf(api).isConforming shouldBe true
        }
    }
})
