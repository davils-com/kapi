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

package com.davils.kapi.codegen.core.asyncapi

import com.davils.kapi.codegen.core.MediaTypes
import com.davils.kapi.codegen.core.asyncapi.plan.PathPart
import com.davils.kapi.codegen.core.asyncapi.read.Addresses
import com.davils.kapi.codegen.core.asyncapi.read.Correlations
import com.davils.kapi.codegen.core.emit.Texts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val parameter: (String) -> PathPart.Parameter = { name ->
    PathPart.Parameter(name = name, member = name, default = null, choices = emptyList())
}

private const val QUOTE: String = "\""

private const val BACKSLASH: String = "\\"

private const val DOLLAR: String = "\${'\$'}"

private const val CR: String = "\r"

private const val CRLF: String = "\r\n"

private const val LIMIT: Int = 2_000

private const val ADDRESS_LIMIT: Int = 2_048

private const val LONGER: Int = 5_000

private const val SHOWN: Int = 20

private fun quoted(text: String): String = QUOTE + text + QUOTE

private fun escape(hex: String): String = BACKSLASH + "u" + hex

private val literals: Map<String, String> = mapOf(
    "" to quoted(""),
    "plain" to quoted("plain"),
    "a\$b" to quoted("a" + DOLLAR + "b"),
    "\u202E" to quoted(escape("202e")),
    "a\nb" to listOf(quoted("a"), quoted(escape("000a")), quoted("b")).joinToString(separator = " + "),
    "\uD83D\uDE00" to quoted(escape("d83d") + escape("de00")),
    "a\u0085b" to listOf(quoted("a"), quoted(escape("0085")), quoted("b")).joinToString(separator = " + "),
    "lone \uD800" to quoted("lone ") + " + " + quoted(escape("d800")),
)

private val kdocs: Map<String, String> = mapOf(
    "ends */ here" to "ends *&#47; here",
    "opens /* here" to "opens &#47;* here",
    "@param x" to "&#64;param x",
    "line" + CRLF + "break" + CR + "here" to "line\nbreak\nhere",
    "\u202Eflip" to "\\u202eflip",
    "dot \u00B7 and \u2662 and %" to "dot &#183; and &#9826; and &#37;",
    "smile \uD83D\uDE00" to "smile \uD83D\uDE00",
    "next\u0085line" to "next\\u0085line",
)

private val addresses: Map<String, List<PathPart>?> = mapOf(
    "/rooms/{roomId}" to listOf(PathPart.Literal("/rooms/"), parameter("roomId")),
    "rooms" to listOf(PathPart.Literal("/rooms")),
    "/{a}-{b}.json" to listOf(
        PathPart.Literal("/"),
        parameter("a"),
        PathPart.Literal("-"),
        parameter("b"),
        PathPart.Literal(".json"),
    ),
    "/rooms?x=1" to null,
    "/rooms/{unclosed" to null,
    "/rooms/{}" to null,
    "/rooms/{a b}" to null,
    "/a b" to null,
    "/{a}/x y" to null,
    "/" + "a".repeat(ADDRESS_LIMIT) to null,
)

private val types: Map<String, Boolean> = mapOf(
    "application/json" to true,
    "Application/JSON; charset=utf-8" to true,
    "application/vnd.example+json" to true,
    "text/json" to false,
    "application/xml" to false,
    "application/json-patch" to false,
)

private val expressions: Map<String, List<String>?> = mapOf(
    "\$message.payload#/id" to listOf("id"),
    "\$message.payload#" to emptyList(),
    "\$message.payload#/a~1b/0" to listOf("a/b", "0"),
    "\$message.header#/id" to null,
    "\$message.payload#id" to null,
)

class TextsTest : FunSpec({
    context("a string literal") {
        literals.forEach { (text, code) ->
            test("escapes what a reader could misread: ${code.take(SHOWN)}") {
                Texts.literal(text).toString() shouldBe code
            }
        }
    }

    context("a KDoc text") {
        kdocs.forEach { (text, shown) ->
            test("cannot end its comment or open a tag: ${shown.take(SHOWN)}") {
                Texts.kdoc(text) shouldBe shown
            }
        }

        test("is shortened to a bounded length") {
            Texts.kdoc("x".repeat(LONGER)).length shouldBe LIMIT
        }
    }

    context("an address") {
        addresses.forEach { (address, parts) ->
            test("is a path template or refused: ${address.take(SHOWN)}") {
                Addresses.parts(address, parameter) shouldBe parts
            }
        }
    }

    context("a content type") {
        types.forEach { (type, isJson) ->
            test("is JSON or not: $type") {
                MediaTypes.isJson(type) shouldBe isJson
            }
        }
    }

    context("a correlation id") {
        expressions.forEach { (expression, tokens) ->
            test("is read from the payload only: $expression") {
                Correlations.tokens(expression) shouldBe tokens
            }
        }
    }
})
