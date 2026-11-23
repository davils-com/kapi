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

import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.CredentialPlacement
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.SecurityUse
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.Tokens
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.client.pipeline.CallPhase
import com.davils.kapi.runtime.client.pipeline.Interception
import com.davils.kapi.runtime.client.pipeline.Interceptor
import com.davils.kapi.runtime.client.pipeline.RoundTrip
import com.davils.kapi.runtime.core.parameter.ParameterEntry
import com.davils.kapi.runtime.core.response.headersOf
import com.davils.kapi.runtime.core.security.credentials
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.io.bytestring.ByteString

private const val TRACE: String = "X-Trace"

private const val BEARER: String = "bearer"

private const val TEAPOT: Int = 418

private const val CACHE: String = "cache"

private val cached: Exchange = Exchange.Answered(TEAPOT, headersOf(emptyMap()), ByteString())

private val answering: Interceptor = Interceptor { trip -> trip.withExchange(cached) }

private val secured = pets.change { security(listOf(SecurityUse(BEARER, CredentialPlacement.Bearer))) }

class ApiClientPipelineTest : FunSpec({
    context("the phases of a client") {
        test("let an interceptor change the call before it is sent") {
            val exchanges = Exchanges(configure = {
                intercept(CallPhase.Prepare, "trace") { trip ->
                    trip.withCall(trip.call.change { header(name = TRACE, value = "t1") })
                }
            })

            exchanges.execute(pets)

            exchanges.requests.single().headers[TRACE] shouldBe "t1"
        }

        test("let an interceptor answer a call, which is then not sent, in the first phase or right before sending") {
            val prepared = Exchanges(configure = { intercept(CallPhase.Prepare, CACHE, answering) })
            val ready = Exchanges(configure = { intercept(CallPhase.Send, "mock", answering) })

            prepared.execute(secured) shouldBe cached
            ready.execute(pets) shouldBe cached
            prepared.requests.shouldBeEmpty()
            ready.requests.shouldBeEmpty()
        }

        test("let an interceptor change how a call ended") {
            val exchanges = Exchanges(configure = { intercept(CallPhase.Receive, CACHE, answering) })

            exchanges.execute(pets) shouldBe cached
        }

        test("end a call as aborted when an interceptor fails, and name it") {
            val exchanges = Exchanges(configure = {
                intercept(CallPhase.Prepare, "fine") { trip -> trip }
                intercept(CallPhase.Prepare, "broken") { _ -> error("no") }
                intercept(CallPhase.Prepare, "never") { _ -> error("never") }
            })

            exchanges.execute(pets) shouldBe Exchange.Failed(CallFailure.Aborted("broken"))
            exchanges.requests.shouldBeEmpty()
        }

        test("stop before the call is sent when the client has no credentials for it") {
            val received = mutableListOf<RoundTrip>()
            val exchanges = Exchanges(configure = {
                intercept(CallPhase.Receive, "seen") { trip ->
                    received += trip
                    trip
                }
            })

            exchanges.execute(secured) shouldBe Exchange.Failed(CallFailure.CredentialMissing)
            exchanges.requests.shouldBeEmpty()
            received.shouldBeEmpty()
        }

        test("show an interceptor after the credentials the call it is sent as and the secret of each scheme") {
            val trips = mutableListOf<RoundTrip>()
            val exchanges = Exchanges(configure = {
                credentials = credentials { scheme(BEARER, Tokens(listOf("token-1"))) }
                intercept(CallPhase.Authorize, "signature") { trip ->
                    trips += trip
                    trip
                }
            })

            exchanges.execute(secured)
            val trip = trips.single()

            trip.call.headers shouldBe listOf(ParameterEntry(name = "Authorization", value = "Bearer token-1"))
            trip.secrets.keys shouldBe setOf(BEARER)
            trip.exchange shouldBe null
            trip.toString() shouldNotContain "token-1"
            trip.toString() shouldBe "RoundTrip(Call(listPets, GET), not sent, schemes=[bearer])"
        }

        test("are named in the interceptions of the settings") {
            val interception = Interception(CallPhase.Send, "x") { trip -> trip }

            interception.toString() shouldBe "Interception(Send, x)"
            Exchanges(configure = { intercept(CallPhase.Send, "y") { trip -> trip } }).withClient { client ->
                client.settings.interceptions.map(Interception::name)
            } shouldBe listOf("y")
        }
    }
})
