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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.fixture.codes
import com.davils.kapi.spec.asyncapi.fixture.document
import com.davils.kapi.spec.asyncapi.fixture.head
import com.davils.kapi.spec.asyncapi.fixture.reading
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import com.davils.kore.core.validation.violation.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val LONG: Int = 1000

class FieldReadingTest : FunSpec({
    context("a field") {
        test("that is unknown is reported with the field that comes closest") {
            val reading = readingOf("chanels: {}")
            val finding = reading.diagnostics.single()

            finding.code shouldBe DiagnosticCodes.FIELD_UNKNOWN
            finding.message shouldContain "did you mean channels?"
            finding.location
                .shouldNotBeNull()
                .pointer
                .toString() shouldBe "/chanels"
        }

        test("that is unknown and like no field is reported without a suggestion") {
            readingOf("something: {}").diagnostics.single().message shouldBe "\"something\" is no field of The document"
        }

        test("that is unknown is shown escaped and bounded in the message") {
            val key = """"line\nbreak\u202E${"x".repeat(LONG)}": 1"""
            val message = readingOf(key).diagnostics.single().message

            message shouldNotContain "\n"
            message shouldNotContain "\u202E"
            message shouldContain "\\n"
            message.length shouldBeLessThan LONG
        }

        test("of the wrong type is reported where it was written") {
            val reading = readingOf("channels: []")
            val location = reading.diagnostics
                .single()
                .location
                .shouldNotBeNull()

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_TYPE)
            location.pointer.toString() shouldBe "/channels"
            location.position.shouldNotBeNull().line shouldBe head.size
        }

        test("that is required and missing is reported at its object") {
            val reading = reading(listOf("asyncapi: 3.0.0", "info:", "  title: Chat"))

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_MISSING)
            reading.diagnostics.single().message shouldBe "The info needs the field version"
        }

        test("of AsyncAPI 2 is reported with what AsyncAPI 3 has instead") {
            val reading = readingOf(
                """
                    |tags: []
                    |channels:
                    |  chat:
                    |    publish: {}
                """.trimMargin(),
            )

            reading.codes() shouldContainExactly listOf(DiagnosticCodes.FIELD_VERSION, DiagnosticCodes.FIELD_VERSION)
            reading.diagnostics.last().message shouldContain "describes what an application does in operations"
        }

        test("may be downgraded to a warning with a reason") {
            val reading = readingOf("unknown: 1") {
                severities { downgrade(DiagnosticCodes.FIELD_UNKNOWN, "A vendor field") }
            }

            reading.document()
            reading.diagnostics.single().severity shouldBe Severity.Warning
        }
    }

    context("the source") {
        test("refuses merge keys unless they are accepted") {
            val text =
                """
                    |x-base: &base {title: Chat}
                    |channels:
                    |  chat:
                    |    <<: *base
                """.trimMargin()

            readingOf(text).codes() shouldBe listOf(DiagnosticCodes.SOURCE_MERGE_KEY)
            readingOf(text) { hasMergeKeys = true }.codes() shouldBe emptyList()
        }

        test("refuses a key that is no scalar") {
            readingOf("channels:\n  ? [a]\n  : {}").codes() shouldBe listOf(DiagnosticCodes.SOURCE_KEY_NOT_SCALAR)
        }

        test("refuses a value that JSON cannot express") {
            readingOf("x-when: .nan").codes() shouldContainExactly listOf(DiagnosticCodes.SOURCE_NON_JSON_VALUE)
        }
    }
})
