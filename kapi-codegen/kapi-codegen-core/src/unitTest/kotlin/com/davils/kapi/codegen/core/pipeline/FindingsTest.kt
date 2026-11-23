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


package com.davils.kapi.codegen.core.pipeline

import com.davils.kore.core.text.TextRange
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kaml.document.pointer.JsonPointer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val LATE: Int = 40

private val first: DocumentUri = DocumentUri("memory:/a.yaml")

private val second: DocumentUri = DocumentUri("memory:/b.yaml")

private fun at(document: DocumentUri, pointer: String, start: Int? = null): Location =
    Location(document, JsonPointer.parse(pointer), start?.let(TextRange::at))

class FindingsTest : FunSpec({
    context("sorted") {
        test("orders by document, place, pointer, code and message and puts what has no place first") {
            val findings = Findings()
            findings.error(CodegenCodes.NAME_COLLISION, "in the second document", at(second, "/a", 1))
            findings.error(CodegenCodes.NAME_COLLISION, "late", at(first, "/a", LATE))
            findings.error(CodegenCodes.NAME_COLLISION, "by pointer b", at(first, "/b"))
            findings.error(CodegenCodes.NAME_COLLISION, "by pointer a", at(first, "/a"))
            findings.error(CodegenCodes.NAME_COLLISION, "same place, later code", at(first, "/a", 1))
            findings.error(CodegenCodes.EXTENSION_VALUE, "same place, second message", at(first, "/a", 1))
            findings.error(CodegenCodes.EXTENSION_VALUE, "same place, first message", at(first, "/a", 1))
            findings.error(CodegenCodes.EXTENSION_VALUE, "nowhere, second", null)
            findings.error(CodegenCodes.EXTENSION_VALUE, "nowhere, first", null)

            findings.sorted().map { diagnostic -> diagnostic.message } shouldBe listOf(
                "nowhere, first",
                "nowhere, second",
                "by pointer a",
                "by pointer b",
                "same place, first message",
                "same place, second message",
                "same place, later code",
                "late",
                "in the second document",
            )
        }

        test("keeps one of two findings that are the same") {
            val findings = Findings()
            findings.error(CodegenCodes.EXTENSION_VALUE, "twice", null)
            findings.error(CodegenCodes.EXTENSION_VALUE, "twice", null)

            findings.sorted().size shouldBe 1
            findings.hasErrors shouldBe true
        }

        test("has no errors without findings") {
            Findings().hasErrors shouldBe false
        }
    }
})
