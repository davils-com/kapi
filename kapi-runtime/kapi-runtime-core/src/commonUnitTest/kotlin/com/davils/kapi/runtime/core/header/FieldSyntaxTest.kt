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

package com.davils.kapi.runtime.core.header

import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FieldSyntaxTest : FunSpec({
    context("a token") {
        test("holds letters, digits and the symbols of RFC 9110") {
            FieldSyntax.isToken("X-Request-Id") shouldBe true
            FieldSyntax.isToken("a!#$%&'*+-.^_`|~") shouldBe true
            FieldSyntax.isTokenChar('~') shouldBe true
        }

        test("is never empty and holds no separator, space or character beyond ASCII") {
            listOf("", "a b", "a:b", "a\"b", "a(b", "é", "a\r\nb").map(FieldSyntax::isToken) shouldBe
                List(size = 7) { false }
            FieldSyntax.isTokenChar('/') shouldBe false
        }
    }

    context("a field value") {
        test("may hold spaces, tabs and characters beyond ASCII") {
            FieldSyntax.isFieldValue("text/plain; q=0.5\tx") shouldBe true
            FieldSyntax.isFieldValue("é") shouldBe true
            FieldSyntax.isFieldValue("") shouldBe true
        }

        test("holds no line break, NUL or DEL that could inject a field") {
            listOf("a\rb", "a\nb", "a\u0000b", "a\u007Fb").map(FieldSyntax::isFieldValue) shouldBe
                List(size = 4) { false }
            FieldSyntax.isControl('\t') shouldBe false
            FieldSyntax.isControl('\u001B') shouldBe true
        }
    }
})
