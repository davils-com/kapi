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
import com.davils.kapi.runtime.client.oauth.fixture.TokenServer
import com.davils.kapi.runtime.client.oauth.fixture.error
import com.davils.kapi.runtime.client.oauth.fixture.granted
import com.davils.kapi.runtime.client.oauth.fixture.json
import com.davils.kapi.runtime.core.security.Secret
import com.davils.konfetti.core.eventBus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

private const val OVERSIZED: Int = 70_000

private const val CREDENTIALS: String = "grant_type=client_credentials"

private const val TOKEN: String = "t1"

private const val BEARER: String = """{"access_token":"t1","token_type":"bearer"}"""

private const val BASIC: String = "Basic a2FwaTpzM2NyJTI2dA=="

private val secret: Secret = Secret("s3cr&t")

private fun server(configure: OAuthSettingsBuilder.() -> Unit = {}): TokenServer = TokenServer(
    configure = {
        authentication = ClientAuthentication.SecretBasic(secret)
        configure()
    },
)

private fun answering(answer: suspend MockRequestHandleScope.() -> HttpResponseData) =
    TokenServer(configure = { authentication = ClientAuthentication.SecretBasic(secret) }) { answer() }

private val refusals: Map<String, String> = mapOf(
    "a token of another type" to """{"access_token":"t1","token_type":"mac"}""",
    "an empty token" to """{"access_token":"","token_type":"bearer"}""",
    "no token at all" to """{"token_type":"Bearer"}""",
    "no JSON" to "<html>",
)

