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

import com.davils.kapi.runtime.client.oauth.AuthorizationResponse
import com.davils.kapi.runtime.client.oauth.ClientAuthentication
import com.davils.kapi.runtime.client.oauth.DevicePrompt
import com.davils.kapi.runtime.client.oauth.Discovery
import com.davils.kapi.runtime.client.oauth.Grant
import com.davils.kapi.runtime.client.oauth.OAuthTokens
import com.davils.kapi.runtime.client.oauth.Revocation
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kapi.runtime.testing.fake.FakeTokenSource
import com.davils.kapi.runtime.testing.fake.ManualClock
import com.davils.kapi.runtime.testing.fake.deterministicCrypto
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

private const val ISSUER: String = "https://login.example.com"

private const val FIRST: String = "access-token-1"

private const val SECOND: String = "access-token-2"

private const val CLIENT_CREDENTIALS: String = "client_credentials"

private const val CLIENT: String = "client_id=client&client_secret=client-secret"

private const val THREE: Int = 3

private val quiet: DevicePrompt = DevicePrompt { _ -> }

private suspend fun <T> FakeAuthorizationServer.using(
    grant: Grant = Grant.ClientCredentials,
    block: suspend (OAuthTokens) -> T,
): T = httpClient().use { http -> block(tokens(http, grant)) }

private fun device(): Grant.DeviceCode = Grant.DeviceCode(prompt = quiet, minimumInterval = 1.milliseconds)

