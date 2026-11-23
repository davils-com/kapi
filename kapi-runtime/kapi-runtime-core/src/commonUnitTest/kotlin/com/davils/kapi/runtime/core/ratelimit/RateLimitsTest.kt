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

class RateLimitsTest : FunSpec({
    context("RateLimit-Policy") {
        test("reads the examples of the draft") {
            val policies = RateLimits.parsePolicies("\"burst\";q=100;w=60,\"daily\";q=1000;w=86400").shouldNotBeNull()

            policies shouldBe listOf(RateLimitPolicy("burst", BURST, window = MINUTE), RateLimitPolicy("daily", DAILY, window = DAY))
        }

        test("reads the unit and the partition key and writes them back") {
            val text = "\"bytes\";q=1000;qu=\"content-bytes\";w=60;pk=:YWJj:"
            val policy = RateLimits.parsePolicies(text).shouldNotBeNull().single()

            policy.unit shouldBe QuotaUnit.ContentBytes
            policy.partitionKey shouldBe "abc".encodeToByteString()
            RateLimits.formatPolicies(listOf(policy)) shouldBe text
        }

        test("ignores members the draft does not define") {
            val text = "burst;q=1, \"no-quota\";w=1, \"bad\";q=-1, \"unit\";q=1;qu=\"apples\", \"window\";q=1;w=\"x\", " +
                "\"key\";q=1;pk=1, (\"inner\");q=1, \"good\";q=5"

            RateLimits.parsePolicies(text).shouldNotBeNull().map(RateLimitPolicy::name) shouldBe listOf("good")
        }

        test("refuses what is no List and what exceeds the limits") {
            RateLimits.parsePolicies("\"a\";q=1,").shouldBeNull()
            RateLimits.parsePolicies("\"a\";q=1, \"b\";q=1", fieldLimits { maxMembers = 1 }).shouldBeNull()
        }
    }

    context("RateLimit") {
        test("reads the example of the draft and writes it back") {
            val limit = RateLimits.parseLimits("\"default\";r=50;t=30").shouldNotBeNull().single()

            limit shouldBe RateLimit(DEFAULT, REMAINING, RESET)
            RateLimits.formatLimits(listOf(limit, RateLimit("x", 0L))) shouldBe "\"default\";r=50;t=30, \"x\";r=0"
        }

        test("ignores members without a remaining quota or with a malformed reset") {
            RateLimits.parseLimits("\"a\";t=1, \"b\";r=1;t=-1, \"c\";r=1;pk=\"x\"").shouldNotBeNull() shouldBe emptyList()
            RateLimits.parseLimits("\"a\";r=1;x=2").shouldNotBeNull().single().remaining shouldBe 1L
            RateLimits.parseLimits("nope nope").shouldBeNull()
        }
    }

    context("the values") {
        test("refuse what a field cannot carry") {
            shouldThrow<IllegalArgumentException> { RateLimitPolicy("a\r\nb", 1L) }
            shouldThrow<IllegalArgumentException> { RateLimitPolicy(DEFAULT, -1L) }
            shouldThrow<IllegalArgumentException> { RateLimitPolicy(DEFAULT, 1L, window = -1L) }
            shouldThrow<IllegalArgumentException> { RateLimit(DEFAULT, -1L) }
            shouldThrow<IllegalArgumentException> { RateLimit(DEFAULT, 1L, reset = -1L) }
            shouldThrow<IllegalArgumentException> { RateLimit("é", 1L) }
        }

        test("are equal by their items") {
            RateLimit(DEFAULT, 1L) shouldBe RateLimit(DEFAULT, 1L)
            RateLimit(DEFAULT, 1L).hashCode() shouldBe RateLimit(DEFAULT, 1L).hashCode()
            RateLimitPolicy(DEFAULT, 1L).hashCode() shouldBe RateLimitPolicy(DEFAULT, 1L).hashCode()
            RateLimitPolicy(DEFAULT, 1L) shouldNotBe RateLimitPolicy(DEFAULT, 1L, QuotaUnit.ConcurrentRequests)
            RateLimitPolicy(DEFAULT, 1L).toString() shouldBe "\"default\";q=1"
            RateLimit(DEFAULT, 1L).toString() shouldBe "\"default\";r=1"
        }
    }
})
