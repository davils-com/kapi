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

import com.davils.kapi.runtime.client.call.CredentialPlacement
import com.davils.kapi.runtime.client.call.SecurityUse
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.Spans
import com.davils.kapi.runtime.client.fixture.Tokens
import com.davils.kapi.runtime.client.fixture.collecting
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.client.fixture.unauthorized
import com.davils.kapi.runtime.client.redirect.redirectPolicy
import com.davils.kapi.runtime.core.security.credentials
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.meterRegistry
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.HistogramPoint
import com.davils.kollect.core.snapshot.SumPoint
import com.davils.kollect.core.trace.SpanKind
import com.davils.kollect.core.trace.SpanOutcome
import com.davils.konfetti.core.eventBus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.coroutineScope
import kotlinx.io.IOException

private const val API: String = "petstore"

private const val OPERATION: String = "listPets"

private const val CALLS: String = "kapi.client.calls"

private const val REPORTED: Int = 3

private const val SUCCESS: String = "success"

private val statuses: Map<HttpStatusCode, String> = mapOf(
    HttpStatusCode.OK to SUCCESS,
    HttpStatusCode.NotModified to SUCCESS,
    HttpStatusCode.NotFound to "client_error",
    HttpStatusCode.BadGateway to "server_error",
)

private fun MeterRegistry.count(outcome: String): Double {
    val family = snapshot().family(MetricName.of(CALLS))
    val points = family?.points.orEmpty().filterIsInstance<SumPoint>()
    return points.single { point -> point.labels.values == listOf(API, OPERATION, outcome) }.value
}

class ApiClientTelemetryTest : FunSpec({
    context("the meters of a client") {
        statuses.forEach { (status, outcome) ->
            test("count a call answered with ${status.value} as $outcome") {
                val registry = meterRegistry()
                val exchanges = Exchanges(configure = {
                    name = API
                    meters = registry
                }) { respond(content = "", status = status) }

                exchanges.withClient { client ->
                    client.execute(pets)
                    client.execute(pets)
                }

                registry.count(outcome) shouldBe 2.0
            }
        }

        test("count a call that got no answer as failed and time every call of an operation") {
            val registry = meterRegistry()
            val exchanges = Exchanges(configure = {
                name = API
                meters = registry
            }) { throw IOException("refused") }

            exchanges.execute(pets)
            val timer = registry.snapshot().family(MetricName.of("kapi.client.call.duration"))

            registry.count("failed") shouldBe 1.0
            val points = timer?.points.orEmpty()
            points.filterIsInstance<HistogramPoint>().single().count shouldBe 1
        }
    }

    context("the trace of a client") {
        test("has one client span per call that names the operation, the status and how it ended") {
            val spans = Spans()
            val exchanges = Exchanges(configure = {
                name = API
                tracer = spans
            })

            exchanges.execute(pets)
            val span = spans.recorded.single()

            span.operation shouldBe "kapi.client.call"
            span.kind shouldBe SpanKind.Client
            span.attributes shouldBe mapOf(
                "kapi.api" to API,
                "kapi.operation" to OPERATION,
                "kapi.outcome" to SUCCESS,
                "http.response.status_code" to "200",
            )
            span.outcome shouldBe SpanOutcome.Ok
        }

        test("ends the span of a call that failed or met a server error as an error") {
            val spans = Spans()
            Exchanges(configure = { tracer = spans }) { throw IOException("refused") }.execute(pets)
            Exchanges(configure = { tracer = spans }) { respond(content = "", status = HttpStatusCode.BadGateway) }
                .execute(pets)

            spans.recorded.map { span -> span.outcome } shouldBe listOf(SpanOutcome.Error, SpanOutcome.Error)
        }
    }

    context("the events of a client") {
        test("report redirects and refreshed credentials, naming no URL and no token") {
            coroutineScope {
                val bus = eventBus(this)
                bus.declare(ClientEvent.TOPIC)
                val secured = pets.change { security(listOf(SecurityUse("bearer", CredentialPlacement.Bearer))) }
                val exchanges = Exchanges(configure = {
                    events = bus
                    redirects = redirectPolicy()
                    credentials = credentials { scheme("bearer", Tokens(listOf("old", "new"))) }
                }) { request ->
                    val isMoved = request.url.encodedPath.endsWith("/moved")
                    val location = headersOf(name = HttpHeaders.Location, value = "/v1/moved")
                    val moved = respond(content = "", status = HttpStatusCode.Found, headers = location)
                    unauthorized().takeIf { isMoved } ?: moved
                }
                val reported = collecting(this, bus[ClientEvent.TOPIC], REPORTED)

                exchanges.withClient { client -> client.execute(secured) }

                reported.await().map(ClientEvent::toString) shouldBe listOf(
                    "Redirected(api, listPets, status=302, hop=1, isCrossOrigin=false)",
                    "Refreshed(api, listPets, bearer, isRefreshed=true)",
                    "Redirected(api, listPets, status=302, hop=1, isCrossOrigin=false)",
                )
                bus.close()
            }
        }
    }
})
