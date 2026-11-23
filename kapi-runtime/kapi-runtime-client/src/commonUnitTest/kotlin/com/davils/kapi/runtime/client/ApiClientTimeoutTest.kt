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
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.Timeout
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.ok
import com.davils.kapi.runtime.client.fixture.pets
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.plugins.HttpTimeoutCapability
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val CONNECT: Long = 2_000

private const val SOCKET: Long = 3_000

private val timedOut: Exchange = Exchange.Failed(CallFailure.TimedOut)

class ApiClientTimeoutTest : FunSpec({
    context("a call the server does not answer in time") {
        test("is a timeout after the time the call allows") {
            val exchanges = Exchanges {
                delay(1.minutes)
                ok("[]")
            }

            exchanges.execute(pets.change { timeout(request = 20.milliseconds) }) shouldBe timedOut
        }

        test("is a timeout after the time the client allows when the call names none") {
            val exchanges = Exchanges(configure = { timeout = Timeout(request = 20.milliseconds) }) {
                delay(1.minutes)
                ok("[]")
            }

            exchanges.execute(pets) shouldBe timedOut
        }

        test("is answered when the call allows more time than the client") {
            val exchanges = Exchanges(configure = { timeout = Timeout(request = 20.milliseconds) }) {
                delay(100.milliseconds)
                ok("[]")
            }

            exchanges.execute(pets.change { timeout(request = 1.minutes) }).shouldBeInstanceOf<Exchange.Answered>()
        }
    }

    context("the time a connection and a read may take") {
        test("is handed to the engine from the call before the client") {
            val exchanges = Exchanges(configure = { timeout = Timeout(connect = 9.seconds, socket = 3.seconds) })

            exchanges.execute(pets.change { timeout(connect = 2.seconds) })
            val limits = exchanges.requests.single().getCapabilityOrNull(HttpTimeoutCapability)

            limits?.connectTimeoutMillis shouldBe CONNECT
            limits?.socketTimeoutMillis shouldBe SOCKET
            limits?.requestTimeoutMillis shouldBe null
        }

        test("is left to the engine when neither names one") {
            val exchanges = Exchanges()

            exchanges.execute(pets.change { timeout(request = 1.minutes) })

            exchanges.requests.single().getCapabilityOrNull(HttpTimeoutCapability) shouldBe null
        }
    }
})
