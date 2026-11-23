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

package com.davils.kapi.spec.openapi.policy

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kapi.spec.core.policy.PolicyCodes
import com.davils.kapi.spec.openapi.fixture.findings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import com.davils.kapi.spec.core.policy.ReadLimitsBuilder
import com.davils.kapi.spec.core.policy.ReadLimits

class ReadLimitsTest : FunSpec({
    context("the default limits") {
        test("are valid and bound every dimension of a document") {
            val limits = ReadLimits.Default
            val report = ReadLimitsBuilder().validate()

            report.violations.shouldBeEmpty()
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

        test("render every dimension") {
            val rendered = ReadLimits.Default.toString()

            rendered shouldContain "documents=${ReadLimitsBuilder.DEFAULT_DOCUMENTS}"
            rendered shouldContain "diagnostics=${ReadLimitsBuilder.DEFAULT_DIAGNOSTICS}"
        }
    }

    context("validation") {
        test("reports every limit that is not positive at once") {
            val builder = ReadLimitsBuilder()
            builder.maximumDocumentLength = 0
            builder.maximumDocuments = 0
            builder.maximumDepth = 0
            builder.maximumNodes = 0
            builder.maximumAliasExpansion = 0
            builder.maximumReferenceHops = -1
            builder.maximumScalarLength = 0
            builder.maximumPatternLength = 0
            builder.maximumDiagnostics = 0
            val names = listOf(
                "maximumDocumentLength",
                "maximumDocuments",
                "maximumDepth",
                "maximumNodes",
                "maximumAliasExpansion",
                "maximumReferenceHops",
                "maximumScalarLength",
                "maximumPatternLength",
                "maximumDiagnostics",
            )

            builder.validate().findings() shouldBe
                names.map { name -> name to PolicyCodes.NOT_POSITIVE }
        }

        test("refuses a depth the parser cannot nest safely") {
            val builder = ReadLimitsBuilder()
            builder.maximumDepth = YamlLimits.HARD_DEPTH + 1

            builder.validate().findings() shouldBe listOf(
                "maximumDepth" to PolicyCodes.ABOVE_HARD_LIMIT,
            )
        }

        test("accepts the deepest nesting the parser allows") {
            val builder = ReadLimitsBuilder()
            builder.maximumDepth = YamlLimits.HARD_DEPTH

            builder.build().maximumDepth shouldBe YamlLimits.HARD_DEPTH
        }
    }
})
