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

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.server.fixture.OK
import com.davils.kapi.runtime.server.fixture.PROBE
import com.davils.kapi.runtime.server.fixture.problemOf
import com.davils.kapi.runtime.server.fixture.serveOne
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.reply.reply
import com.davils.kapi.runtime.server.request.Request
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration.Companion.minutes

private const val TOO_MANY: Int = 429
private const val UNAVAILABLE: Int = 503

class OperationGuardTest : FunSpec({
    test("an operation serves no more requests at once than its limit and refuses the next one at once") {
        val entered = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val handler: suspend (Request) -> Reply = {
            entered.complete(Unit)
            gate.await()
            reply(OK)
        }
        serveOne(handler = handler, settings = { limits { maxConcurrentRequests = 1 } }) {
            coroutineScope {
                val first = async { client.post(PROBE) }
                entered.await()
                val second = client.post(PROBE)

                second.status.value shouldBe UNAVAILABLE
                problemOf(second).code shouldBe ServerCodes.OVERLOADED
                gate.complete(Unit)
                first.await().status.value shouldBe OK
            }
        }
    }

    test("a caller over the rate limit is answered with too many requests and the time to wait") {
        serveOne(handler = { reply(OK) }, settings = { rateLimit(requests = 1, per = 1.minutes) }) {
            client.post(PROBE).status.value shouldBe OK
            val limited = client.post(PROBE)

            limited.status.value shouldBe TOO_MANY
            limited.headers["Retry-After"] shouldBe "60"
            problemOf(limited).code shouldBe ServerCodes.RATE_LIMITED
        }
    }

    test("a server without a rate limit answers every request") {
        serveOne(handler = { reply(OK) }) {
            repeat(times = 3) { client.post(PROBE).status.value shouldBe OK }
        }
    }
})
