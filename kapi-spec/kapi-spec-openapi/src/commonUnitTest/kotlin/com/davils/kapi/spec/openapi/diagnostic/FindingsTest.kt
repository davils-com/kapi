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

package com.davils.kapi.spec.openapi.diagnostic

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.fixture.configuration
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.text.TextPosition
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.violation.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val MAXIMUM: Int = 3

private const val IGNORED: String = "ignored"

private val first: DocumentUri = DocumentUri("memory:/petstore.yaml")

private val second: DocumentUri = DocumentUri("memory:/models.yaml")

private val order: Map<DocumentUri, Int> = mapOf(first to 0, second to 1)

private fun at(document: DocumentUri, offset: Int): Location = Location(
    document = document,
    pointer = JsonPointer.ROOT,
    range = TextRange.at(offset),
    position = TextPosition(line = 0, column = offset),
)

private fun findings(policy: SeverityPolicy = SeverityPolicy.Default): Findings = Findings(policy, MAXIMUM)

class FindingsTest : FunSpec({
    context("a collection of findings") {
        test("is empty and without errors until something is reported") {
            val findings = findings()

            findings.hasErrors shouldBe false
            findings.sorted(order).shouldBeEmpty()
        }

        test("gives a finding the severity of its code") {
            val findings = findings()

            findings.report(DiagnosticCodes.REFERENCE_SIBLING, IGNORED)
            findings.hasErrors shouldBe false
            findings.report(DiagnosticCodes.FIELD_UNKNOWN, "unknown")

            findings.hasErrors shouldBe true
            findings.sorted(order).map(Diagnostic::severity) shouldBe
                listOf(Severity.Error, Severity.Warning)
        }

        test("follows the policy of the reader") {
            val strict = configuration { severities { areWarningsErrors = true } }
            val findings = findings(strict.severities)

            findings.report(DiagnosticCodes.REFERENCE_SIBLING, IGNORED)

            findings.hasErrors shouldBe true
        }

        test("adopts a finding of the parser with the severity the parser gave it") {
            val findings = findings()
            val snippet = LineMap.of("a: 1").snippet(TextRange.at(0))
            val code = DiagnosticCodes.SOURCE_MULTIPLE_DOCUMENTS

            findings.adopt(
                code = code,
                base = Severity.Warning,
                message = "several",
                location = at(first, 0),
                snippet = snippet,
            )

            findings.hasErrors shouldBe false
            findings.sorted(order).single().snippet shouldBe snippet
        }

        test("orders findings by document, by place, by code and by message") {
            val findings = Findings(SeverityPolicy.Default, Int.MAX_VALUE)
            findings.report(DiagnosticCodes.FIELD_UNKNOWN, "in the second document", at(second, 0))
            findings.report(DiagnosticCodes.FIELD_UNKNOWN, "late", at(first, 2))
            findings.report(DiagnosticCodes.FIELD_UNKNOWN, "b", at(first, 1))
            findings.report(DiagnosticCodes.FIELD_UNKNOWN, "a", at(first, 1))
            findings.report(DiagnosticCodes.FIELD_MISSING, "missing", at(first, 1))
            findings.report(DiagnosticCodes.LOAD_FAILED, "nowhere")

            findings.sorted(order).map(Diagnostic::message) shouldBe
                listOf("nowhere", "missing", "a", "b", "late", "in the second document")
        }

        test("keeps a finding that was reported twice only once") {
            val findings = findings()

            repeat(2) { findings.report(DiagnosticCodes.REFERENCE_SIBLING, IGNORED) }

            findings.sorted(order).size shouldBe 1
        }

        test("drops what exceeds the limit and says so with an error") {
            val findings = findings()

            repeat(MAXIMUM + 2) { index ->
                findings.report(DiagnosticCodes.REFERENCE_SIBLING, "$IGNORED $index")
            }
            val sorted = findings.sorted(order)

            findings.hasErrors shouldBe true
            sorted.size shouldBe MAXIMUM + 1
            sorted.last().code shouldBe DiagnosticCodes.LIMIT_DIAGNOSTICS
            sorted.last().message shouldContain "2 further findings were dropped"
        }
    }
})
