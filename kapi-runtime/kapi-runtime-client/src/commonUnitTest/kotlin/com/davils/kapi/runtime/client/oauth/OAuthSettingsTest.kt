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
import com.davils.kapi.runtime.client.oauth.fixture.TOKEN_URL
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.krypt.jose.jws.JwsAlgorithm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val SECRET: String = "s3cr&t"

private const val LONG: String = "a-client-secret-of-thirty-two-by"

private const val ISSUER: String = "https://login.example.com"

private val accepted: List<String> = listOf(
    TOKEN_URL,
    ISSUER,
    "http://localhost:8080/token",
    "http://127.0.0.1/token",
    "http://[::1]:9000/token",
)

private val refusedUrls: List<String> = listOf(
    "",
    "http://login.example.com/token",
    "https://login.example.com/token?tenant=a",
    "ftp://login.example.com/token",
    "http://localhost.example.com/token",
)

private val redirect: AuthorizationRedirect = AuthorizationRedirect { _ -> null }

private fun codesOf(configure: OAuthSettingsBuilder.() -> Unit): List<ViolationCode> {
    val builder = OAuthSettingsBuilder(isFollowingRedirects = false)
    builder.tokenUrl = TOKEN_URL
    builder.clientId = CLIENT
    builder.authentication = ClientAuthentication.SecretBasic(Secret(SECRET))
    builder.configure()
    return builder.validate().violations.map { violation -> violation.code }
}

