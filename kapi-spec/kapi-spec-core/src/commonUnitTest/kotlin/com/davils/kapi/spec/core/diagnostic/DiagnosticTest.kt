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

package com.davils.kapi.spec.core.diagnostic

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.text.TextPosition
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith

private const val TEXT: String = "openapi: 3.1.0\ninof: {}\n"

private val range: TextRange = TextRange.of(start = 15, length = 4)

private val location: Location = Location(
    document = DocumentUri("file:///api/petstore.yaml"),
    pointer = JsonPointer.of("inof"),
    range = range,
    position = TextPosition(line = 1, column = 0),
)

private val unknown: Diagnostic = Diagnostic(
    code = ViolationCode("openapi.field.unknown"),
    severity = Severity.Error,
    message = "inof is no field of the document, did you mean info?",
    location = location,
    snippet = LineMap.of(TEXT).snippet(range),
)

private val sibling: Diagnostic = Diagnostic(
    code = ViolationCode("openapi.reference.sibling"),
    severity = Severity.Warning,
    message = "fields beside a reference are ignored",
)

class DiagnosticTest : FunSpec({
    context("a diagnostic") {
        test("renders its place, its severity, its code and its message in one line") {
            val line = "file:///api/petstore.yaml:2:1#/inof: error: [openapi.field.unknown] " +
                "inof is no field of the document, did you mean info?"

            unknown.toString() shouldBe line
        }

        test("renders without a place when it has none") {
            sibling.toString() shouldBe
                "warning: [openapi.reference.sibling] fields beside a reference are ignored"
        }

        test("tells an error from a warning") {
            unknown.isError shouldBe true
            sibling.isError shouldBe false
        }

        test("equals a diagnostic of the same finding") {
            val same = Diagnostic(
                code = unknown.code,
                severity = unknown.severity,
                message = unknown.message,
                location = location,
                snippet = unknown.snippet,
            )

            unknown shouldBe same
            unknown.hashCode() shouldBe same.hashCode()
            unknown shouldNotBe sibling
            unknown.equals(unknown.message) shouldBe false
        }
    }

    context("render") {
        test("shows the line of the source below the diagnostic") {
            val rendered = unknown.render()

            rendered shouldStartWith unknown.toString()
            rendered shouldContain "inof: {}"
        }

        test("is the line alone when there is no snippet") {
            sibling.render() shouldBe sibling.toString()
        }

        test("joins several diagnostics line by line") {
            listOf(sibling, sibling).render() shouldBe sibling.toString() + "\n" + sibling.toString()
        }
    }

    context("hasErrors") {
        test("is true as soon as one diagnostic is an error") {
            listOf(sibling, unknown).hasErrors shouldBe true
            listOf(sibling).hasErrors shouldBe false
            emptyList<Diagnostic>().hasErrors shouldBe false
        }
    }
})
