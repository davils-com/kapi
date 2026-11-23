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

import com.davils.kapi.runtime.client.ClientEvent
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.ok
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.konfetti.core.eventBus
import com.davils.kontinuity.bulkhead.bulkhead
import com.davils.kontinuity.core.backoff.fixed
import com.davils.kontinuity.policy.Policy
import com.davils.kontinuity.policy.resilience
import com.davils.kontinuity.retry.retry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val ATTEMPTS: Int = 3

private const val UNAVAILABLE: Int = 503

private const val PETS: String = "/pets"

private fun retrying(isFailingAfterMaxAttempts: Boolean = false): Policy = resilience("pets") {
    retry(
        retry("pets-retry") {
            maxAttempts = ATTEMPTS
            backoff = fixed(1.milliseconds)
            results = CallRetry.results
            interval = CallRetry.interval(maximum = 1.milliseconds)
            this.isFailingAfterMaxAttempts = isFailingAfterMaxAttempts
        },
    )
}

class ApiClientResilienceTest : FunSpec({
    context("a retryable call under a policy that retries") {
        test("is sent again until the server answers, waiting what the server asks for") {
            val statuses = listOf(HttpStatusCode.ServiceUnavailable, HttpStatusCode.BadGateway, HttpStatusCode.OK)
            val answers = ArrayDeque(statuses)
            val exchanges = Exchanges(configure = { resilience = retrying() }) {
                val delay = headersOf(name = HttpHeaders.RetryAfter, value = "1")
                respond(content = "", status = answers.removeFirst(), headers = delay)
            }

            exchanges.execute(pets).shouldBeInstanceOf<Exchange.Answered>().status shouldBe HttpStatusCode.OK.value
            exchanges.requests.size shouldBe ATTEMPTS
        }

        listOf(false, true).forEach { isFailing ->
            test("ends with the last answer once the policy gives up, failing after the attempts: $isFailing") {
                val exchanges = Exchanges(configure = { resilience = retrying(isFailing) }) {
                    respond(content = "", status = HttpStatusCode.ServiceUnavailable)
                }

                exchanges.execute(pets).shouldBeInstanceOf<Exchange.Answered>().status shouldBe UNAVAILABLE
                exchanges.requests.size shouldBe ATTEMPTS
            }
        }

        test("keeps its idempotency key on every attempt and reports each retry") {
            val keyed = apiCall(operation = "addPet", method = "POST") {
                path(PETS)
                idempotencyKey()
            }
            coroutineScope {
                val bus = eventBus(this)
                bus.declare(ClientEvent.TOPIC)
                val events = Exchanges(configure = {
                    resilience = retrying()
                    events = bus
                }) { respond(content = "", status = HttpStatusCode.ServiceUnavailable) }
                val reported = async(start = CoroutineStart.UNDISPATCHED) {
                    val events = bus[ClientEvent.TOPIC].events().take(2)
                    events.map { envelope -> envelope.payload }.toList()
                }
                events.withClient { client ->
                    reported.isActive shouldBe true
                    client.execute(keyed)
                }
                val keys = events.requests.map { request -> request.headers["Idempotency-Key"] }

                keys.toSet().size shouldBe 1
                val retries = reported.await()

                retries.map { event -> (event as ClientEvent.Retrying).attempt } shouldBe listOf(2, ATTEMPTS)
                retries.first().toString() shouldBe "Retrying(api, addPet, attempt=2)"
                bus.close()
            }
        }
    }

    context("a call that is not retryable") {
        test("is sent once, whatever the policy would retry") {
            val post = apiCall(operation = "addPet", method = "POST") { path(PETS) }
            val upload = apiCall(operation = "build", method = "PUT") {
                path(PETS)
                upload(MediaType.OctetStream, flowOf("x".encodeToByteString()))
            }
            val exchanges = Exchanges(configure = { resilience = retrying() }) {
                respond(content = "", status = HttpStatusCode.ServiceUnavailable)
            }

            exchanges.execute(post)
            exchanges.execute(upload)

            exchanges.requests.size shouldBe 2
        }
    }

    context("a call the policy does not admit") {
        test("is not sent and names the kind that rejected it") {
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val policy = resilience("pets") {
                bulkhead(
                    bulkhead("pets-bulkhead") {
                        maxConcurrentCalls = 1
                        maxWaitingCalls = 0
                        maxWaitDuration = Duration.ZERO
                    },
                )
            }
            val exchanges = Exchanges(configure = { resilience = policy }) {
                entered.complete(Unit)
                release.await()
                ok("[]")
            }

            val rejected = exchanges.withClient { client ->
                coroutineScope {
                    val first = async { client.execute(pets) }
                    entered.await()
                    val second = client.execute(pets)
                    release.complete(Unit)
                    first.await()
                    second
                }
            }

            rejected shouldBe Exchange.Failed(CallFailure.Rejected("bulkhead"))
            exchanges.requests.size shouldBe 1
        }
    }
})
