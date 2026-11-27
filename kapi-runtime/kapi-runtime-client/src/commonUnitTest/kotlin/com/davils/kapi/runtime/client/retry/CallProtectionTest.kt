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

package com.davils.kapi.runtime.client.retry

import com.davils.kapi.runtime.client.ApiClient
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.core.response.headersOf
import com.davils.kontinuity.circuitbreaker.circuitBreaker
import com.davils.kontinuity.circuitbreaker.countBased
import com.davils.kontinuity.core.backoff.fixed
import com.davils.kontinuity.policy.Policy
import com.davils.kontinuity.policy.resilience
import com.davils.kontinuity.retry.retry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.io.bytestring.ByteString

private const val WINDOW: Int = 2

private val addPet = apiCall(operation = "addPet", method = "POST") { path("/pets") }

private fun breaking(): Policy = resilience("pets-protection") {
    circuitBreaker(
        circuitBreaker("pets-breaker") {
            results = CallProtection.results
            slidingWindow = countBased(WINDOW)
            minimumNumberOfCalls = WINDOW
        },
    )
}

private fun retrying(): Policy = resilience("pets-retry") {
    retry(
        retry("pets-retry") {
            maxAttempts = 3
            backoff = fixed(1.milliseconds)
            results = CallRetry.results
        },
    )
}

private suspend fun ApiClient.executeAll(times: Int): List<Exchange> = List(times) { execute(addPet) }

class CallProtectionTest : FunSpec({
    context("the failures it records") {
        test("are unreachable servers, timeouts and server errors, never answers of 4xx") {
            CallProtection.isFailure(Attempt(pets, Exchange.Failed(CallFailure.TimedOut))) shouldBe true
            CallProtection.isFailure(Attempt(pets, Exchange.Failed(CallFailure.Rejected("bulkhead")))) shouldBe false
            listOf(500, 503, 599).forEach { status ->
                CallProtection.isFailure(Attempt(pets, answered(status))) shouldBe true
            }
            listOf(200, 404, 429, 600).forEach { status ->
                CallProtection.isFailure(Attempt(pets, answered(status))) shouldBe false
            }
        }

        test("build a circuit breaker that counts attempts by them") {
            CallProtection.circuitBreaker("pets-breaker").config.results shouldBe CallProtection.results
        }
    }

    context("a client") {
        test("protects every call by default and drops the protection when asked") {
            Exchanges().withClient { client -> client.settings.protection.shouldNotBeNull() }
            Exchanges(configure = { isDefaultProtection = false }).withClient { client ->
                client.settings.protection.shouldBeNull()
            }
        }

        test("stops sending a call that is not idempotent once the breaker opened, and sends it only once") {
            val exchanges = Exchanges(configure = {
                protection = breaking()
                resilience = retrying()
            }) { respond(content = "", status = HttpStatusCode.ServiceUnavailable) }

            val answers = exchanges.withClient { client -> client.executeAll(WINDOW + 1) }

            exchanges.requests.size shouldBe WINDOW
            answers.last() shouldBe Exchange.Failed(CallFailure.Rejected("circuit-breaker"))
        }

        test("lets every attempt of a retried call pass the protection") {
            val exchanges = Exchanges(configure = {
                protection = breaking()
                resilience = retrying()
            }) { respond(content = "", status = HttpStatusCode.ServiceUnavailable) }

            val answer = exchanges.withClient { client -> client.execute(pets) }

            exchanges.requests.size shouldBe WINDOW
            answer shouldBe Exchange.Failed(CallFailure.Rejected("circuit-breaker"))
        }
    }
})

private fun answered(status: Int): Exchange =
    Exchange.Answered(status = status, headers = headersOf(emptyMap()), content = ByteString())
