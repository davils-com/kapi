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

package com.davils.kapi.runtime.server.webhook

import com.davils.kore.testing.TestClock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val NOW: Instant = Instant.fromEpochSeconds(1_800_000_000L)

private val clock: TestClock = TestClock(NOW)
private val first: ByteString = "first signature".encodeToByteString()
private val second: ByteString = "second signature".encodeToByteString()

class ReplayGuardTest : FunSpec({
    test("an in-memory guard accepts a signature once and refuses its replay") {
        val guard = ReplayGuard.inMemory(clock = clock)

        guard.isFirstUse(first, NOW + 5.minutes) shouldBe true
        guard.isFirstUse(first, NOW + 5.minutes) shouldBe false
        guard.isFirstUse(second, NOW + 5.minutes) shouldBe true
    }

    test("an in-memory guard needs room for at least one signature") {
        shouldThrow<IllegalArgumentException> { ReplayGuard.inMemory(maxEntries = 0) }
    }
})
