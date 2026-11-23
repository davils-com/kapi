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

package com.davils.kapi.runtime.core.digest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val SHA_256: String = "sha-256"

private const val TOO_HEAVY: Int = 11

class DigestMembersTest : FunSpec({
    context("digests") {
        test("ignore members that are no byte sequence") {
            DigestFields.parseDigests("sha-256=(:YQ==:), sha-512=1").shouldNotBeNull() shouldBe emptyList()
            DigestFields.parseDigests("sha-256=:YQ==:,").shouldBeNull()
        }
    }

    context("preferences") {
        test("ignore members that are no integer or weigh more than 10") {
            DigestFields.parsePreferences("sha-256=(1), sha-512=?1, md5=11, sha=-1").shouldNotBeNull() shouldBe
                emptyList()
            DigestFields.parsePreferences("sha-256=1,").shouldBeNull()
        }

        test("refuse to write a weight outside 0 to 10") {
            shouldThrow<IllegalArgumentException> { DigestFields.formatPreferences(listOf(SHA_256 to -1)) }
            shouldThrow<IllegalArgumentException> { DigestFields.formatPreferences(listOf(SHA_256 to TOO_HEAVY)) }
        }
    }

    context("choosing") {
        test("chooses nothing without supported algorithms or without a preference for them") {
            DigestFields.choose(listOf(SHA_256 to 1), emptyList()).shouldBeNull()
            DigestFields.choose(listOf("sha-512" to 1), listOf(DigestAlgorithm.Sha256)).shouldBeNull()
        }
    }
})
