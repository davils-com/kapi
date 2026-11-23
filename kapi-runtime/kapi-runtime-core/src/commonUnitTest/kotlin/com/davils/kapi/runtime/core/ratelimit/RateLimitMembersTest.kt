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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.io.bytestring.encodeToByteString

private const val NAME: String = "a"

private const val KEYED_LIMIT: String = """"a";r=1;pk=:YWJj:"""

private const val UNDEFINED_LIMITS: String = """("a");r=1, a;r=1, "b";r="1", "c";r=1;t="x", "d";r=1;pk=:YWJj:"""

class RateLimitMembersTest : FunSpec({
    context("RateLimit") {
        test("reads and writes a partition key") {
            val limits = RateLimits.parseLimits(KEYED_LIMIT).shouldNotBeNull()
            val limit = limits.single()

            limit.partitionKey shouldBe "abc".encodeToByteString()
            RateLimits.formatLimits(listOf(limit)) shouldBe KEYED_LIMIT
        }

        test("ignores an inner list, a name that is no String and malformed numbers") {
            val limits = RateLimits.parseLimits(UNDEFINED_LIMITS).shouldNotBeNull()

            limits.map(RateLimit::name) shouldBe listOf("d")
        }

        test("differs from what is no limit") {
            RateLimit(name = NAME, remaining = 1L).equals(NAME) shouldBe false
        }
    }

    context("RateLimit-Policy") {
        test("ignores a unit that is no String") {
            RateLimits.parsePolicies(""""a";q=1;qu=1""").shouldNotBeNull() shouldBe emptyList()
        }

        test("differs from what is no policy") {
            RateLimitPolicy(name = NAME, quota = 1L).equals(NAME) shouldBe false
        }
    }
})
