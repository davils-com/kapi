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

package com.davils.kapi.runtime.core.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val DELETE: Char = '\u007F'

private const val TAB: Char = '\t'

private const val BEYOND_LATIN: Char = 'ā'

private fun inQuotes(text: String): String = "\"$text\""

private fun quoted(text: String, isUnicode: Boolean = false): String? = FieldScanner(text).tokenOrQuoted(isUnicode)

class FieldGrammarTest : FunSpec({
    context("the characters of a field") {
        test("are controls below the space except the tab, and the delete") {
            FieldGrammar.isControl(TAB) shouldBe false
            FieldGrammar.isControl('\u0001') shouldBe true
            FieldGrammar.isControl(DELETE) shouldBe true
            FieldGrammar.isControl('a') shouldBe false
        }

        test("are obsolete text between U+0080 and U+00FF") {
            FieldGrammar.isObsolete('é') shouldBe true
            FieldGrammar.isObsolete(BEYOND_LATIN) shouldBe false
            FieldGrammar.isObsolete('a') shouldBe false
            FieldGrammar.isBeyondLatin(BEYOND_LATIN) shouldBe true
            FieldGrammar.isBeyondLatin('é') shouldBe false
        }

        test("are quotable when they are whitespace, visible or obsolete") {
            FieldGrammar.isQuotable(TAB) shouldBe true
            FieldGrammar.isQuotable('é') shouldBe true
            FieldGrammar.isQuotable(DELETE) shouldBe false
        }
    }

    context("quoting") {
        test("escapes quotes and backslashes") {
            FieldGrammar.quote("""a"b\c""") shouldBe """"a\"b\\c""""
            FieldGrammar.tokenOrQuoted("a b") shouldBe "\"a b\""
            FieldGrammar.tokenOrQuoted("ab") shouldBe "ab"
        }
    }

    context("a quoted string") {
        test("reads escaped whitespace, visible and obsolete characters") {
            quoted(""""a\ b\"\é"""") shouldBe """a b"é"""
            quoted(inQuotes("a\\${TAB}b")) shouldBe "a${TAB}b"
        }

        test("refuses an escaped control character and an escape at the end") {
            quoted(inQuotes("a\\\u0001")).shouldBeNull()
            quoted("\"a\\").shouldBeNull()
            quoted("\"a").shouldBeNull()
        }

        test("reads characters beyond Latin-1 only where Unicode is allowed") {
            quoted("\"$BEYOND_LATIN\"").shouldBeNull()
            quoted("\"$BEYOND_LATIN\"", isUnicode = true) shouldBe BEYOND_LATIN.toString()
            quoted(inQuotes("\\$BEYOND_LATIN"), isUnicode = true).shouldBeNull()
            quoted("\"$DELETE\"", isUnicode = true).shouldBeNull()
        }

        test("leaves a token unquoted") {
            quoted("token rest") shouldBe "token"
            quoted(" token").shouldBeNull()
        }
    }
})
