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

package com.davils.kapi.spec.openapi

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.policy.PolicyCodes
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.core.policy.ReferencePolicy
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kapi.spec.core.source.Resolver
import com.davils.kapi.spec.openapi.fixture.configuration
import com.davils.kapi.spec.openapi.fixture.findings
import com.davils.kapi.spec.testing.MemoryResolver
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val REASON: String = "The vendor document carries fields of its own."

class ReaderBuilderTest : FunSpec({
    context("the default configuration") {
        test("resolves nothing, checks the structure twice and keeps every default policy") {
            val configuration = configuration()

            configuration.resolver shouldBe Resolver.None
            configuration.structure shouldBe StructureCheck.ReaderAndOfficialSchema
            configuration.hasMergeKeys shouldBe false
            configuration.limits shouldBe ReadLimits.Default
            configuration.references shouldBe ReferencePolicy.Default
            configuration.severities shouldBe SeverityPolicy.Default
            configuration.toString() shouldContain "structure=ReaderAndOfficialSchema"
        }
    }

    context("a configured reader") {
        test("takes every option it was given") {
            val resolver = MemoryResolver(emptyMap())
            val configuration = configuration {
                this.resolver = resolver
                structure = StructureCheck.Reader
                hasMergeKeys = true
                limits { maximumDocuments = 2 }
                references { root = DocumentUri("file:///api/") }
                severities { downgrade(DiagnosticCodes.FIELD_UNKNOWN, REASON) }
            }

            configuration.resolver shouldBe resolver
            configuration.structure shouldBe StructureCheck.Reader
            configuration.hasMergeKeys shouldBe true
            configuration.limits.maximumDocuments shouldBe 2
            configuration.references.root shouldBe DocumentUri("file:///api/")
            configuration.severities.isDowngraded(DiagnosticCodes.FIELD_UNKNOWN) shouldBe true
        }

        test("reports every invalid option at once with its path") {
            val builder = ReaderBuilder()
            builder.limits { maximumDocuments = 0 }
            builder.references { root = DocumentUri("file:///api/petstore.yaml") }
            builder.severities { downgrade(DiagnosticCodes.LIMIT_NODES, REASON) }

            builder.validate().findings() shouldBe listOf(
                "limits.maximumDocuments" to PolicyCodes.NOT_POSITIVE,
                "references.root" to PolicyCodes.ROOT_NOT_DIRECTORY,
                "severities" to ReaderViolationCodes.SEVERITY_FIXED,
            )
        }

        test("keeps the policies it had when a block is invalid and refuses to build") {
            val builder = ReaderBuilder()
            builder.limits { maximumDocuments = 0 }

            builder.limits shouldBe ReadLimits.Default
            shouldThrow<ValidationException> { builder.build() }
        }
    }
})
