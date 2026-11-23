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

package com.davils.kapi.runtime.testing.oauth

import com.davils.kapi.runtime.client.oauth.Grant
import com.davils.kapi.runtime.testing.fake.FakeTokenSource
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val CLIENT: String = "client_id=client&client_secret=client-secret"

private const val CLIENT_ID: String = "client"

private const val TOKEN_URL: String = "https://login.example.com/oauth/token"

private const val OK: Int = 200

private const val BAD_REQUEST: Int = 400

private const val UNAUTHORIZED: Int = 401

private const val FOUR: Int = 4

private const val BASIC: String = "Basic Y2xpZW50OmNsaWVudC1zZWNyZXQ="

private suspend fun FakeAuthorizationServer.post(
    url: String,
    form: String,
    authorization: String? = null,
): Pair<Int, String> = httpClient().use { http ->
    val response = http.post(url) {
        authorization?.let { value -> header(key = HttpHeaders.Authorization, value = value) }
        setBody(form)
    }
    response.status.value to response.bodyAsText()
}

class AuthorizationEdgeTest : FunSpec({
    context("raw requests to the token endpoint") {
        test("are refused when they miss what their grant needs") {
            val server = fakeAuthorizationServer()
            val token = server.tokenUrl

            val answers = listOf(
                server.post(url = token, form = CLIENT),
                server.post(url = token, form = "grant_type=refresh_token&$CLIENT"),
                server.post(url = token, form = "grant_type=authorization_code&$CLIENT"),
                server.post(
                    url = token,
                    form = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Atoken-exchange&$CLIENT",
                ),
            )

            answers shouldBe listOf(
                BAD_REQUEST to """{"error":"unsupported_grant_type"}""",
                BAD_REQUEST to """{"error":"invalid_grant"}""",
                BAD_REQUEST to """{"error":"invalid_grant"}""",
                BAD_REQUEST to """{"error":"invalid_request"}""",
            )
        }

        test("authenticate the client only by its id and secret") {
            val server = fakeAuthorizationServer()
            val token = server.tokenUrl
            val grant = "grant_type=client_credentials"

            val statuses = listOf(
                server.post(url = token, form = grant),
                server.post(url = token, form = grant, authorization = "Bearer x"),
                server.post(url = token, form = grant, authorization = "Basic Y2xpZW50"),
                server.post(url = token, form = grant, authorization = "Basic JXp6OmNsaWVudC1zZWNyZXQ="),
                server.post(url = token, form = grant, authorization = BASIC),
            ).map { (status, _) -> status }

            statuses shouldBe List(FOUR) { UNAUTHORIZED } + OK
            server.requests.first().clientId shouldBe null
            server.requests.last { request -> !request.isAuthenticated }.clientId shouldBe "%zz"
        }

        test("revoke nothing without a token") {
            val server = fakeAuthorizationServer()

            server.post(url = server.revocationUrl, form = CLIENT) shouldBe (OK to "")
            server.revokedTokens shouldBe emptyList()
            server.requests.single().toString() shouldBe
                "EndpointRequest(Revocation, grant=none, fields=[client_id, client_secret])"
        }
    }

    context("the grants of the client runtime") {
        test("echo the scope and name the requested token type") {
            val server = fakeAuthorizationServer()
            val grant = Grant.TokenExchange(subject = FakeTokenSource("s"), requestedTokenType = "urn:example:jwt")

            server.httpClient().use { http -> server.tokens(http, grant) { scopes = setOf("read") }.token() }

            server.requests.single().field("scope") shouldBe "read"
            server.requests.single().field("requested_token_type") shouldBe "urn:example:jwt"
        }

        test("approve no authorization request of another client") {
            val server = fakeAuthorizationServer { clientSecret = null }
            val grant = Grant.AuthorizationCode(redirectUri = "com.example.app:/callback", redirect = server.redirect)

            val token = server.httpClient().use { http -> server.tokens(http, grant) { clientId = "other" }.token() }

            token shouldBe null
            server.requests shouldBe emptyList()
        }
    }

    context("the configuration") {
        test("refuses every issuer that is no https URL") {
            val issuers = listOf(
                "http://a.example.com",
                "https://",
                "https://a.example.com/",
                "https://a.example.com?x",
            )

            issuers.forEach { found ->
                shouldThrow<ValidationException> { fakeAuthorizationServer { issuer = found } }
            }
        }

        test("refuses times that are not above zero") {
            val configurations: List<AuthorizationServerBuilder.() -> Unit> = listOf(
                { interval = (-1).milliseconds },
                { deviceLifetime = Duration.ZERO },
                { lifetime = Duration.ZERO },
            )

            configurations.forEach { configure ->
                shouldThrow<ValidationException> {
                    fakeAuthorizationServer(
                        configure,
                    )
                }
            }
            fakeAuthorizationServer { lifetime = null }.clientId shouldBe CLIENT_ID
        }
    }

    context("recorded requests") {
        test("are values") {
            val request = EndpointRequest(
                endpoint = Endpoint.Token,
                url = TOKEN_URL,
                fields = listOf("grant_type" to "client_credentials"),
                clientId = CLIENT_ID,
                isAuthenticated = true,
            )
            val twin = EndpointRequest(
                endpoint = Endpoint.Token,
                url = TOKEN_URL,
                fields = listOf("grant_type" to "client_credentials"),
                clientId = CLIENT_ID,
                isAuthenticated = true,
            )

            request shouldBe twin
            request.hashCode() shouldBe twin.hashCode()
            request.equals("request") shouldBe false
            request shouldNotBe EndpointRequest(
                endpoint = Endpoint.Token,
                url = TOKEN_URL,
                fields = emptyList(),
                clientId = null,
                isAuthenticated = false,
            )
        }
    }
})
