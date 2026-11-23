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

package com.davils.kapi.spec.asyncapi.validation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GrammarsTest : FunSpec({
    context("a runtime expression") {
        test("reads a header or the payload, with an optional JSON pointer") {
            val valid = listOf(
                "\$message.header",
                "\$message.payload",
                "\$message.header#/a/b",
                "\$message.payload#",
                "\$message.payload#/a~1b",
            )
            val invalid = listOf(
                "",
                "\$message.",
                "\$message.body",
                "\$message.header#a",
                "\$message.header#/~2",
                "message.header",
            )

            valid.forEach { text -> Grammars.isRuntimeExpression(text) shouldBe true }
            invalid.forEach { text -> Grammars.isRuntimeExpression(text) shouldBe false }
        }
    }

    context("a media type") {
        test("is a type and a subtype of tokens with parameters") {
            val valid = listOf(
                "application/json",
                "text/plain;charset=utf-8",
                "a/b; q=\"x y\"",
                "application/vnd.a+json",
            )
            val invalid =
                listOf("", "json", "a/", "/b", "a/b/c", "a b/c", "a/b;c", "a/b;=c") +
                    listOf("a/b;c=\"", "a/b;c=\"x", "a/b;c=x\"")

            valid.forEach { text -> Grammars.isMediaType(text) shouldBe true }
            invalid.forEach { text -> Grammars.isMediaType(text) shouldBe false }
        }
    }

    context("a template") {
        test("names its variables in order") {
            Templates.names("{a}.{b}") shouldBe listOf("a", "b")
            Templates.names("plain") shouldBe emptyList()
            Templates.names("{a") shouldBe null
        }
    }

    context("a key") {
        test("of a component may hold dots, of a server not") {
            Grammars.isComponentKey("a.b-c_D9") shouldBe true
            Grammars.isComponentKey("") shouldBe false
            Grammars.isComponentKey("a b") shouldBe false
            Grammars.isIdentifier("a-b_C9") shouldBe true
            Grammars.isIdentifier("a.b") shouldBe false
            Grammars.isIdentifier("") shouldBe false
        }
    }
})
