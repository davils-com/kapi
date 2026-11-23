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

package com.davils.kapi.runtime.client.fixture

import com.davils.kapi.runtime.client.ApiClient
import com.davils.kapi.runtime.client.ClientSettingsBuilder
import com.davils.kapi.runtime.client.apiClient
import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.apiCall
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

internal const val BASE: String = "https://api.example.com/v1"

internal const val JSON: String = "application/json"

internal val pets: Call = apiCall(operation = "listPets", method = "GET") { path("/pets") }

internal class Exchanges(
    private val configure: ClientSettingsBuilder.() -> Unit = {},
    private val http: HttpClientConfig<*>.() -> Unit = { followRedirects = false },
    answer: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { ok("[]") },
) {
    private val seen: MutableList<HttpRequestData> = mutableListOf()

    private val engine: MockEngine = MockEngine { request ->
        seen += request
        answer(request)
    }

    val requests: List<HttpRequestData> get() = seen.toList()

    val urls: List<String> get() = seen.map { request -> request.url.toString() }

    suspend fun execute(call: Call): Exchange = withClient { client -> client.execute(call) }

    suspend fun <T> withClient(block: suspend (ApiClient) -> T): T =
        HttpClient(engine, http).use { client -> block(clientOf(client)) }

    private fun clientOf(client: HttpClient): ApiClient = apiClient(client) {
        baseUrl = BASE
        configure()
    }
}

internal fun MockRequestHandleScope.ok(content: String): HttpResponseData = respond(
    content = content,
    status = HttpStatusCode.OK,
    headers = headersOf(name = HttpHeaders.ContentType, value = JSON),
)

internal fun MockRequestHandleScope.unauthorized(): HttpResponseData =
    respond(content = "", status = HttpStatusCode.Unauthorized)