class OAuthTokensTest : FunSpec({
    context("a token for the credentials of a client") {
        test("is asked for with the client in a basic field and the scopes it wants") {
            val tokens = server { scopes = setOf("read", "write") }

            tokens.withTokens { source -> source.token() } shouldBe Secret(TOKEN)
            tokens.forms shouldBe listOf("$CREDENTIALS&scope=read+write")
            tokens.requests.single().headers[HttpHeaders.Authorization] shouldBe BASIC
        }

        test("is asked for with the client in the form when the server wants it there") {
            val tokens = TokenServer(configure = { authentication = ClientAuthentication.SecretPost(secret) })

            tokens.withTokens { source -> source.token() }

            tokens.forms shouldBe listOf("$CREDENTIALS&client_id=kapi&client_secret=s3cr%26t")
            tokens.requests.single().headers[HttpHeaders.Authorization] shouldBe null
        }

        test("is kept until it expires, however often it is asked for") {
            val tokens = server()

            tokens.withTokens { source -> listOf(source.token(), source.token()) } shouldBe
                listOf(Secret(TOKEN), Secret(TOKEN))
            tokens.requests.size shouldBe 1
        }
    }

    context("a token that expires") {
        test("is asked for again before its time is up, by the leeway") {
            val clock = TestTimeSource()
            val tokens = TokenServer(configure = {
                authentication = ClientAuthentication.SecretBasic(secret)
                leeway = 10.seconds
                timeSource = clock
            }) { granted(TOKEN, more = ""","expires_in":"60"""") }

            tokens.withTokens { source ->
                source.token()
                clock += 49.seconds
                source.token()
                clock += 2.seconds
                source.token()
            }

            tokens.requests.size shouldBe 2
        }

        test("is kept for good when the server names no time") {
            val clock = TestTimeSource()
            val tokens = server { timeSource = clock }

            tokens.withTokens { source ->
                source.token()
                clock += 1_000.minutes
                source.token()
            }

            tokens.requests.size shouldBe 1
        }
    }

    context("a token the server rejected") {
        test("is replaced by a fresh one, unless another call already did or the server hands out the same") {
            val answers = ArrayDeque(listOf("t1", "t2"))
            val rotating = answering { granted(answers.removeFirst()) }
            val outcome = rotating.withTokens { source ->
                val first = source.token()
                listOf(first, source.refresh(first), source.token())
            }
            val isOtherRefreshed = server().withTokens { source ->
                source.token()
                source.refresh(Secret("older"))
            }
            val isSameRefreshed = server().withTokens { source -> source.refresh(source.token()) }

            outcome shouldBe listOf(Secret(TOKEN), true, Secret("t2"))
            isOtherRefreshed shouldBe true
            isSameRefreshed shouldBe false
        }
    }

    context("a refresh token") {
        test("is exchanged without a secret by a public client, and replaced when the server rotates it") {
            val tokens = TokenServer(configure = { grant = Grant.RefreshToken(Secret("r1")) }) {
                granted(TOKEN, more = ""","refresh_token":"r2"""")
            }

            val grant = tokens.withTokens { source ->
                source.token()
                source.grant.value
            }

            tokens.forms shouldBe listOf("grant_type=refresh_token&refresh_token=r1&client_id=kapi")
            grant shouldBe Grant.RefreshToken(Secret("r2"))
        }

        test("that expired falls back to the grant of the source") {
            val answers = ArrayDeque(listOf("r2", null, "r3"))
            val tokens = answering {
                val refresh = answers.removeFirst()
                refresh?.let { found -> granted("t-$found", more = ""","refresh_token":"$found","expires_in":0""") }
                    ?: error("invalid_grant")
            }

            val grants = tokens.withTokens { source ->
                source.token()
                source.token()
                val fallen = source.grant.value
                source.token()
                listOf(fallen, source.grant.value)
            }

            grants shouldBe listOf(Grant.ClientCredentials, Grant.RefreshToken(Secret("r3")))
        }
    }

    context("a server that hands out no token") {
        refusals.forEach { (what, content) ->
            test("leaves the source without one when it answers with $what") {
                answering { json(content) }.withTokens { source -> source.token() } shouldBe null
            }
        }

        test("leaves the source without one when it refuses, cannot be reached or takes too long") {
            val refusing = answering { respond("", HttpStatusCode.Unauthorized) }
            val unreachable = answering { throw IOException("refused") }
            val abandoned = answering { throw HttpRequestTimeoutException("https://login.example.com", null) }
            val endless = answering { json("x".repeat(OVERSIZED)) }
            val slow = TokenServer(configure = {
                authentication = ClientAuthentication.SecretBasic(secret)
                timeout = 20.milliseconds
            }) {
                delay(1.minutes)
                granted("late")
            }

            listOf(refusing, unreachable, abandoned, endless, slow).forEach { tokens ->
                tokens.withTokens { source -> source.token() } shouldBe null
            }
        }

        test("cannot refresh what it never had, and refreshes into its first token when it can") {
            val forbidden = answering { respond("", HttpStatusCode.Forbidden) }

            forbidden.withTokens { source -> source.refresh(null) } shouldBe false
            server().withTokens { source -> source.refresh(null) } shouldBe true
        }

        test("is asked again by the next call") {
            val answers = ArrayDeque(listOf(HttpStatusCode.ServiceUnavailable, HttpStatusCode.OK))
            val tokens = answering { json(content = BEARER, status = answers.removeFirst()) }

            tokens.withTokens { source -> listOf(source.token(), source.token()) } shouldBe listOf(null, Secret(TOKEN))
        }
    }

    context("the events of a source") {
        test("say that a token came and why none did, and never name one") {
            coroutineScope {
                val bus = eventBus(this)
                bus.declare(OAuthEvent.TOPIC)
                val answers = ArrayDeque(listOf(HttpStatusCode.OK, HttpStatusCode.BadRequest))
                val tokens = TokenServer(configure = {
                    authentication = ClientAuthentication.SecretBasic(secret)
                    events = bus
                }) {
                    val status = answers.removeFirst()
                    granted(TOKEN).takeIf { status == HttpStatusCode.OK } ?: error("invalid_client")
                }
                val reported = async(start = CoroutineStart.UNDISPATCHED) {
                    val events = bus[OAuthEvent.TOPIC].events().take(2)
                    events.map { envelope -> envelope.payload.toString() }.toList()
                }

                val rendered = tokens.withTokens { source ->
                    source.refresh(source.token())
                    source.toString()
                }

                reported.await() shouldBe listOf(
                    "Granted($CLIENT, client_credentials)",
                    "Refused($CLIENT, client_credentials, status 400 invalid_client)",
                )
                rendered shouldNotContain "s3cr"
                bus.close()
            }
        }
    }
})
