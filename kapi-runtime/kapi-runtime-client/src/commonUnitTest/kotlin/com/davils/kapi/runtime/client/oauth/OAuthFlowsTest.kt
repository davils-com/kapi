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

package com.davils.kapi.runtime.client.oauth

import com.davils.kapi.runtime.client.oauth.fixture.CLIENT
import com.davils.kapi.runtime.client.oauth.fixture.Jwts
import com.davils.kapi.runtime.client.oauth.fixture.TOKEN_URL
import com.davils.kapi.runtime.client.oauth.fixture.TokenServer
import com.davils.kapi.runtime.client.oauth.fixture.error
import com.davils.kapi.runtime.client.oauth.fixture.granted
import com.davils.kapi.runtime.client.oauth.fixture.json
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kore.core.outcome.Outcome
import com.davils.krypt.core.SecretBytes
import com.davils.krypt.core.algorithm.DigestAlgorithm
import com.davils.krypt.core.algorithm.KeyType
import com.davils.krypt.jose.JoseKey
import com.davils.krypt.jose.jws.Jws
import com.davils.krypt.jose.jws.JwsAlgorithm
import com.davils.krypt.primitive.Crypto
import com.davils.krypt.primitive.key.SecretKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.io.bytestring.encodeToByteString
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val ISSUER: String = "https://login.example.com/tenant"

private const val AUTHORIZE: String = "https://login.example.com/authorize"

private const val DEVICE: String = "https://login.example.com/device"

private const val REVOKE: String = "https://login.example.com/revoke"

private const val REDIRECT: String = "com.example.app:/callback"

private const val CODE: String = "code-1"

private const val TOKEN: String = "t1"

private const val EVIL: String = "https://evil.example.com"

private const val CLIENT_ID: String = "client_id"

private const val LONG_SECRET: String = "a-client-secret-of-thirty-two-by"

private const val VERIFIER_LENGTH: Int = 43

private const val LIFETIME: Int = 600

private const val PENDING: String = "authorization_pending"

private const val DOCUMENT: String = """{"issuer":"$ISSUER","token_endpoint":"$TOKEN_URL",""" +
    """"authorization_endpoint":"$AUTHORIZE","device_authorization_endpoint":"$DEVICE",""" +
    """"revocation_endpoint":"$REVOKE","code_challenge_methods_supported":["S256"]}"""

private const val ISSUER_DOCUMENT: String = """{"issuer":"$ISSUER","token_endpoint":"$TOKEN_URL",""" +
    """"authorization_endpoint":"$AUTHORIZE","authorization_response_iss_parameter_supported":true}"""

private const val DEVICE_ANSWER: String = """{"device_code":"d1","user_code":"ABCD-EFGH",""" +
    """"verification_uri":"https://login.example.com/activate","expires_in":600,"interval":0}"""

private val base64: Base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

private val basic: ClientAuthentication = ClientAuthentication.SecretBasic(Secret("s"))

private fun discovering(answer: suspend MockRequestHandleScope.(String) -> HttpResponseData) = TokenServer(
    configure = {
        tokenUrl = ""
        issuer = ISSUER
        authentication = basic
    },
) { request ->
    val url = request.url.toString()
    json(DOCUMENT).takeIf { url.contains("/.well-known/") } ?: answer(url)
}

private fun revoking(status: HttpStatusCode, more: String = ""): TokenServer = discovering { url ->
    respond(content = "", status = status).takeIf { url == REVOKE } ?: granted(TOKEN, more = more)
}

private fun device(answer: suspend MockRequestHandleScope.() -> HttpResponseData, grant: Grant.DeviceCode) =
    TokenServer(
        configure = {
            deviceAuthorizationUrl = DEVICE
            authentication = ClientAuthentication.None
            this.grant = grant
        },
    ) { request -> json(DEVICE_ANSWER).takeIf { request.url.toString() == DEVICE } ?: answer() }

