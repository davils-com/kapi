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

import com.davils.kapi.runtime.client.dpop.dpopProofs
import com.davils.kapi.runtime.client.oauth.fixture.Jwts
import com.davils.kapi.runtime.client.oauth.fixture.TOKEN_URL
import com.davils.kapi.runtime.client.oauth.fixture.TokenServer
import com.davils.kapi.runtime.client.oauth.fixture.error
import com.davils.kapi.runtime.client.oauth.fixture.granted
import com.davils.kapi.runtime.client.oauth.fixture.json
import com.davils.kapi.runtime.core.destination.HostResolution
import com.davils.kapi.runtime.core.destination.destinationPolicy
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.krypt.core.algorithm.KeyType
import com.davils.krypt.jose.JoseKey
import com.davils.krypt.jose.jws.JwsAlgorithm
import com.davils.krypt.primitive.Crypto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val HOST: String = "https://login.example.com"

private const val DEVICE: String = "$HOST/device"

private const val REVOKE: String = "$HOST/revoke"

private const val ACTIVATE: String = "$HOST/activate"

private const val TOKEN: String = "t1"

private const val REFRESH: String = "r0"

private const val DPOP: String = "DPoP"

private const val HINT: String = "token_type_hint"

private const val IMMEDIATE: String = ""","interval":0"""

private val basic: ClientAuthentication = ClientAuthentication.SecretBasic(Secret("s"))

private val crypto: Crypto = Crypto()

private val quiet: DevicePrompt = DevicePrompt { _ -> }

private fun deviceAnswer(more: String = ""): String =
    """{"device_code":"d1","user_code":"U","verification_uri":"$ACTIVATE"$more}"""

private fun issued(
    document: String,
    configure: OAuthSettingsBuilder.() -> Unit = {},
    answer: suspend MockRequestHandleScope.() -> HttpResponseData = { granted(TOKEN) },
): TokenServer = TokenServer(
    configure = {
        tokenUrl = ""
        issuer = HOST
        authentication = basic
        configure()
    },
) { request -> json(document).takeIf { request.url.encodedPath.contains("/.well-known/") } ?: answer() }

private fun devicing(grant: Grant.DeviceCode, answer: suspend MockRequestHandleScope.() -> HttpResponseData) =
    TokenServer(
        configure = {
            deviceAuthorizationUrl = DEVICE
            authentication = ClientAuthentication.None
            this.grant = grant
        },
    ) { request -> answer().takeIf { request.url.toString() == DEVICE } ?: granted(TOKEN) }

private suspend fun privateKey(): JoseKey.Private = JoseKey.Private(crypto.keys.generate(KeyType.P256).privateKey)

