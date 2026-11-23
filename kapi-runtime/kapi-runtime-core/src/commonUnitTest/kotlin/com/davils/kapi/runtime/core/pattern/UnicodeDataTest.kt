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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.core.pattern

import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

private const val GRINNING_FACE: Int = 0x1F600
private const val UNASSIGNED_GREEK: Int = 0x0378
private const val PRIVATE_USE: Int = 0xE000
private const val SCRIPT_COUNT: Int = 171

class UnicodeDataTest : FunSpec({
    context("the general categories") {
        test("cover every code point exactly once") {
            val all = UnicodeData.categorySet(UnicodeData.CATEGORY_CODES.toSet())

            all.complement().isEmpty shouldBe true
            (LAST_CODE_POINT in all) shouldBe true
        }

        test("name the category of a code point") {
            UnicodeData.categoryOf('A'.code) shouldBe "Lu"
            UnicodeData.categoryOf(UNASSIGNED_GREEK) shouldBe "Cn"
            UnicodeData.categoryOf(GRINNING_FACE) shouldBe "So"
            UnicodeData.categoryOf(PRIVATE_USE) shouldBe "Co"
            UnicodeData.categoryOf(LAST_CODE_POINT) shouldBe "Cn"
        }
    }

    context("the scripts") {
        val names = SCRIPT_NAMES.split(' ')

        test("list every script of Unicode 16.0") {
            names shouldHaveSize SCRIPT_COUNT
        }

        test("name the script of a code point") {
            UnicodeData.scriptOf('A'.code) shouldBe "Latin"
            UnicodeData.scriptOf('α'.code) shouldBe "Greek"
            UnicodeData.scriptOf('中'.code) shouldBe "Han"
            UnicodeData.scriptOf('1'.code) shouldBe "Common"
            UnicodeData.scriptOf(UNASSIGNED_GREEK) shouldBe "Unknown"
        }

        test("compile by every long name and code") {
            val refused = names.flatMap { entry -> entry.split(',') }.filter { name ->
                Pattern.compile("\\p{Script=$name}") !is PatternCompilation.Compiled ||
                    Pattern.compile("\\P{sc=$name}") !is PatternCompilation.Compiled
            }

            refused shouldBe emptyList()
        }
    }

    context("the bidirectional classes") {
        test("name the class of a code point") {
            UnicodeData.bidiClassOf('A'.code) shouldBe "L"
            UnicodeData.bidiClassOf('1'.code) shouldBe "EN"
            UnicodeData.bidiClassOf('א'.code) shouldBe "R"
            UnicodeData.bidiClassOf('ا'.code) shouldBe "AL"
            UnicodeData.bidiClassOf('٠'.code) shouldBe "AN"
            UnicodeData.bidiClassOf('́'.code) shouldBe "NSM"
        }
    }

    context("the code points of a text") {
        test("are read and written as one or two code units") {
            val text = "a😀"

            codePointAt(text, 1) shouldBe GRINNING_FACE
            charCount(codePointAt(text, 0)) shouldBe 1
            charCount(GRINNING_FACE) shouldBe 2
            val written = StringBuilder()
            written.appendPoint(GRINNING_FACE).appendPoint('a'.code)

            written.toString() shouldBe "😀a"
        }
    }

    context("a set of code points") {
        test("merges the ranges it is built from") {
            val ranges = CodePointSet.Ranges()
            ranges.add(first = 'a'.code, last = 'c'.code)
            ranges.add(first = 'b'.code, last = 'f'.code)
            ranges.add(first = GRINNING_FACE, last = GRINNING_FACE)

            val set = ranges.toSet()

            ('e'.code in set) shouldBe true
            ('g'.code in set) shouldBe false
            (GRINNING_FACE in set) shouldBe true
        }
    }

    context("the binary properties") {
        test("compile by every name") {
            val refused = BinaryProperty.entries.flatMap { it.names }.filter { name ->
                Pattern.compile("\\p{$name}") !is PatternCompilation.Compiled
            }

            refused shouldBe emptyList()
        }
    }
})
