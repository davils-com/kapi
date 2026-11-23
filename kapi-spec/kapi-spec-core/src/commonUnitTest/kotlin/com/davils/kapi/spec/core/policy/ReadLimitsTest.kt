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

package com.davils.kapi.spec.core.policy

import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private fun codesOf(configure: ReadLimitsBuilder.() -> Unit): List<ViolationCode> {
    val builder = ReadLimitsBuilder()
    builder.configure()
    return builder.validate().errors.map { violation -> violation.code }
}

private val setters: List<ReadLimitsBuilder.(Long) -> Unit> = listOf(
    { value -> maximumDocumentLength = value.toInt() },
    { value -> maximumDocuments = value.toInt() },
    { value -> maximumDepth = value.toInt() },
    { value -> maximumNodes = value },
    { value -> maximumAliasExpansion = value },
    { value -> maximumReferenceHops = value.toInt() },
    { value -> maximumScalarLength = value.toInt() },
    { value -> maximumPatternLength = value.toInt() },
    { value -> maximumDiagnostics = value.toInt() },
)

private val ceilings: List<Long> = listOf(
    ReadLimits.HARD_DOCUMENT_LENGTH.toLong(),
    ReadLimits.HARD_DOCUMENTS.toLong(),
    ReadLimits.HARD_DEPTH.toLong(),
    ReadLimits.HARD_NODES,
    ReadLimits.HARD_ALIAS_EXPANSION,
    ReadLimits.HARD_REFERENCE_HOPS.toLong(),
    ReadLimits.HARD_SCALAR_LENGTH.toLong(),
    ReadLimits.HARD_PATTERN_LENGTH.toLong(),
    ReadLimits.HARD_DIAGNOSTICS.toLong(),
)

class ReadLimitsTest : FunSpec({
    context("the default limits") {
        test("take the defaults of the builder") {
            val limits = ReadLimits.Default

            limits.maximumDocumentLength shouldBe ReadLimitsBuilder.DEFAULT_DOCUMENT_LENGTH
            limits.maximumDocuments shouldBe ReadLimitsBuilder.DEFAULT_DOCUMENTS
            limits.maximumDepth shouldBe ReadLimitsBuilder.DEFAULT_DEPTH
            limits.maximumNodes shouldBe ReadLimitsBuilder.DEFAULT_NODES
            limits.maximumAliasExpansion shouldBe ReadLimitsBuilder.DEFAULT_ALIAS_EXPANSION
            limits.maximumReferenceHops shouldBe ReadLimitsBuilder.DEFAULT_REFERENCE_HOPS
            limits.maximumScalarLength shouldBe ReadLimitsBuilder.DEFAULT_SCALAR_LENGTH
            limits.maximumPatternLength shouldBe ReadLimitsBuilder.DEFAULT_PATTERN_LENGTH
            limits.maximumDiagnostics shouldBe ReadLimitsBuilder.DEFAULT_DIAGNOSTICS
        }

        test("name every limit") {
            val text = ReadLimits.Default.toString()

            text shouldContain "document=${ReadLimitsBuilder.DEFAULT_DOCUMENT_LENGTH}"
            text shouldContain "diagnostics=${ReadLimitsBuilder.DEFAULT_DIAGNOSTICS}"
        }
    }

    context("every limit") {
        test("may be 1 and its hard ceiling") {
            setters.zip(ceilings).forEach { (setter, ceiling) ->
                codesOf { setter(1L) } shouldBe emptyList()
                codesOf { setter(ceiling) } shouldBe emptyList()
            }
        }

        test("may not be 0 or below") {
            setters.forEach { setter ->
                codesOf { setter(0L) } shouldBe listOf(PolicyCodes.NOT_POSITIVE)
                codesOf { setter(-1L) } shouldBe listOf(PolicyCodes.NOT_POSITIVE)
            }
        }

        test("may not exceed its hard ceiling by one") {
            setters.zip(ceilings).forEach { (setter, ceiling) ->
                codesOf { setter(ceiling + 1) } shouldBe listOf(PolicyCodes.ABOVE_HARD_LIMIT)
            }
        }

        test("refuses to build outside its bounds") {
            val builder = ReadLimitsBuilder()
            builder.maximumDocuments = 0

            shouldThrow<ValidationException> { builder.build() }
        }
    }
})
