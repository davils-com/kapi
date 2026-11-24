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

package com.davils.kapi.runtime.client

import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.CredentialPlacement
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.SecurityUse
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.Tokens
import com.davils.kapi.runtime.client.fixture.ok
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.client.fixture.unauthorized
import com.davils.kapi.runtime.core.security.Credentials
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kapi.runtime.core.security.TokenSource
import com.davils.kapi.runtime.core.security.credentials
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpHeaders

private const val BEARER: String = "bearer"

private const val KEY: String = "apiKey"

private const val UNAUTHORIZED: Int = 401

private const val OK: Int = 200

private const val TOKEN: String = "t1"

private const val KEY_HEADER: String = "X-Api-Key"

private const val OLD: String = "old"

private const val FRESH: String = "new"

private val bearer: SecurityUse = SecurityUse(BEARER, CredentialPlacement.Bearer)

private val secured: Call = securedBy(listOf(bearer))

private fun securedBy(vararg alternatives: List<SecurityUse>): Call = apiCall(operation = "listPets", method = "GET") {
    path("/pets")
    alternatives.forEach { alternative -> security(alternative) }
}

private fun tokens(token: String?): Credentials = credentials {
    scheme(BEARER) { token?.let(::Secret) }
    scheme(KEY) { Secret("k1") }
}

private fun refreshing(source: TokenSource): Credentials = credentials { scheme(BEARER, source) }

private suspend fun statusOf(exchanges: Exchanges): Int =
    exchanges.execute(secured).shouldBeInstanceOf<Exchange.Answered>().status

class ApiClientCredentialsTest : FunSpec({
    context("a scheme the connection meets") {
        test("sends the call without a credential") {
            val exchanges = Exchanges(configure = { credentials = credentials { peer(BEARER) } })

            statusOf(exchanges) shouldBe OK
            exchanges.requests.single().headers[HttpHeaders.Authorization] shouldBe null
        }
    }

    context("the credentials of a call") {
        test("are placed where each scheme wants them") {
            val uses = listOf(
                bearer,
                SecurityUse(KEY, CredentialPlacement.Header(KEY_HEADER)),
                SecurityUse(KEY, CredentialPlacement.Query("key")),
                SecurityUse(KEY, CredentialPlacement.Cookie("key")),
            )
            val exchanges = Exchanges(configure = { credentials = tokens(TOKEN) })
            exchanges.execute(securedBy(uses))
            val request = exchanges.requests.single()

            request.headers[HttpHeaders.Authorization] shouldBe "Bearer $TOKEN"
            request.headers[KEY_HEADER] shouldBe "k1"
            request.url.parameters["key"] shouldBe "k1"
            request.headers[HttpHeaders.Cookie] shouldBe "key=k1"
        }

        test("are a basic field when the scheme is basic") {
            val exchanges = Exchanges(configure = { credentials = tokens("user:pass") })
            exchanges.execute(securedBy(listOf(SecurityUse(BEARER, CredentialPlacement.Basic))))

            exchanges.requests.single().headers[HttpHeaders.Authorization] shouldBe "Basic dXNlcjpwYXNz"
        }

        test("are those of the first alternative the client can meet") {
            val oauth = SecurityUse("oauth", CredentialPlacement.Bearer)
            val key = SecurityUse(KEY, CredentialPlacement.Header(KEY_HEADER))
            val exchanges = Exchanges(configure = { credentials = tokens(TOKEN) })
            exchanges.execute(securedBy(listOf(oauth), listOf(key), listOf(bearer)))
            val request = exchanges.requests.single()

            request.headers[KEY_HEADER] shouldBe "k1"
            request.headers[HttpHeaders.Authorization] shouldBe null
        }

        test("are none when the call asks for none") {
            val exchanges = Exchanges(configure = { credentials = tokens(TOKEN) })
            exchanges.execute(pets)

            exchanges.requests.single().headers[HttpHeaders.Authorization] shouldBe null
        }
    }

    context("a call the client has no credentials for") {
        test("is not sent when no alternative can be met") {
            val exchanges = Exchanges()

            exchanges.execute(secured) shouldBe Exchange.Failed(CallFailure.CredentialMissing)
            exchanges.requests.shouldBeEmpty()
        }

        test("is not sent when a source has no token or one no field may carry") {
            val missing = Exchanges(configure = { credentials = tokens(null) })
            val malformed = Exchanges(configure = { credentials = tokens("a\nb") })
            val both = securedBy(listOf(bearer, SecurityUse(KEY, CredentialPlacement.Header(KEY_HEADER))))

            missing.execute(secured) shouldBe Exchange.Failed(CallFailure.CredentialMissing)
            malformed.execute(both) shouldBe Exchange.Failed(CallFailure.CredentialMalformed(BEARER))
            malformed.requests.shouldBeEmpty()
        }

        test("is not sent in a cookie when it would end the cookie or start another") {
            val cookie = securedBy(listOf(SecurityUse(BEARER, CredentialPlacement.Cookie("session"))))

            listOf("a; admin=1", "a,b", "a b", "\"a\"", "a\\b").forEach { token ->
                val exchanges = Exchanges(configure = { credentials = tokens(token) })

                exchanges.execute(cookie) shouldBe Exchange.Failed(CallFailure.CredentialMalformed(BEARER))
                exchanges.requests.shouldBeEmpty()
            }
        }
    }

    context("a call the server rejects") {
        test("is sent once more after the source refreshed its token") {
            val source = Tokens(listOf(OLD, FRESH))
            val exchanges = Exchanges(configure = { credentials = refreshing(source) }) { request ->
                val isFresh = request.headers[HttpHeaders.Authorization] == "Bearer $FRESH"
                ok("[]").takeIf { isFresh } ?: unauthorized()
            }

            statusOf(exchanges) shouldBe OK
            exchanges.requests.size shouldBe 2
            source.rejected shouldBe listOf(Secret(OLD))
        }

        test("is not sent again when the source refreshed into no token") {
            val exchanges = Exchanges(configure = { credentials = refreshing(Tokens(listOf(OLD, null))) }) {
                unauthorized()
            }

            exchanges.execute(secured) shouldBe Exchange.Failed(CallFailure.CredentialMissing)
            exchanges.requests.size shouldBe 1
        }

        test("is answered as rejected when the call carried nothing to refresh, or nothing can be refreshed") {
            val open = Exchanges { unauthorized() }
            val fixed = Exchanges(configure = { credentials = tokens(TOKEN) }) { unauthorized() }

            open.execute(pets).shouldBeInstanceOf<Exchange.Answered>().status shouldBe UNAUTHORIZED
            statusOf(fixed) shouldBe UNAUTHORIZED
            open.requests.size shouldBe 1
            fixed.requests.size shouldBe 1
        }

        test("is answered as rejected when the fresh token is rejected as well") {
            val exchanges = Exchanges(configure = { credentials = refreshing(Tokens(listOf(OLD, FRESH))) }) {
                unauthorized()
            }

            statusOf(exchanges) shouldBe UNAUTHORIZED
            exchanges.requests.size shouldBe 2
        }
    }
})
