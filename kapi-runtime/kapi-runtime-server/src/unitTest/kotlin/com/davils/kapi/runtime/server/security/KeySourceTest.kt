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

package com.davils.kapi.runtime.server.security

import com.davils.kapi.runtime.server.fixture.crypto
import com.davils.kontinuity.core.environment
import com.davils.krypt.jose.JoseKey
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalKotest::class)
class KeySourceTest : FunSpec({
    coroutineTestScope = true

    test("a cached source asks its source once within the lifetime and again after it") {
        var fetches = 0
        val key = JoseKey.Secret(crypto.keys.generateSecret(), keyId = "k1")
        val source = KeySource {
            fetches += 1
            listOf(key)
        }
        val time = environment { timeSource = testCoroutineScheduler.timeSource }
        val cached = KeySource.cached(source = source, lifetime = 1.minutes, environment = time)

        cached.keys() shouldBe listOf(key)
        cached.keys() shouldBe listOf(key)
        fetches shouldBe 1
        delay(1.minutes + 1.seconds)
        cached.keys() shouldBe listOf(key)
        fetches shouldBe 2
    }

    test("a cached source keeps no failure") {
        var fetches = 0
        val source = KeySource {
            fetches += 1
            check(fetches > 1) { "down" }
            emptyList()
        }
        val cached = KeySource.cached(source)

        shouldThrow<IllegalStateException> { cached.keys() }
        cached.keys() shouldBe emptyList()
        fetches shouldBe 2
    }

    test("a cached source refuses a lifetime that is not positive and finite") {
        shouldThrow<IllegalArgumentException> { KeySource.cached(KeySource.of(emptyList()), lifetime = Duration.ZERO) }
        shouldThrow<IllegalArgumentException> {
            KeySource.cached(KeySource.of(emptyList()), lifetime = Duration.INFINITE)
        }
    }
})
