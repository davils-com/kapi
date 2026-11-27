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

import com.davils.kapi.runtime.core.destination.IpAddress
import com.davils.kapi.runtime.server.fixture.requestOf
import com.davils.kapi.runtime.server.operation.RateLimitKey
import com.davils.kapi.runtime.server.request.Peer
import com.davils.kontinuity.ratelimiter.keyedRateLimiter
import com.davils.kore.core.principal.principal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private const val ALICE: String = "alice"

private val first: Peer = Peer(address = IpAddress.parse("10.0.0.1"))

private val second: Peer = Peer(address = IpAddress.parse("10.0.0.2"))

private fun limitOf(key: RateLimitKey, period: Duration = 1.minutes): CallerLimit {
    val limiter = keyedRateLimiter<String>(name = "test") {
        limitForPeriod = 1
        limitRefreshPeriod = period
        timeout = Duration.ZERO
    }
    return CallerLimit(limiter, key, period)
}

class CallerLimitTest : FunSpec({
    test("a caller is its principal once authorized, or else its address") {
        val limit = limitOf(RateLimitKey.Caller)
        val alice = requestOf(peer = first).authorized(principal(ALICE))

        limit.admits(alice) shouldBe true
        limit.admits(requestOf(peer = second).authorized(principal(ALICE))) shouldBe false
        limit.admits(requestOf(peer = first)) shouldBe true
        limit.admits(requestOf(peer = first)) shouldBe false
    }

    test("a principal is counted wherever it comes from, and requests without one share a budget") {
        val limit = limitOf(RateLimitKey.Principal)

        limit.admits(requestOf(peer = first).authorized(principal(ALICE))) shouldBe true
        limit.admits(requestOf(peer = second).authorized(principal(ALICE))) shouldBe false
        limit.admits(requestOf(peer = first)) shouldBe true
        limit.admits(requestOf(peer = second)) shouldBe false
    }

    test("an address is counted whoever sends from it, and requests without one share a budget") {
        val limit = limitOf(RateLimitKey.Address)

        limit.admits(requestOf(peer = first).authorized(principal(ALICE))) shouldBe true
        limit.admits(requestOf(peer = first).authorized(principal("bob"))) shouldBe false
        limit.admits(requestOf(peer = second)) shouldBe true
        limit.admits(requestOf()) shouldBe true
        limit.admits(requestOf()) shouldBe false
    }

    test("asks to retry after the period in whole seconds, at least one") {
        limitOf(RateLimitKey.Caller).retryAfter shouldBe "60"
        limitOf(RateLimitKey.Caller, period = 10.milliseconds).retryAfter shouldBe "1"
    }
})
