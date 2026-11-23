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
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.client.pipeline.CallPhase
import com.davils.kapi.runtime.core.destination.DestinationRefusal
import com.davils.kapi.runtime.core.destination.HostResolution
import com.davils.kapi.runtime.core.destination.IpAddress
import com.davils.kapi.runtime.core.destination.destinationPolicy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val HOOK: String = "https://hooks.example.com/in"

private const val PRIVATE: String = "https://10.0.0.1/in"

private val refused: Map<String, DestinationRefusal> = mapOf(
    "http://hooks.example.com/in" to DestinationRefusal.Scheme,
    "https://localhost/in" to DestinationRefusal.NotPublic,
    PRIVATE to DestinationRefusal.NotPublic,
    "https://[::1]/in" to DestinationRefusal.NotPublic,
    "https://intranet/in" to DestinationRefusal.NotPublic,
    "https://user@hooks.example.com/in" to DestinationRefusal.UserInfo,
    "/in" to DestinationRefusal.Url,
    "hooks.example.com/in" to DestinationRefusal.Url,
)

private fun delivery(destination: String): Call = apiCall(operation = "petAdopted", method = "POST") {
    destination(destination)
}

class ApiClientDestinationTest : FunSpec({
    context("a call to a destination") {
        test("is sent to exactly that URL, whatever the base of the client") {
            val exchanges = Exchanges()

            exchanges.execute(delivery(HOOK)).shouldBeInstanceOf<Exchange.Answered>()
            exchanges.urls.single() shouldBe HOOK
        }

        test("keeps the query of the URL and adds the query of the call") {
            val exchanges = Exchanges()
            exchanges.execute(delivery("$HOOK?token=a").change { query(name = "pet", value = "4") })

            exchanges.urls.single() shouldBe "$HOOK?token=a&pet=4"
        }

        refused.forEach { (destination, refusal) ->
            test("is not sent when the policy refuses it: '$destination'") {
                val exchanges = Exchanges()

                exchanges.execute(delivery(destination)) shouldBe
                    Exchange.Failed(CallFailure.DestinationRefused(refusal))
                exchanges.requests.shouldBeEmpty()
            }
        }

        test("is sent into a private network once the policy of the client allows that host") {
            val local = destinationPolicy {
                schemes = setOf("http")
                hosts = setOf("localhost")
            }
            val exchanges = Exchanges(configure = { destinations = local })

            exchanges.execute(delivery("http://localhost:8080/in")).shouldBeInstanceOf<Exchange.Answered>()
            exchanges.urls.single() shouldBe "http://localhost:8080/in"
        }

        test("is refused when the resolver points a public name into a private network") {
            val private = HostResolution.Resolved(listOf(IpAddress.parse("10.0.0.7")))
            val resolving = destinationPolicy { resolver = { private } }
            val exchanges = Exchanges(configure = { destinations = resolving })

            exchanges.execute(delivery(HOOK)) shouldBe
                Exchange.Failed(CallFailure.DestinationRefused(DestinationRefusal.ResolvesToPrivate))
        }

        test("is checked as it is sent, also after an interceptor changed where it goes") {
            val exchanges = Exchanges(configure = {
                intercept(CallPhase.Authorize, "detour") { trip ->
                    trip.withCall(trip.call.change { destination(PRIVATE) })
                }
            })

            exchanges.execute(delivery(HOOK)) shouldBe
                Exchange.Failed(CallFailure.DestinationRefused(DestinationRefusal.NotPublic))
            exchanges.requests.shouldBeEmpty()
        }
    }

    context("a call to the base of the client") {
        test("is checked like a destination") {
            val exchanges = Exchanges(configure = { baseUrl = "https://10.1.2.3/v1" })

            exchanges.execute(pets) shouldBe
                Exchange.Failed(CallFailure.DestinationRefused(DestinationRefusal.NotPublic))
        }

        test("fails without a base") {
            val exchanges = Exchanges(configure = { baseUrl = "" })

            exchanges.execute(pets) shouldBe Exchange.Failed(CallFailure.BaseUrlMissing)
            exchanges.execute(delivery(HOOK)).shouldBeInstanceOf<Exchange.Answered>()
        }
    }
})
