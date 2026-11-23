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

package com.davils.kapi.runtime.client.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val LIMIT: Int = 8

private const val LINE: String = "\n"

private fun Lines.accept(text: String): List<String> = accept(text.encodeToByteArray())

class LinesTest : FunSpec({
    context("the lines of a stream") {
        test("end at a line feed, with or without a carriage return before it") {
            val lines = Lines(LIMIT)

            lines.accept(listOf("a", "bc\r", "", "d").joinToString(LINE)) shouldBe listOf("a", "bc", "")
            lines.rest() shouldBe listOf("d")
        }

        test("are put together across chunks, even inside a character, and only from what was read") {
            val lines = Lines(LIMIT)
            val bytes = "ä\n".encodeToByteArray()

            lines.accept(bytes, 1).shouldBeEmpty()
            lines.accept(bytes.copyOfRange(fromIndex = 1, toIndex = bytes.size)) shouldBe listOf("ä")
            lines.accept("a\nb\n".encodeToByteArray(), 2) shouldBe listOf("a")
            lines.rest().shouldBeEmpty()
        }
    }

    context("a line longer than the limit") {
        test("ends the reading with what was complete before it and nothing after") {
            val lines = Lines(LIMIT)

            lines.accept(listOf("ok", "123456789", "later", "").joinToString(LINE)) shouldBe listOf("ok")
            lines.isOverLimit shouldBe true
            lines.accept("more\n").shouldBeEmpty()
            lines.rest().shouldBeEmpty()
        }

        test("is none when it has exactly as many bytes as the limit") {
            val lines = Lines(LIMIT)

            lines.accept("12345678\n") shouldBe listOf("12345678")
            lines.isOverLimit shouldBe false
        }
    }
})
