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

package com.davils.kapi.spec.openapi.validation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val LONG: Int = 100_000

private val oracle: Regex = Regex("""[A-Za-z0-9!#$&^_.+*-]+/[A-Za-z0-9!#$&^_.+*-]+(\s*;.*)?""")

private val texts: List<String> = listOf(
    "",
    "/",
    "text",
    "text/",
    "/plain",
    "text/plain",
    "TEXT/PLAIN",
    "application/vnd.api+json",
    "*/*",
    "!#$&^_.+*-/!#$&^_.+*-",
    "text/plain;",
    "text/plain; charset=utf-8",
    "text/plain \t ; charset=utf-8",
    "text/plain\n;a",
    "text/plain;a\nb",
    "text/plain;a\rb",
    "text/plain;a b",
    "text/plain ",
    "text/plain charset",
    "text/plain,text/html",
    "text//plain",
    "text/plain/x",
    "te xt/plain",
    "text/pl@in",
    "téxt/plain",
    "text/plain;é",
    "text/plain\u000B;a",
    "text/plain\u000C;a",
    "text/plain ;a",
)

class MediaTypeSyntaxTest : FunSpec({
    context("a media type") {
        test("is what the regular expression of a media type accepts") {
            texts.forEach { text -> MediaTypeSyntax.isValid(text) shouldBe oracle.matches(text) }
        }

        test("ends its parameters before a line terminator") {
            MediaTypeSyntax.isValid("text/plain;a\u0085b") shouldBe false
            MediaTypeSyntax.isValid("text/plain;a b") shouldBe false
        }

        test("is scanned once, however long it is") {
            val token = "a".repeat(LONG)

            MediaTypeSyntax.isValid("$token/$token") shouldBe true
            MediaTypeSyntax.isValid("$token/$token ") shouldBe false
        }
    }
})