class OAuthSettingsTest : FunSpec({
    context("the address tokens come from") {
        accepted.forEach { url ->
            test("may be https anywhere and http on this machine: $url") {
                codesOf { tokenUrl = url }.shouldBeEmpty()
            }
        }

        refusedUrls.forEach { url ->
            test("is refused when a secret would travel unprotected or it carries a query: '$url'") {
                codesOf { tokenUrl = url } shouldBe listOf(OAuthViolationCodes.URL_INVALID)
            }
        }

        test("may be discovered from an issuer, whose other endpoints must be protected as well") {
            codesOf {
                tokenUrl = ""
                issuer = ISSUER
            }.shouldBeEmpty()
            codesOf { revocationUrl = "http://login.example.com/revoke" } shouldBe
                listOf(OAuthViolationCodes.URL_INVALID)
        }
    }

    context("the client that asks for tokens") {
        test("needs an id without control characters") {
            codesOf { clientId = " " } shouldBe listOf(OAuthViolationCodes.CLIENT_ID_INVALID)
            codesOf { clientId = "a\nb" } shouldBe listOf(OAuthViolationCodes.CLIENT_ID_INVALID)
        }

        test("needs credentials of its own for the client credentials grant, and none for a refresh token") {
            codesOf { authentication = ClientAuthentication.None } shouldBe
                listOf(OAuthViolationCodes.AUTHENTICATION_INVALID)
            codesOf {
                authentication = ClientAuthentication.None
                grant = Grant.RefreshToken(Secret("r1"))
            }.shouldBeEmpty()
        }

        test("needs a secret as long as the hash of its MAC, and an asymmetric algorithm for its key") {
            codesOf { authentication = ClientAuthentication.SecretJwt(Secret("short")) } shouldBe
                listOf(OAuthViolationCodes.AUTHENTICATION_INVALID)
            codesOf { authentication = ClientAuthentication.SecretJwt(Secret(LONG)) }.shouldBeEmpty()
            codesOf { authentication = ClientAuthentication.SecretJwt(Secret(LONG), JwsAlgorithm.ES256) } shouldBe
                listOf(OAuthViolationCodes.AUTHENTICATION_INVALID)
        }
    }

    context("the grant of a source") {
        test("needs the endpoints and the redirect URI it uses") {
            codesOf { grant = Grant.AuthorizationCode("com.example.app:/callback", redirect) } shouldBe
                listOf(OAuthViolationCodes.GRANT_INVALID)
            codesOf {
                authorizationUrl = "https://login.example.com/authorize"
                grant = Grant.AuthorizationCode("https://app.example.com/callback#x", redirect)
            } shouldBe listOf(OAuthViolationCodes.GRANT_INVALID)
            codesOf {
                authorizationUrl = "https://login.example.com/authorize"
                grant = Grant.AuthorizationCode("com.example.app:/callback", redirect)
            }.shouldBeEmpty()
            codesOf { grant = Grant.DeviceCode({ _ -> }) } shouldBe listOf(OAuthViolationCodes.GRANT_INVALID)
            codesOf {
                issuer = ISSUER
                grant = Grant.DeviceCode(prompt = { _ -> }, maxWait = Duration.ZERO)
            } shouldBe listOf(OAuthViolationCodes.GRANT_INVALID)
            codesOf { grant = Grant.TokenExchange(subject = { null }, subjectTokenType = " ") } shouldBe
                listOf(OAuthViolationCodes.GRANT_INVALID)
        }

        test("asks for scopes that are tokens of RFC 6749") {
            codesOf { scopes = setOf("read", "pets:write") }.shouldBeEmpty()
            codesOf { scopes = setOf("a b") } shouldBe listOf(OAuthViolationCodes.SCOPE_INVALID)
            codesOf { scopes = setOf("") } shouldBe listOf(OAuthViolationCodes.SCOPE_INVALID)
        }
    }

    context("the times of a source") {
        listOf((-1).seconds, Duration.INFINITE).forEach { time ->
            test("refuse a leeway of $time") {
                codesOf { leeway = time } shouldBe listOf(OAuthViolationCodes.TIME_INVALID)
            }
        }

        listOf(Duration.ZERO, Duration.INFINITE).forEach { time ->
            test("refuse a timeout of $time") {
                codesOf { timeout = time } shouldBe listOf(OAuthViolationCodes.TIME_INVALID)
            }
        }

        test("default to half a minute of leeway and of patience") {
            OAuthSettingsBuilder.DEFAULT_LEEWAY shouldBe 30.seconds
            OAuthSettingsBuilder.DEFAULT_TIMEOUT shouldBe 30.seconds
            codesOf { leeway = Duration.ZERO }.shouldBeEmpty()
        }
    }

    context("the source of an HttpClient that follows redirects itself") {
        test("is refused, because a redirect would carry the client secret wherever a server points") {
            val failure = HttpClient(MockEngine { respondOk() }).use { http ->
                shouldThrow<ValidationException> {
                    oauthTokens(http) {
                        tokenUrl = TOKEN_URL
                        clientId = CLIENT
                        authentication = ClientAuthentication.SecretPost(Secret(SECRET))
                    }
                }
            }

            failure.report.violations.map { violation -> violation.code } shouldBe
                listOf(OAuthViolationCodes.REDIRECTS_FOLLOWED)
        }
    }

    context("the values of a source") {
        test("never show a secret, a key or a token") {
            val authentications = listOf(
                ClientAuthentication.None,
                ClientAuthentication.SecretBasic(Secret(SECRET)),
                ClientAuthentication.SecretPost(Secret(SECRET)),
                ClientAuthentication.SecretJwt(Secret(SECRET)),
            )
            val grants = listOf(
                Grant.ClientCredentials,
                Grant.RefreshToken(Secret(SECRET)),
                Grant.AuthorizationCode("app:/cb", redirect),
                Grant.DeviceCode({ _ -> }),
                Grant.TokenExchange(subject = { null }),
            )

            (authentications + grants).forEach { value -> value.toString() shouldNotContain SECRET }
            authentications.map(ClientAuthentication::toString) shouldBe
                listOf("None", "SecretBasic(****)", "SecretPost(****)", "SecretJwt(HS256)")
            grants.map(Grant::name).last() shouldBe "urn:ietf:params:oauth:grant-type:token-exchange"
            ClientAuthentication.SecretBasic(Secret(SECRET)) shouldBe ClientAuthentication.SecretBasic(Secret(SECRET))
            ClientAuthentication.SecretPost(Secret(SECRET)).hashCode() shouldBe Secret(SECRET).hashCode()
            ClientAuthentication.SecretJwt(Secret(SECRET)) shouldBe ClientAuthentication.SecretJwt(Secret(SECRET))
            ClientAuthentication.SecretJwt(Secret(SECRET)).hashCode() shouldBe JwsAlgorithm.HS256.hashCode()
            ClientAuthentication.SecretBasic(Secret(SECRET)).hashCode() shouldBe Secret(SECRET).hashCode()
            Grant.RefreshToken(Secret(SECRET)) shouldBe Grant.RefreshToken(Secret(SECRET))
            Grant.RefreshToken(Secret(SECRET)).hashCode() shouldBe Secret(SECRET).hashCode()
            TokenTypes.JWT shouldBe "urn:ietf:params:oauth:token-type:jwt"
        }
    }
})
