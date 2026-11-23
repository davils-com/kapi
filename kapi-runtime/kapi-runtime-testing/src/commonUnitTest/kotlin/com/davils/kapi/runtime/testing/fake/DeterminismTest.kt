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

package com.davils.kapi.runtime.testing.fake

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val THREE: Int = 3

private const val FIVE: Long = 5L

private const val MINUS_TWO: Byte = -2

class DeterminismTest : FunSpec({
    context("a manual clock") {
        test("moves its wall clock and its marks together, only when advanced") {
            val clock = ManualClock()
            val mark = clock.markNow()

            clock.now() shouldBe ManualClock.DEFAULT_START
            mark.elapsedNow() shouldBe Duration.ZERO
            clock.advance(5.minutes)

            clock.now() shouldBe Instant.parse("2026-01-01T00:05:00Z")
            mark.elapsedNow() shouldBe 5.minutes
            (clock.markNow() > mark) shouldBe true
            clock.toString() shouldBe "ManualClock(2026-01-01T00:05:00Z)"
        }

        test("refuses to move backwards or forever") {
            val clock = ManualClock(Instant.fromEpochSeconds(0L))

            shouldThrow<IllegalArgumentException> { clock.advance((-1).seconds) }
            shouldThrow<IllegalArgumentException> { clock.advance(Duration.INFINITE) }
            clock.now() shouldBe Instant.fromEpochSeconds(0L)
        }
    }

    context("counting randomness") {
        test("draws the next values of its counter") {
            val random = CountingRandom(seed = 254)

            random.bytes(THREE).toList() shouldBe listOf(MINUS_TWO, -1, 0)
            random.bytes(2).toList() shouldBe listOf<Byte>(1, 2)
            random.drawn shouldBe FIVE
            random.toString() shouldBe "CountingRandom(seed=254, drawn=5)"
        }
    }

    context("a deterministic crypto object") {
        test("uses the clock and the randomness it is given") {
            val clock = ManualClock()
            val random = CountingRandom()
            val crypto = deterministicCrypto(clock = clock, random = random)

            crypto.environment.clock shouldBe clock
            crypto.environment.random
                .bytes(2)
                .toList() shouldBe listOf<Byte>(0, 1)
            val fresh = deterministicCrypto().environment
            fresh.random.bytes(1).toList() shouldBe listOf<Byte>(0)
        }
    }
})
