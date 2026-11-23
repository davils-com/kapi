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

import com.davils.kapi.codegen.core.fixture.Golden
import com.davils.kapi.codegen.core.fixture.KotlinCompilation
import com.davils.kapi.codegen.core.fixture.KotlinStyle
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val DOCUMENT: String = "hostile"

private const val WHEN_VALUES: String = "com.example.petstore.model.PetstoreWhenVal"

private val hidden: List<Char> = listOf('\u202E', '\u2066', '\u0000', '\u2028', '\uFEFF')

private val values: List<String> = listOf("\"\"\"", "\${pwned}", "x*/y", "a\u202Eb", "c\\d", "fun", "VALUE")

class HostileDocumentTest : FunSpec({
    context("a document whose names and texts try to break out of where they are written") {
        test("becomes Kotlin that compiles and keeps the rules") {
            val files = Golden.generated(DOCUMENT).files

            KotlinCompilation.problems(files).shouldBeEmpty()
            KotlinStyle.problems(files).shouldBeEmpty()
        }

        test("calls and declares nothing the document names") {
            val files = Golden.kotlin(Golden.generated(DOCUMENT).files)

            files.flatMap(KotlinStyle::injections).shouldBeEmpty()
        }

        test("writes no invisible character and no raw string, only their escapes") {
            val text = Golden.rendered(Golden.generated(DOCUMENT).files)

            hidden.forEach { char -> text.contains(char) shouldBe false }
            text shouldContain "\\u202E"
        }

        test("keeps every text of the document as the data it was") {
            val read = KotlinCompilation.loaded(Golden.generated(DOCUMENT).files) { loader ->
                val type = loader.loadClass(WHEN_VALUES)
                val companion = type.getField("Companion").get(null)
                val entries = companion.javaClass.getMethod("getEntries").invoke(companion) as List<*>
                entries.map { entry -> entry?.toString().orEmpty() }
            }

            read shouldBe values
        }
    }
})