class OAuthFlowsTest : FunSpec({
    context("the document of an issuer") {
        test("is discovered once at the well-known place of RFC 8414 and names the endpoints") {
            val tokens = discovering { _ -> granted(TOKEN) }

            val found = tokens.withTokens { source ->
                source.token()
                source.token()
                source.metadata()
            }

            tokens.urls.first() shouldBe
                "https://login.example.com/.well-known/oauth-authorization-server/tenant"
            tokens.requests.size shouldBe 2
            found?.tokenEndpoint shouldBe TOKEN_URL
            found?.codeChallengeMethods shouldBe listOf("S256")
            found?.run { toString() } shouldBe "ServerMetadata($ISSUER)"
        }

        test("is discovered after the issuer for OpenID Connect") {
            val tokens = TokenServer(configure = {
                tokenUrl = ""
                issuer = ISSUER
                discovery = Discovery.OpenIdConnect
                authentication = basic
            }) { json(DOCUMENT) }

            tokens.withTokens { source -> source.metadata() }

            tokens.urls.single() shouldBe "$ISSUER/.well-known/openid-configuration"
        }

        test("is refused for another issuer, an unprotected endpoint or no answer, which leaves no token") {
            listOf(
                DOCUMENT.replace(oldValue = ISSUER, newValue = EVIL),
                DOCUMENT.replace(oldValue = REVOKE, newValue = "http://login.example.com/revoke"),
                """{"issuer":"$ISSUER"}""",
                "<html>",
            ).forEach { answer ->
                val tokens = TokenServer(configure = {
                    tokenUrl = ""
                    issuer = ISSUER
                    authentication = basic
                }) { json(answer) }

                tokens.withTokens { source -> listOf(source.metadata(), source.token()) } shouldBe listOf(null, null)
            }
            TokenServer(configure = { authentication = basic }).withTokens { source -> source.metadata() } shouldBe null
        }
    }

    context("the authorization code grant") {
        test("sends the user with a challenge and a state, and trades the code for a token with the verifier") {
            val requests = mutableListOf<AuthorizationRequest>()
            val tokens = TokenServer(configure = {
                authorizationUrl = AUTHORIZE
                scopes = setOf("openid")
                grant = Grant.AuthorizationCode(REDIRECT) { request ->
                    requests += request
                    AuthorizationResponse.fromRedirect("$REDIRECT?code=$CODE&state=${request.state}")
                }
            }) { granted(TOKEN, more = ""","refresh_token":"r1"""") }

            val grant = tokens.withTokens { source ->
                source.token() shouldBe Secret(TOKEN)
                source.grant.value
            }
            val request = requests.single()
            val query = Url(request.url).parameters
            val form = Jwts.fields(tokens.forms.single())
            val verifier = form.getValue("code_verifier")
            val digest = Crypto().digests.digest(verifier.encodeToByteString(), DigestAlgorithm.Sha256)

            query["response_type"] shouldBe "code"
            query[CLIENT_ID] shouldBe CLIENT
            query["redirect_uri"] shouldBe REDIRECT
            query["scope"] shouldBe "openid"
            query["code_challenge_method"] shouldBe "S256"
            query["code_challenge"] shouldBe base64.encode(digest.toByteArray())
            query["state"] shouldBe request.state
            form["code"] shouldBe CODE
            form["redirect_uri"] shouldBe REDIRECT
            verifier.length shouldBe VERIFIER_LENGTH
            grant shouldBe Grant.RefreshToken(Secret("r1"))
            request.toString() shouldBe "AuthorizationRequest($REDIRECT)"
        }

        test("gets no token for another state, another issuer, no answer or a failing redirect") {
            val answers = listOf<(AuthorizationRequest) -> AuthorizationResponse?>(
                { _ -> AuthorizationResponse(code = Secret(CODE), state = "forged") },
                { request ->
                    AuthorizationResponse(code = Secret(CODE), state = request.state, issuer = EVIL)
                },
                { _ -> null },
                { _ -> error("no browser") },
            )

            answers.forEach { answer ->
                val tokens = TokenServer(configure = {
                    authorizationUrl = AUTHORIZE
                    grant = Grant.AuthorizationCode(REDIRECT) { request -> answer(request) }
                })

                tokens.withTokens { source -> source.token() } shouldBe null
                tokens.requests.size shouldBe 0
            }
        }

        test("reads what the authorization server redirected back to") {
            val response = AuthorizationResponse.fromRedirect("$REDIRECT?code=c%201&state=s&iss=https%3A%2F%2Fa")

            response?.code shouldBe Secret("c 1")
            response?.issuer shouldBe "https://a"
            response?.run { toString() } shouldBe "AuthorizationResponse(s)"
            AuthorizationResponse.fromRedirect("$REDIRECT?error=access_denied&state=s") shouldBe null
            AuthorizationResponse.fromRedirect("$REDIRECT?state=s") shouldBe null
            AuthorizationResponse.fromRedirect("$REDIRECT?code=c") shouldBe null
            AuthorizationResponse.fromRedirect("$REDIRECT?code=%ZZ&state=s") shouldBe null
            AuthorizationResponse.fromRedirect("$REDIRECT?code=c&code=d&state=s") shouldBe null
            AuthorizationResponse.fromRedirect("$REDIRECT?code=c&state=s&state=t") shouldBe null
            AuthorizationResponse.fromRedirect("$REDIRECT?code=c&state=s&iss=a&iss=b") shouldBe null
        }

        test("needs the issuer in the response when the metadata of the server announce it") {
            val answers = listOf<Pair<String?, String?>>(null to null, ISSUER to TOKEN)

            answers.forEach { (issuer, expected) ->
                val tokens = TokenServer(configure = {
                    tokenUrl = ""
                    this.issuer = ISSUER
                    authentication = basic
                    grant = Grant.AuthorizationCode(REDIRECT) { request ->
                        AuthorizationResponse(code = Secret(CODE), state = request.state, issuer = issuer)
                    }
                }) { request ->
                    val isDocument = request.url.encodedPath.contains("/.well-known/")
                    json(ISSUER_DOCUMENT).takeIf { isDocument } ?: granted(TOKEN)
                }

                tokens.withTokens { source -> source.token()?.reveal() } shouldBe expected
            }
        }
    }

    context("the device authorization grant") {
        test("shows the user code and polls until the user authorized the device, slowing down when asked") {
            val shown = mutableListOf<DeviceAuthorization>()
            val polls = ArrayDeque(listOf(PENDING, "slow_down", null))
            val prompt = DevicePrompt { authorization -> shown += authorization }
            val tokens = device(
                answer = { polls.removeFirst()?.let { code -> error(code) } ?: granted(TOKEN) },
                grant = Grant.DeviceCode(prompt = prompt, minimumInterval = 1.milliseconds),
            )

            tokens.withTokens { source -> source.token() } shouldBe Secret(TOKEN)
            shown.single().userCode shouldBe "ABCD-EFGH"
            shown.single().expiresIn shouldBe LIFETIME.seconds
            shown.single().toString() shouldBe "DeviceAuthorization(https://login.example.com/activate, 10m)"
            Jwts.fields(tokens.forms.last()) shouldBe mapOf(
                "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                "device_code" to "d1",
                CLIENT_ID to CLIENT,
            )
        }

        test("stops when the user denies, the code expired or the server cannot authorize the device") {
            val quiet = DevicePrompt { _ -> }
            val denied = device(
                answer = { error("access_denied") },
                grant = Grant.DeviceCode(prompt = quiet, minimumInterval = 1.milliseconds),
            )
            val expired = device(
                answer = { error(PENDING) },
                grant = Grant.DeviceCode(prompt = quiet, maxWait = 1.milliseconds, minimumInterval = 5.milliseconds),
            )
            val unprotected = TokenServer(configure = {
                deviceAuthorizationUrl = DEVICE
                authentication = ClientAuthentication.None
                grant = Grant.DeviceCode(prompt = quiet)
            }) { json(DEVICE_ANSWER.replace(oldValue = "https://login", newValue = "http://login")) }
            val broken = DevicePrompt { _ -> error("no screen") }
            val failing = device(answer = { granted(TOKEN) }, grant = Grant.DeviceCode(prompt = broken))

            listOf(denied, expired, unprotected, failing).forEach { tokens ->
                tokens.withTokens { source -> source.token() } shouldBe null
            }
            unprotected.requests.size shouldBe 1
        }
    }

    context("the token exchange") {
        test("trades the token of the subject and the actor for one of this client") {
            val tokens = TokenServer(configure = {
                authentication = ClientAuthentication.SecretPost(Secret("s"))
                grant = Grant.TokenExchange(
                    subject = { Secret("user-token") },
                    actor = { Secret("actor-token") },
                    actorTokenType = TokenTypes.JWT,
                    audience = "pets",
                    requestedTokenType = TokenTypes.ACCESS_TOKEN,
                )
            })

            tokens.withTokens { source -> source.token() } shouldBe Secret(TOKEN)
            Jwts.fields(tokens.forms.single()) shouldBe mapOf(
                "grant_type" to "urn:ietf:params:oauth:grant-type:token-exchange",
                "subject_token" to "user-token",
                "subject_token_type" to TokenTypes.ACCESS_TOKEN,
                "actor_token" to "actor-token",
                "actor_token_type" to TokenTypes.JWT,
                "audience" to "pets",
                "requested_token_type" to TokenTypes.ACCESS_TOKEN,
                CLIENT_ID to CLIENT,
                "client_secret" to "s",
            )
        }

        test("asks for nothing without the token of the subject or of a named actor") {
            listOf(
                Grant.TokenExchange(subject = { null }),
                Grant.TokenExchange(subject = { Secret("u") }, actor = { error("vault down") }),
            ).forEach { exchange ->
                val tokens = TokenServer(configure = {
                    authentication = ClientAuthentication.SecretPost(Secret("s"))
                    grant = exchange
                })

                tokens.withTokens { source -> source.token() } shouldBe null
                tokens.requests.size shouldBe 0
            }
        }
    }

    context("a client that authenticates with a JWT") {
        test("signs an assertion for the issuer with its private key") {
            val crypto = Crypto()
            val pair = crypto.keys.generate(KeyType.P256)
            val key = JoseKey.Private(pair.privateKey)
            val tokens = TokenServer(configure = {
                issuer = ISSUER
                authentication = ClientAuthentication.PrivateKeyJwt(key, JwsAlgorithm.ES256)
                this.crypto = crypto
            })

            tokens.withTokens { source -> source.token() }
            val form = Jwts.fields(tokens.forms.single())
            val assertion = form.getValue("client_assertion")
            val keys = listOf(JoseKey.Public(pair.publicKey))
            val verified = Jws(crypto).verify(assertion, keys, setOf(JwsAlgorithm.ES256))
            val claims = Jwts.claims(assertion)

            form["client_assertion_type"] shouldBe "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
            verified.shouldBeInstanceOf<Outcome.Success<*>>()
            Jwts.text(claims, "iss") shouldBe CLIENT
            Jwts.text(claims, "sub") shouldBe CLIENT
            Jwts.text(claims, "aud") shouldBe ISSUER
            tokens.urls.first() shouldBe TOKEN_URL
        }

        test("signs an assertion for the token endpoint with an HMAC of its secret") {
            val crypto = Crypto()
            val tokens = TokenServer(configure = {
                authentication = ClientAuthentication.SecretJwt(Secret(LONG_SECRET))
                this.crypto = crypto
            })

            tokens.withTokens { source -> source.token() }
            val assertion = Jwts.fields(tokens.forms.single()).getValue("client_assertion")
            val key = JoseKey.Secret(SecretKey.of(SecretBytes.copyOf(LONG_SECRET.encodeToByteArray())))
            val verified = Jws(crypto).verify(assertion, listOf(key), setOf(JwsAlgorithm.HS256))

            verified.shouldBeInstanceOf<Outcome.Success<*>>()
            Jwts.text(Jwts.claims(assertion), "aud") shouldBe TOKEN_URL
        }
    }

    context("a revocation") {
        test("revokes the refresh token the source holds and forgets its tokens") {
            val tokens = revoking(HttpStatusCode.OK, more = ""","refresh_token":"r1"""")

            val revocation = tokens.withTokens { source ->
                source.token()
                listOf(source.revoke(), source.grant.value, source.revoke())
            }

            revocation shouldBe listOf(Revocation.Revoked, Grant.ClientCredentials, Revocation.NothingHeld)
            Jwts.fields(tokens.forms[2]) shouldBe mapOf("token" to "r1", "token_type_hint" to "refresh_token")
        }

        test("revokes the access token without a refresh token, and keeps it when the server refuses") {
            val revoked = revoking(HttpStatusCode.OK)
            val refusing = revoking(HttpStatusCode.ServiceUnavailable)
            val unsupported = TokenServer(configure = { authentication = basic })

            revoked.withTokens { source ->
                source.token()
                source.revoke()
            } shouldBe Revocation.Revoked
            Jwts.fields(revoked.forms[2])["token_type_hint"] shouldBe "access_token"
            refusing.withTokens { source ->
                source.token()
                listOf(source.revoke(), source.token())
            } shouldBe listOf(Revocation.Failed, Secret(TOKEN))
            unsupported.withTokens { source ->
                source.token()
                source.revoke()
            } shouldBe Revocation.Unsupported
        }
    }
})
