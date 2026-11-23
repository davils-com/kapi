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

package com.davils.kapi.runtime.client.oauth.fixture

import com.davils.kapi.runtime.client.oauth.OAuthSettingsBuilder
import com.davils.kapi.runtime.client.oauth.OAuthTokens
import com.davils.kapi.runtime.client.oauth.oauthTokens
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

internal const val TOKEN_URL: String = "https://login.example.com/oauth/token"

internal const val CLIENT: String = "kapi"

internal class TokenServer(
    private val configure: OAuthSettingsBuilder.() -> Unit = {},
    answer: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { granted("t1") },
) {
    private val seen: MutableList<HttpRequestData> = mutableListOf()

    private val sent: MutableList<String> = mutableListOf()

    private val engine: MockEngine = MockEngine { request ->
        seen += request
        sent += request.body.toByteArray().decodeToString()
        answer(request)
    }

    val requests: List<HttpRequestData> get() = seen.toList()

    val urls: List<String> get() = seen.map { request -> request.url.toString() }

    val forms: List<String> get() = sent.toList()

    suspend fun <T> withTokens(block: suspend (OAuthTokens) -> T): T =
        HttpClient(engine) { followRedirects = false }.use { http -> block(tokensOf(http)) }

    private fun tokensOf(http: HttpClient): OAuthTokens = oauthTokens(http) {
        tokenUrl = TOKEN_URL
        clientId = CLIENT
        configure()
    }
}

internal fun MockRequestHandleScope.json(
    content: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData = respond(
    content = content,
    status = status,
    headers = headersOf(name = HttpHeaders.ContentType, value = "application/json"),
)

internal fun MockRequestHandleScope.granted(token: String, more: String = ""): HttpResponseData =
    json("""{"access_token":"$token","token_type":"Bearer"$more}""")

internal fun MockRequestHandleScope.error(
    code: String,
    status: HttpStatusCode = HttpStatusCode.BadRequest,
): HttpResponseData = json(content = """{"error":"$code"}""", status = status)
