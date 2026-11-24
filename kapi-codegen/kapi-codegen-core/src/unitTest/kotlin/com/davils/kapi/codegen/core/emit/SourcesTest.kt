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

package com.davils.kapi.codegen.core.emit

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val HIDDEN: Char = '\u200B'

private const val ESCAPED: String = "\\u200B"

private val raw: String = "\"".repeat(3)

class SourcesTest : FunSpec({
    context("visible text") {
        test("escapes a hidden character in code, a string, a character and a comment") {
            val source = """val a = "x$HIDDEN\"$HIDDEN" + '\'' + '$HIDDEN' /** $HIDDEN */"""

            Sources.visible(source) shouldBe """val a = "x$ESCAPED\"$ESCAPED" + '\'' + '$ESCAPED' /** $ESCAPED */"""
        }

        test("writes a hidden character in a raw string as a template, which keeps its value") {
            val source = """val a = ${raw}x$HIDDEN$raw.trimMargin() + "$HIDDEN""""

            Sources.visible(source) shouldBe """val a = ${raw}x${'$'}{'$ESCAPED'}$raw.trimMargin() + "$ESCAPED""""
        }

        test("leaves text without hidden characters as it is") {
            val clean = """val a = ${raw}x$raw"""

            Sources.visible(clean) shouldBe clean
        }
    }
})