class FakeAuthorizationServerTest : FunSpec({
    context("the client credentials grant") {
        test("issues counted tokens to the authenticated client") {
            val server = fakeAuthorizationServer()

            val token = server.using { tokens -> tokens.token()?.reveal() }

            token shouldBe FIRST
            server.issuedTokens shouldBe listOf(FIRST)
            val request = server.requestsTo(Endpoint.Token).single()
            request.grantType shouldBe CLIENT_CREDENTIALS
            request.clientId shouldBe "client"
            request.isAuthenticated shouldBe true
            request.toString() shouldBe "EndpointRequest(Token, grant=\"client_credentials\", fields=[grant_type])"
        }

        test("refuses a client with the wrong secret") {
            val server = fakeAuthorizationServer()

            val token = server.httpClient().use { http ->
                server.tokens(http) { authentication = ClientAuthentication.SecretPost(Secret("wrong")) }.token()
            }

            token shouldBe null
            server.requests.single().isAuthenticated shouldBe false
            server.issuedTokens shouldBe emptyList()
        }

        test("refuses as scripted, once per refusal") {
            val server = fakeAuthorizationServer {
                refuse(CLIENT_CREDENTIALS)
                refuse(grant = CLIENT_CREDENTIALS, error = "temporarily_unavailable")
            }

            val tokens = server.using { tokens -> List(3) { tokens.token()?.reveal() } }

            tokens shouldBe listOf(null, null, FIRST)
        }
    }

    context("the refresh token grant") {
        test("rotates the refresh token on every use") {
            val clock = ManualClock()
            val server = fakeAuthorizationServer { lifetime = 1.hours }

            val tokens = server.httpClient().use { http ->
                val source = server.tokens(http) { timeSource = clock }
                val first = source.token()?.reveal()
                clock.advance(2.hours)
                val second = source.token()?.reveal()
                listOf(first, second)
            }

            tokens shouldBe listOf(FIRST, SECOND)
            server.requests.map(EndpointRequest::grantType) shouldBe listOf(CLIENT_CREDENTIALS, "refresh_token")
            server.requests.last().field("refresh_token") shouldBe "refresh-token-1"
        }

        test("accepts a configured refresh token once") {
            val server = fakeAuthorizationServer {
                refreshTokens = setOf("given")
                isRefreshTokenIssued = false
                lifetime = null
            }
            val grant = Grant.RefreshToken(Secret("given"))

            val first = server.using(grant) { tokens -> tokens.token()?.reveal() }
            val second = server.using(grant) { tokens -> tokens.token()?.reveal() }

            first shouldBe FIRST
            second shouldBe null
        }
    }

    context("the device authorization grant") {
        test("answers the scripted polls before it grants") {
            val server = fakeAuthorizationServer { devicePolls = listOf(DevicePoll.Pending, DevicePoll.SlowDown) }

            val token = server.using(device()) { tokens -> tokens.token()?.reveal() }

            token shouldBe SECOND
            server.requestsTo(Endpoint.DeviceAuthorization) shouldHaveSize 1
            server.requestsTo(Endpoint.Token) shouldHaveSize THREE
            val polls = server.requestsTo(Endpoint.Token)
            polls.first().field("device_code") shouldBe "device-code-1"
        }

        test("stops at a denial") {
            val server = fakeAuthorizationServer { devicePolls = listOf(DevicePoll.Denied) }

            server.using(device()) { tokens -> tokens.token() } shouldBe null
            DevicePoll.entries.map(DevicePoll::error) shouldBe
                listOf("authorization_pending", "slow_down", "access_denied", "expired_token")
        }

        test("refuses an unknown device code") {
            val server = fakeAuthorizationServer()

            val answer = server.httpClient().use { http ->
                val response = http.post(server.tokenUrl) {
                    header(key = HttpHeaders.ContentType, value = "application/x-www-form-urlencoded")
                    setBody(
                        "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Adevice_code&device_code=x&$CLIENT",
                    )
                }
                response.bodyAsText()
            }

            answer shouldBe """{"error":"invalid_grant"}"""
        }
    }

    context("the token exchange grant") {
        test("exchanges a subject token and names the issued type") {
            val server = fakeAuthorizationServer()
            val grant = Grant.TokenExchange(subject = FakeTokenSource("subject"), audience = "api")

            val token = server.using(grant) { tokens -> tokens.token()?.reveal() }

            token shouldBe FIRST
            val request = server.requests.single()
            request.field("subject_token") shouldBe "subject"
            request.field("audience") shouldBe "api"
        }

        test("refuses a request without a subject token type") {
            val server = fakeAuthorizationServer()

            val answer = server.httpClient().use { http ->
                val response = http.post(server.tokenUrl) {
                    header(key = HttpHeaders.Authorization, value = "Basic Y2xpZW50OmNsaWVudC1zZWNyZXQ=")
                    header(key = HttpHeaders.ContentType, value = "application/x-www-form-urlencoded")
                    setBody("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Atoken-exchange&subject_token=s")
                }
                response.bodyAsText()
            }

            answer shouldBe """{"error":"invalid_request"}"""
        }
    }

    context("the authorization code grant") {
        test("grants the code that the redirect approved, with its verifier") {
            val server = fakeAuthorizationServer { clientSecret = null }
            val grant = Grant.AuthorizationCode(redirectUri = "com.example.app:/callback", redirect = server.redirect)

            val token = server.httpClient().use { http ->
                val tokens = server.tokens(http, grant) { crypto = deterministicCrypto() }
                tokens.token()?.reveal()
            }

            token shouldBe SECOND
            server.requests.single().field("code") shouldBe "authorization-code-1"
        }

        test("refuses a code without the verifier it was issued for") {
            val server = fakeAuthorizationServer()
            val grant = Grant.AuthorizationCode(redirectUri = "com.example.app:/callback", redirect = { request ->
                server.redirect.authorize(request)?.let { response ->
                    AuthorizationResponse(
                        code = Secret("authorization-code-9"),
                        state = response.state,
                    )
                }
            })

            server.using(grant) { tokens -> tokens.token() } shouldBe null
        }
    }

    context("the other endpoints") {
        test("revoke tokens and publish metadata for discovery") {
            val server = fakeAuthorizationServer { issuer = "$ISSUER/tenant" }

            val revocation = server.httpClient().use { http ->
                val tokens = server.tokens(http) {
                    tokenUrl = ""
                    issuer = server.issuer
                    revocationUrl = ""
                }
                tokens.token()
                tokens.metadata()?.tokenEndpoint shouldBe server.tokenUrl
                tokens.revoke()
            }

            revocation shouldBe Revocation.Revoked
            server.revokedTokens shouldBe listOf("refresh-token-1")
            server.requests.map(EndpointRequest::endpoint) shouldBe
                listOf(Endpoint.Metadata, Endpoint.Token, Endpoint.Revocation)
        }

        test("publish OpenID Connect metadata and answer unknown URLs with 404") {
            val server = fakeAuthorizationServer()

            val answers = server.httpClient().use { http ->
                val tokens = server.tokens(http) {
                    tokenUrl = ""
                    issuer = ISSUER
                    discovery = Discovery.OpenIdConnect
                }
                val discovered = tokens.metadata()?.issuer
                val status = http.get("$ISSUER/nothing").status
                listOf(discovered, status.value.toString())
            }

            answers shouldBe listOf(ISSUER, "404")
            server.requests.last().endpoint shouldBe Endpoint.Unknown
        }

        test("answer an unsupported grant") {
            val server = fakeAuthorizationServer()

            val answer = server.httpClient().use { http ->
                val response = http.post(server.tokenUrl) {
                    header(key = HttpHeaders.Authorization, value = "Basic !!!")
                    setBody("grant_type=password&client_id=client&client_secret=client-secret")
                }
                response.bodyAsText()
            }

            answer shouldBe """{"error":"unsupported_grant_type"}"""
        }
    }

    context("the configuration") {
        test("refuses an issuer that is no https URL, a blank client and negative times") {
            val failure = shouldThrow<ValidationException> {
                fakeAuthorizationServer {
                    issuer = "http://login.example.com/"
                    clientId = " "
                    interval = (-1).milliseconds
                    lifetime = Duration.ZERO
                }
            }

            failure.report.violations shouldHaveSize THREE
            fakeAuthorizationServer().toString() shouldContain "FakeAuthorizationServer(https://login.example.com"
            fakeAuthorizationServer().toString() shouldNotContain "client-secret"
        }
    }
})
