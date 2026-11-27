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

package com.davils.kapi.runtime.server.operation

import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.reply.reply
import com.davils.kapi.runtime.server.request.Request
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds

private const val OK: Int = 200

private const val PROBE: String = "probe"

private const val PATH: String = "/probe"

class OperationRateLimitTest : FunSpec({
    test("counts the callers by default and is equal by its parts") {
        val limit = OperationRateLimit(requests = 10, per = 1.minutes)

        limit.key shouldBe RateLimitKey.Caller
        limit shouldBe OperationRateLimit(requests = 10, per = 1.minutes, key = RateLimitKey.Caller)
        limit shouldNotBe OperationRateLimit(requests = 10, per = 1.minutes, key = RateLimitKey.Address)
        limit shouldNotBe OperationRateLimit(requests = 11, per = 1.minutes)
        limit shouldNotBe OperationRateLimit(requests = 10, per = 2.minutes)
        limit.hashCode() shouldBe OperationRateLimit(requests = 10, per = 1.minutes).hashCode()
        limit.toString() shouldBe "OperationRateLimit(10 per 1m, Caller)"
    }

    test("allows the bounds and refuses what lies outside them") {
        OperationRateLimit(requests = OperationRateLimit.MOST_REQUESTS, per = OperationRateLimit.MOST_PERIOD)
        OperationRateLimit(requests = 1, per = 1.nanoseconds)

        shouldThrow<IllegalArgumentException> { OperationRateLimit(requests = 0, per = 1.minutes) }
        shouldThrow<IllegalArgumentException> {
            OperationRateLimit(requests = OperationRateLimit.MOST_REQUESTS + 1, per = 1.minutes)
        }
        shouldThrow<IllegalArgumentException> { OperationRateLimit(requests = 1, per = Duration.ZERO) }
        shouldThrow<IllegalArgumentException> { OperationRateLimit(requests = 1, per = 1.days + 1.nanoseconds) }
    }

    test("is carried by an operation, which has none by default") {
        val limit = OperationRateLimit(requests = 1, per = 1.minutes, key = RateLimitKey.Principal)
        val limited = Operation(name = PROBE, method = "GET", path = PATH, rateLimit = limit) { reply(OK) }
        val free = Operation(name = PROBE, method = "GET", path = PATH) { reply(OK) }

        limited.rateLimit shouldBe limit
        free.rateLimit.shouldBeNull()
    }

    test("keeps the constructor of kapi 1.1 for code compiled against it") {
        val handler: suspend (Request) -> Reply = { reply(OK) }
        val constructor = Operation::class.java.getConstructor(
            String::class.java,
            String::class.java,
            String::class.java,
            List::class.java,
            Boolean::class.javaPrimitiveType,
            Function2::class.java,
        )
        val operation = constructor.newInstance(PROBE, "GET", PATH, emptyList<List<SchemeUse>>(), true, handler)

        operation.isDelivery shouldBe true
        operation.rateLimit.shouldBeNull()
    }
})
