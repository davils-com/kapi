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

package com.davils.kapi.spec.core.value

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val NAME: String = "x-kapi-name"

private const val AUDITED: String = "x-audited"

class ExtensionNameTest : FunSpec({
    context("an extension name") {
        test("starts with x- and keeps its text") {
            ExtensionName(NAME).value shouldBe NAME
            ExtensionName(NAME).toString() shouldBe NAME
        }

        test("refuses what does not start with x- or names nothing after it") {
            listOf("", "x", "x-", "X-kide", "kide-name").forEach { text ->
                val failure = shouldThrow<IllegalArgumentException> { ExtensionName(text) }

                failure.message shouldContain "is no extension name"
            }
        }
    }

    context("parseOrNull") {
        test("reads a name and returns null for what is none") {
            ExtensionName.parseOrNull(AUDITED) shouldBe ExtensionName(AUDITED)
            ExtensionName.parseOrNull("audited") shouldBe null
        }
    }

    context("isExtension") {
        test("tells an extension from a field") {
            ExtensionName.isExtension(AUDITED) shouldBe true
            ExtensionName.isExtension("description") shouldBe false
        }
    }
})
