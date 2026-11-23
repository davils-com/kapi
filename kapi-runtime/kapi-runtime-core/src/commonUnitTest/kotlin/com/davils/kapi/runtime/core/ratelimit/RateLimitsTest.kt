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

package com.davils.kapi.runtime.core.ratelimit

import com.davils.kapi.runtime.core.fieldLimits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.io.bytestring.encodeToByteString

private const val BURST: Long = 100L

private const val MINUTE: Long = 60L

private const val DAILY: Long = 1_000L

private const val DAY: Long = 86_400L

private const val REMAINING: Long = 50L

private const val RESET: Long = 30L

private const val DEFAULT: String = "default"

private const val EXAMPLE_POLICIES: String = """"burst";q=100;w=60,"daily";q=1000;w=86400"""

private const val EXAMPLE_LIMIT: String = """"default";r=50;t=30"""

private const val WITH_UNIT: String = """"bytes";q=1000;qu="content-bytes";w=60;pk=:YWJj:"""

private const val UNDEFINED_POLICIES: String = """burst;q=1, "no-quota";w=1, "bad";q=-1, "unit";q=1;qu="apples", """ +
    """"window";q=1;w="x", "key";q=1;pk=1, ("inner");q=1, "good";q=5"""

private const val UNDEFINED_LIMITS: String = """"a";t=1, "b";r=1;t=-1, "c";r=1;pk="x""""

private fun policies(text: String): List<RateLimitPolicy> = RateLimits.parsePolicies(text).shouldNotBeNull()

private fun limits(text: String): List<RateLimit> = RateLimits.parseLimits(text).shouldNotBeNull()

class RateLimitsTest : FunSpec({
    context("RateLimit-Policy") {
        test("reads the examples of the draft") {
            policies(EXAMPLE_POLICIES) shouldBe listOf(
                RateLimitPolicy(name = "burst", quota = BURST, window = MINUTE),
                RateLimitPolicy(name = "daily", quota = DAILY, window = DAY),
            )
        }

        test("reads the unit and the partition key and writes them back") {
            val policy = policies(WITH_UNIT).single()

            policy.unit shouldBe QuotaUnit.ContentBytes
            policy.partitionKey shouldBe "abc".encodeToByteString()
            RateLimits.formatPolicies(listOf(policy)) shouldBe WITH_UNIT
        }

        test("ignores members the draft does not define") {
            policies(UNDEFINED_POLICIES).map(RateLimitPolicy::name) shouldBe listOf("good")
        }

        test("refuses what is no List and what exceeds the limits") {
            RateLimits.parsePolicies(""""a";q=1,""").shouldBeNull()
            RateLimits.parsePolicies(""""a";q=1, "b";q=1""", fieldLimits { maxMembers = 1 }).shouldBeNull()
        }
    }

    context("RateLimit") {
        test("reads the example of the draft and writes it back") {
            val limit = limits(EXAMPLE_LIMIT).single()

            limit shouldBe RateLimit(name = DEFAULT, remaining = REMAINING, reset = RESET)
            RateLimits.formatLimits(listOf(limit, RateLimit(name = "x", remaining = 0L))) shouldBe
                """$EXAMPLE_LIMIT, "x";r=0"""
        }

        test("ignores members without a remaining quota or with a malformed reset") {
            limits(UNDEFINED_LIMITS) shouldBe emptyList()
            limits(""""a";r=1;x=2""").single().remaining shouldBe 1L
            RateLimits.parseLimits("nope nope").shouldBeNull()
        }
    }

    context("the values") {
        test("refuse what a field cannot carry") {
            shouldThrow<IllegalArgumentException> { RateLimitPolicy(name = "a\r\nb", quota = 1L) }
            shouldThrow<IllegalArgumentException> { RateLimitPolicy(name = DEFAULT, quota = -1L) }
            shouldThrow<IllegalArgumentException> { RateLimitPolicy(name = DEFAULT, quota = 1L, window = -1L) }
            shouldThrow<IllegalArgumentException> { RateLimit(name = DEFAULT, remaining = -1L) }
            shouldThrow<IllegalArgumentException> { RateLimit(name = DEFAULT, remaining = 1L, reset = -1L) }
            shouldThrow<IllegalArgumentException> { RateLimit(name = "é", remaining = 1L) }
        }

        test("are equal by their items") {
            val limit = RateLimit(name = DEFAULT, remaining = 1L)
            val policy = RateLimitPolicy(name = DEFAULT, quota = 1L)

            limit shouldBe RateLimit(name = DEFAULT, remaining = 1L)
            limit.hashCode() shouldBe RateLimit(name = DEFAULT, remaining = 1L).hashCode()
            policy.hashCode() shouldBe RateLimitPolicy(name = DEFAULT, quota = 1L).hashCode()
            policy shouldNotBe RateLimitPolicy(name = DEFAULT, quota = 1L, unit = QuotaUnit.ConcurrentRequests)
            policy.toString() shouldBe """"default";q=1"""
            limit.toString() shouldBe """"default";r=1"""
        }
    }
})