class OAuthEdgeTest : FunSpec({
    context("the discovery of an issuer") {
        test("looks at the root of an issuer without a path") {
            val tokens = issued("""{"issuer":"$HOST","token_endpoint":"$TOKEN_URL"}""")

            tokens.withTokens { source -> source.token() } shouldBe Secret(TOKEN)
            tokens.urls.first() shouldBe "$HOST/.well-known/oauth-authorization-server"
        }

        test("finds nothing when the document is missing, out of reach or lacks the endpoint of the grant") {
            val missing = TokenServer(configure = {
                tokenUrl = ""
                issuer = HOST
                authentication = basic
            }) { respond(content = "", status = HttpStatusCode.NotFound) }
            val unreachable = TokenServer(configure = {
                tokenUrl = ""
                issuer = HOST
                authentication = basic
            }) { throw IOException("down") }
            val document = """{"issuer":"$HOST","token_endpoint":"$TOKEN_URL"}"""
            val coded = issued(document, configure = {
                grant = Grant.AuthorizationCode("app:/cb") { _ -> error("never asked") }
            })
            val device = issued(document, configure = {
                authentication = ClientAuthentication.None
                grant = Grant.DeviceCode(quiet)
            })

            listOf(missing, unreachable, coded, device).forEach { tokens ->
                tokens.withTokens { source -> source.token() } shouldBe null
            }
            issued(document).withTokens { source ->
                source.token()
                source.revoke()
            } shouldBe Revocation.Unsupported
        }
    }

    context("the device authorization") {
        test("is given up when the server refuses, cannot be reached, answers no JSON or an unusable grant") {
            val answers = listOf<suspend MockRequestHandleScope.() -> HttpResponseData>(
                { error("invalid_client") },
                { throw IOException("down") },
                { json("<html>") },
                { json(deviceAnswer()) },
                { json(deviceAnswer(""","expires_in":600""").replace(oldValue = "\"d1\"", newValue = "\"\"")) },
            )

            answers.forEach { answer ->
                devicing(Grant.DeviceCode(quiet), answer).withTokens { source -> source.token() } shouldBe null
            }
        }

        test("shows the complete URI only when it is protected, and waits five seconds unless told otherwise") {
            val shown = mutableListOf<DeviceAuthorization>()
            val prompt = DevicePrompt { authorization -> shown += authorization }
            val complete = ""","expires_in":600,"verification_uri_complete":"$ACTIVATE?c=U""""
            val plain = complete.replace(oldValue = "https://", newValue = "http://") + IMMEDIATE
            val fragment = complete.replace(oldValue = "c=U", newValue = "c=U#x") + IMMEDIATE
            val spaced = complete.replace(oldValue = "c=U", newValue = "c=U V") + IMMEDIATE
            val accented = complete.replace(oldValue = "c=U", newValue = "c=Ué") + IMMEDIATE
            listOf(complete, plain, fragment, spaced, accented).forEach { more ->
                val tokens = TokenServer(configure = {
                    deviceAuthorizationUrl = DEVICE
                    authentication = ClientAuthentication.None
                    grant = Grant.DeviceCode(prompt)
                }) { request -> json(deviceAnswer(more)).takeIf { request.url.toString() == DEVICE } ?: granted(TOKEN) }

                tokens.withTokens { source -> source.token() } shouldBe Secret(TOKEN)
                tokens.requests.size shouldBe 2
            }

            shown.map(
                DeviceAuthorization::verificationUriComplete,
            ) shouldBe listOf("$ACTIVATE?c=U", null, null, null, null)
        }
    }

    context("the token exchange") {
        test("asks for nothing when the subject fails, and names the type of an actor only with one") {
            val failing = TokenServer(configure = {
                authentication = basic
                grant = Grant.TokenExchange(subject = { error("vault down") })
            })
            val typed = TokenServer(configure = {
                authentication = basic
                grant = Grant.TokenExchange(subject = { Secret("u") }, actorTokenType = TokenTypes.JWT)
            })
            val untyped = TokenServer(configure = {
                authentication = basic
                grant = Grant.TokenExchange(subject = { Secret("u") }, actor = { Secret("a") })
            })

            failing.withTokens { source -> source.token() } shouldBe null
            failing.requests.size shouldBe 0
            listOf(typed, untyped).forEach { tokens -> tokens.withTokens { source -> source.token() } }
            Jwts.fields(typed.forms.single()).keys.contains("actor_token_type") shouldBe false
            Jwts.fields(untyped.forms.single()).keys.contains("actor_token_type") shouldBe false
            Jwts.fields(untyped.forms.single())["actor_token"] shouldBe "a"
        }
    }

    context("a request at an endpoint") {
        test("is not sent when the client cannot sign its assertion or the destination is refused") {
            val key = privateKey()
            val unsigned = TokenServer(configure = {
                authentication = ClientAuthentication.PrivateKeyJwt(key, JwsAlgorithm.ES384)
            })
            val refused = TokenServer(configure = {
                authentication = basic
                destinations = destinationPolicy { resolver = { HostResolution.Failed("down") } }
            })

            listOf(unsigned, refused).forEach { tokens ->
                tokens.withTokens { source -> source.token() } shouldBe null
                tokens.requests.size shouldBe 0
            }
        }

        test("goes without a DPoP proof that cannot be made, and fails for a failure without a name") {
            val pair = crypto.keys.generate(KeyType.P256)
            val proofs = dpopProofs(
                crypto = crypto,
                key = JoseKey.Private(pair.privateKey),
                publicKey = pair.publicKey,
                algorithm = JwsAlgorithm.ES384,
            )
            val unproven = TokenServer(configure = {
                authentication = basic
                dpop = proofs
            }) { json("""{"access_token":"t1","token_type":"DPoP"}""") }
            val nameless = TokenServer(configure = { authentication = basic }) {
                throw object : IllegalStateException() {}
            }

            unproven.withTokens { source -> source.token() } shouldBe Secret(TOKEN)
            unproven.requests.single().headers[DPOP] shouldBe null
            nameless.withTokens { source -> source.token() } shouldBe null
        }
    }

    context("the tokens of a source") {
        test("keep the grant when the server hands out an empty refresh token") {
            val tokens = TokenServer(
                configure = { authentication = basic },
            ) { granted(TOKEN, more = ""","refresh_token":""""") }

            tokens.withTokens { source ->
                source.token()
                source.grant.value
            } shouldBe Grant.ClientCredentials
        }

        test("keep a refresh token of the settings that the server calls invalid") {
            val tokens = TokenServer(configure = {
                authentication = ClientAuthentication.None
                grant = Grant.RefreshToken(Secret(REFRESH))
            }) { error("invalid_grant") }

            tokens.withTokens { source ->
                source.token()
                source.grant.value
            } shouldBe Grant.RefreshToken(Secret(REFRESH))
        }

        test("revoke the refresh token of the settings before any token came, and fail when out of reach") {
            val revoking = TokenServer(configure = {
                revocationUrl = REVOKE
                authentication = ClientAuthentication.None
                grant = Grant.RefreshToken(Secret(REFRESH))
            }) { respond(content = "", status = HttpStatusCode.OK) }
            val unreachable = TokenServer(configure = {
                revocationUrl = REVOKE
                authentication = basic
            }) { request ->
                if (request.url.toString() == REVOKE) throw IOException("down")
                granted(TOKEN)
            }

            revoking.withTokens { source -> source.revoke() } shouldBe Revocation.Revoked
            Jwts.fields(revoking.forms.single()) shouldBe mapOf(
                "token" to REFRESH,
                HINT to "refresh_token",
                "client_id" to "kapi",
            )
            unreachable.withTokens { source ->
                source.token()
                source.revoke()
            } shouldBe Revocation.Failed
        }
    }

    context("the settings of a source") {
        test("refuse a MAC for a private key, and accept grants whose endpoints the issuer publishes") {
            val key = privateKey()
            val redirect = AuthorizationRedirect { _ -> null }
            val settings = listOf<OAuthSettingsBuilder.() -> Unit>(
                { authentication = ClientAuthentication.PrivateKeyJwt(key, JwsAlgorithm.HS256) },
                {
                    issuer = HOST
                    grant = Grant.AuthorizationCode("app:/cb", redirect)
                },
                {
                    issuer = HOST
                    grant = Grant.DeviceCode(quiet)
                },
                { grant = Grant.DeviceCode(prompt = quiet, minimumInterval = (-1).milliseconds) },
                { grant = Grant.DeviceCode(prompt = quiet, minimumInterval = Duration.INFINITE) },
            )

            settings.map { configure -> codesOf(configure) } shouldBe listOf(
                listOf(OAuthViolationCodes.AUTHENTICATION_INVALID),
                emptyList(),
                emptyList(),
                listOf(OAuthViolationCodes.GRANT_INVALID),
                listOf(OAuthViolationCodes.GRANT_INVALID),
            )
        }

        test("refuse redirect URIs without a scheme, with a fragment or invisible characters, and odd scopes") {
            val redirect = AuthorizationRedirect { _ -> null }
            listOf(":/cb", "1app:/cb", "a_b:/cb", "app:/c b", "app").forEach { uri ->
                codesOf {
                    authorizationUrl = "$HOST/authorize"
                    grant = Grant.AuthorizationCode(uri, redirect)
                } shouldBe listOf(OAuthViolationCodes.GRANT_INVALID)
            }
            listOf("a\"b", "a\\b", "aé").forEach { scope ->
                codesOf { scopes = setOf(scope) } shouldBe listOf(OAuthViolationCodes.SCOPE_INVALID)
            }
        }
    }
})

private fun codesOf(configure: OAuthSettingsBuilder.() -> Unit): List<ViolationCode> {
    val builder = OAuthSettingsBuilder(isFollowingRedirects = false)
    builder.tokenUrl = TOKEN_URL
    builder.clientId = "kapi"
    builder.authentication = basic
    builder.configure()
    return builder.validate().violations.map { violation -> violation.code }
}
