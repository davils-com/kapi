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

package com.davils.kapi.spec.openapi.model.path

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val LONG: Int = 100_000

private const val SLASHED: String = "/{a/b}"

private const val EMPTY: String = "{}"

private val pathOracle: Regex = Regex("""\{([^{}/]+)}""")

private val serverOracle: Regex = Regex("""\{([^{}]+)}""")

private val texts: List<String> = listOf(
    "",
    "/",
    "{",
    "}",
    EMPTY,
    "{a}",
    "/{a}/{b}",
    "/{a}{b}",
    "/{{a}}",
    "/{a{b}",
    "/{a}b}",
    SLASHED,
    "/{a/{b}",
    "/{}{a}",
    "/{ }",
    "/{ä😀}",
    "/{a\nb}",
    "/{a",
    "/a}",
    "{a}}{b}",
    "/{{{{",
    "https://{host}:{port}/v{version}",
    "https://{host/x}",
)

private fun oracleNames(oracle: Regex, text: String): List<String> {
    val matches = oracle.findAll(text).toList()
    return matches.map { match -> match.groupValues[1] }
}

private fun pathNames(text: String): List<String> = Placeholders.names(text = text, stops = Placeholders.PATH_STOPS)

private fun serverOf(text: String): List<String> = Placeholders.names(text = text, stops = Placeholders.SERVER_STOPS)

class PlaceholdersTest : FunSpec({
    context("the names between braces") {
        test("are found as the regular expression of a path finds them") {
            texts.forEach { text -> pathNames(text) shouldBe oracleNames(pathOracle, text) }
        }

        test("are found as the regular expression of a server finds them") {
            texts.forEach { text -> serverOf(text) shouldBe oracleNames(serverOracle, text) }
        }

        test("allow a slash in a server variable but not in a path parameter") {
            serverOf(SLASHED) shouldBe listOf("a/b")
            pathNames(SLASHED) shouldBe emptyList()
        }
    }

    context("the replacement of every placeholder") {
        test("matches the replacement of the regular expression") {
            texts.forEach { text ->
                val replaced = Placeholders.replaced(text = text, stops = Placeholders.PATH_STOPS, replacement = EMPTY)

                replaced shouldBe pathOracle.replace(input = text, replacement = EMPTY)
            }
        }

        test("keeps the text without placeholders") {
            Placeholders.replaced(text = "/pets", stops = Placeholders.PATH_STOPS, replacement = "") shouldBe "/pets"
        }
    }

    context("hostile text") {
        test("is scanned once, however many braces stay open") {
            val open = "/" + "{a".repeat(LONG)
            val unclosed = "/{" + "a".repeat(LONG)

            pathNames(open) shouldBe emptyList()
            serverOf(unclosed) shouldBe emptyList()
        }
    }
})
