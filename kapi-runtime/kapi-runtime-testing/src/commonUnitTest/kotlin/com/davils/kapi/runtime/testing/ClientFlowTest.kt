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

package com.davils.kapi.runtime.testing

import com.davils.kapi.runtime.client.ApiClient
import com.davils.kapi.runtime.client.apiClient
import com.davils.kapi.runtime.client.call.CredentialPlacement
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.SecurityUse
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.stream.StreamEvent
import com.davils.kapi.runtime.client.stream.events
import com.davils.kapi.runtime.client.stream.records
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.framing.ServerSentEvent
import com.davils.kapi.runtime.core.problem.Problem
import com.davils.kapi.runtime.core.security.RefreshingTokenSource
import com.davils.kapi.runtime.core.security.credentials
import com.davils.kapi.runtime.testing.contract.ContractCodes
import com.davils.kapi.runtime.testing.contract.assertConforms
import com.davils.kapi.runtime.testing.contract.contractCheck
import com.davils.kapi.runtime.testing.fake.FakeTokenSource
import com.davils.kapi.runtime.testing.fake.RefreshOutcome
import com.davils.kapi.runtime.testing.fixture.BASE_URL
import com.davils.kapi.runtime.testing.fixture.PETSTORE
import com.davils.kapi.runtime.testing.fixture.Pet
import com.davils.kapi.runtime.testing.fixture.documentOf
import com.davils.kapi.runtime.testing.mock.MockApi
import com.davils.kapi.runtime.testing.mock.mockApi
import com.davils.kapi.runtime.testing.oauth.fakeAuthorizationServer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.toList

private const val BEARER: String = "bearer"

private const val OK: Int = 200

private const val CREATED: Int = 201

private const val UNAUTHORIZED: Int = 401

private const val PET_ID: Int = 7

private const val GET: String = "GET"

private val rex: Pet = Pet(id = 1, name = "Rex")

private val fetched: Decoding<Pet> = Decoding.Decoded(rex.copy(id = PET_ID))

private val secured: List<SecurityUse> = listOf(SecurityUse(BEARER, CredentialPlacement.Bearer))

private fun petstore(accepted: String): MockApi = mockApi {
    basePath = "/v1"
    get("/pets/{id}") {
        val isAccepted = request.header(HttpHeaders.Authorization) == "Bearer $accepted"
        respondProblem(Problem(title = "Unauthorized", status = UNAUTHORIZED))
        if (isAccepted) respond(OK, rex.copy(id = pathParameters.getValue("id").toInt()))
    }
    post("/pets") { respond(CREATED, body<Pet>()) }
    get("/pets/{id}/events") { respondEvents(listOf(ServerSentEvent(data = "born"), ServerSentEvent(data = "fed"))) }
    get("/pets/records") { respondRecords(Pet.serializer(), listOf(rex, rex.copy(id = 2))) }
}

private suspend fun ApiClient.pet(id: Int): Exchange = execute(
    apiCall(operation = "getPet", method = GET) {
        path("/pets/$id")
        header(name = "X-Request-Id", value = "req-$id")
        security(secured)
    },
)

private suspend fun ApiClient.created(): Exchange.Answered {
    val body = encode(Pet.serializer(), Pet(id = 3, name = "Fido"))
    val call = apiCall(operation = "createPet", method = "POST") {
        path("/pets")
        body?.let(::body)
    }
    return execute(call).shouldBeInstanceOf<Exchange.Answered>()
}

private suspend fun flow(api: MockApi, source: RefreshingTokenSource): List<Any?> = api.httpClient().use { http ->
    val client = apiClient(http) {
        baseUrl = BASE_URL
        credentials = credentials { scheme(BEARER, source) }
    }
    val answered = client.pet(PET_ID).shouldBeInstanceOf<Exchange.Answered>()
    val created = client.created()
    val events = client.events(apiCall(operation = "petEvents", method = GET) { path("/pets/7/events") })
    val records = client.records(
        Pet.serializer(),
        apiCall(operation = "petRecords", method = GET) { path("/pets/records") },
    )
    val eventItems = events.toList().filterIsInstance<StreamEvent.Item<ServerSentEvent>>()
    val recordItems = records.toList().filterIsInstance<StreamEvent.Item<Pet>>()
    listOf(
        client.decode(Pet.serializer(), answered),
        created.status,
        eventItems.map { item -> item.value.data },
        recordItems.map { item -> item.value.id },
    )
}

class ClientFlowTest : FunSpec({
    context("a client of the client runtime against a mock API") {
        test("refreshes a rejected fake token, calls, streams and keeps the contract") {
            val api = petstore(accepted = "t2")
            val source = FakeTokenSource("t1", listOf(RefreshOutcome.Rotate("t2")))

            val results = flow(api, source)

            results shouldBe listOf(fetched, CREATED, listOf("born", "fed"), listOf(1, 2))
            source.rejected.map { secret -> secret?.reveal() } shouldBe listOf("t1")
            api.exchanges.map { exchange -> exchange.response.status } shouldBe listOf(
                UNAUTHORIZED,
                OK,
                CREATED,
                OK,
                OK,
            )
            api.assertConforms(documentOf(PETSTORE)).exchanges shouldBe api.exchanges.size
        }

        test("takes its tokens from the fake authorization server") {
            val server = fakeAuthorizationServer()
            val api = petstore(accepted = "access-token-1")

            val results = server.httpClient().use { http -> flow(api, server.tokens(http)) }

            results.first() shouldBe fetched
            server.issuedTokens shouldBe listOf("access-token-1")
        }

        test("reports a client that breaks the contract") {
            val api = petstore(accepted = "t1")
            api.httpClient().use { http ->
                val client = apiClient(http) { baseUrl = BASE_URL }
                client.execute(apiCall(operation = "getPet", method = GET) { path("/pets/0") })
            }

            val report = contractCheck(documentOf(PETSTORE)) { basePath = "/v1" }.check(api.exchanges)

            report.codes shouldBe listOf(ContractCodes.PARAMETER_INVALID, ContractCodes.PARAMETER_MISSING)
        }
    }
})
