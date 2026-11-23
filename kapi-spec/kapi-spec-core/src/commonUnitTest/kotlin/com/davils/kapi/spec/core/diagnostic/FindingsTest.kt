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

package com.davils.kapi.spec.core.diagnostic

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kapi.spec.core.policy.SeverityPolicyBuilder
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val TEXT: String = "a: 1\nb: 2\n"

private const val BROKEN: String = "broken"

private const val DOUBTFUL: String = "doubtful"

private const val ONE: String = "one"

private const val NOWHERE: String = "nowhere"

private val first: DocumentUri = DocumentUri("memory:/first.yaml")

private val second: DocumentUri = DocumentUri("memory:/second.yaml")

private val broken: ViolationCode = ViolationCode("test.broken")

private val doubtful: ViolationCode = ViolationCode("test.doubtful")

private val overflow: ViolationCode = ViolationCode("test.overflow")

private val pointer: JsonPointer = JsonPointer.of("b")

private fun findings(maximum: Int = 10, policy: SeverityPolicy = SeverityPolicy.Default): Findings = Findings(
    policy = policy,
    maximum = maximum,
    overflow = overflow,
    baseSeverity = { code -> Severity.Warning.takeIf { code == doubtful } ?: Severity.Error },
)

private fun at(document: DocumentUri, offset: Int): Location =
    Location(document = document, pointer = pointer, range = TextRange.at(offset))

class FindingsTest : FunSpec({
    context("a report") {
        test("takes the severity the code has and keeps the location") {
            val findings = findings()
            val place = at(first, 0)

            findings.report(doubtful, DOUBTFUL, place)

            val diagnostic = findings.sorted(emptyMap()).single()
            diagnostic.severity shouldBe Severity.Warning
            diagnostic.location shouldBe place
            findings.hasErrors shouldBe false
        }

        test("is an error when the code is one, or when warnings are errors") {
            val strict = SeverityPolicyBuilder().apply { areWarningsErrors = true }
            val errors = findings()
            val promoted = findings(policy = strict.build())

            errors.report(broken, BROKEN)
            promoted.report(doubtful, DOUBTFUL)

            errors.hasErrors shouldBe true
            promoted.hasErrors shouldBe true
        }

        test("is kept once, however often it is made") {
            val findings = findings()

            findings.report(broken, BROKEN, at(first, 0))
            findings.report(broken, BROKEN, at(first, 0))

            findings.sorted(emptyMap()).size shouldBe 1
        }
    }

    context("an adopted finding") {
        test("keeps its base severity unless the policy changes it") {
            val downgrading = SeverityPolicyBuilder().apply { downgrade(broken, "known and accepted") }
            val findings = findings(policy = downgrading.build())
            val lines = LineMap.of(TEXT)
            val range = TextRange.at(TEXT.indexOf('b'))
            val place = Location(
                document = first,
                pointer = pointer,
                range = range,
                position = lines.positionOf(range.start),
            )

            findings.adopt(
                code = broken,
                base = Severity.Error,
                message = "adopted",
                location = place,
                snippet = lines.snippet(range),
            )
            findings.adopt(
                code = doubtful,
                base = Severity.Error,
                message = "kept",
                location = place,
                snippet = lines.snippet(range),
            )

            val severities = findings.sorted(emptyMap()).map(Diagnostic::severity)
            severities shouldBe listOf(Severity.Warning, Severity.Error)
        }
    }

    context("the limit of a reading") {
        test("keeps the findings up to it and counts the rest in one error") {
            val findings = findings(maximum = 2)

            findings.report(doubtful, ONE)
            findings.report(doubtful, "two")
            findings.report(doubtful, "three")
            findings.report(doubtful, "four")

            val sorted = findings.sorted(emptyMap())
            sorted.map(Diagnostic::code) shouldBe listOf(doubtful, doubtful, overflow)
            sorted.last().severity shouldBe Severity.Error
            sorted.last().message shouldContain "2 further findings were dropped"
            findings.hasErrors shouldBe true
        }

        test("adds nothing when nothing was dropped") {
            val findings = findings(maximum = 1)

            findings.report(doubtful, ONE)

            findings.sorted(emptyMap()).map(Diagnostic::code) shouldBe listOf(doubtful)
        }
    }

    context("the findings of a reading") {
        test("are sorted by document, place, code and message, with the unlocated first") {
            val findings = findings()
            findings.report(broken, "later", at(second, 0))
            findings.report(doubtful, "b", at(first, TEXT.length))
            findings.report(broken, "b", at(first, TEXT.length))
            findings.report(broken, "a", at(first, TEXT.length))
            findings.report(broken, "unplaced", Location(first, JsonPointer.ROOT))
            findings.report(broken, NOWHERE)
            findings.report(broken, "unknown document", at(DocumentUri("memory:/third.yaml"), 0))

            val messages = findings.sorted(mapOf(first to 0, second to 1)).map(Diagnostic::message)

            messages shouldBe listOf(NOWHERE, "unknown document", "unplaced", "a", "b", "b", "later")
        }

        test("name the places of a document") {
            val findings = findings()
            findings.report(broken, "first", at(first, 0))
            findings.report(broken, "second", at(second, 0))
            findings.report(broken, NOWHERE)

            findings.pointersIn(first) shouldBe setOf(pointer)
            findings.pointersIn(DocumentUri("memory:/third.yaml")) shouldBe emptySet()
        }
    }
})
