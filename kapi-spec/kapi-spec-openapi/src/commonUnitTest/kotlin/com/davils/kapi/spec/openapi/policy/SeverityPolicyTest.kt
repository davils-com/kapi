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

package com.davils.kapi.spec.openapi.policy

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.core.policy.PolicyCodes
import com.davils.kapi.spec.openapi.diagnostic.CodeRules
import com.davils.kapi.spec.openapi.fixture.findings
import com.davils.kore.core.validation.violation.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import com.davils.kapi.spec.core.policy.SeverityPolicyBuilder
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kore.core.annotation.DavilsInternal

private const val REASON: String = "The vendor document carries fields of its own."

private fun policy(configure: SeverityPolicyBuilder.() -> Unit): SeverityPolicy {
    val builder = SeverityPolicyBuilder()
    builder.configure()
    return builder.build()
}

class SeverityPolicyTest : FunSpec({
    context("the default policy") {
        test("keeps the severity of every code") {
            val policy = SeverityPolicy.Default

            policy.areWarningsErrors shouldBe false
            policy.downgrades shouldBe emptyMap()
            policy.severityOf(DiagnosticCodes.FIELD_UNKNOWN, CodeRules.severityOf(DiagnosticCodes.FIELD_UNKNOWN)) shouldBe Severity.Error
            policy.severityOf(DiagnosticCodes.REFERENCE_SIBLING, CodeRules.severityOf(DiagnosticCodes.REFERENCE_SIBLING)) shouldBe Severity.Warning
        }
    }

    context("warnings as errors") {
        test("turns every warning into an error") {
            val policy = policy { areWarningsErrors = true }

            policy.severityOf(DiagnosticCodes.REFERENCE_SIBLING, CodeRules.severityOf(DiagnosticCodes.REFERENCE_SIBLING)) shouldBe Severity.Error
            policy.severityOf(DiagnosticCodes.FIELD_UNKNOWN, CodeRules.severityOf(DiagnosticCodes.FIELD_UNKNOWN)) shouldBe Severity.Error
            policy.severityOf(DiagnosticCodes.FIELD_UNKNOWN, Severity.Warning) shouldBe Severity.Error
        }
    }

    context("a downgrade") {
        test("turns an adjustable error into a warning and names its reason") {
            val policy = policy { downgrade(DiagnosticCodes.FIELD_UNKNOWN, REASON) }

            policy.isDowngraded(DiagnosticCodes.FIELD_UNKNOWN) shouldBe true
            policy.isDowngraded(DiagnosticCodes.FIELD_VERSION) shouldBe false
            policy.downgrades shouldBe mapOf(DiagnosticCodes.FIELD_UNKNOWN.value to REASON)
            policy.severityOf(DiagnosticCodes.FIELD_UNKNOWN, CodeRules.severityOf(DiagnosticCodes.FIELD_UNKNOWN)) shouldBe Severity.Warning
            policy.toString() shouldContain DiagnosticCodes.FIELD_UNKNOWN.value
        }

        test("stays a warning when warnings are errors, because it was acknowledged") {
            val policy = policy {
                areWarningsErrors = true
                downgrade(DiagnosticCodes.FIELD_UNKNOWN, REASON)
            }

            policy.severityOf(DiagnosticCodes.FIELD_UNKNOWN, CodeRules.severityOf(DiagnosticCodes.FIELD_UNKNOWN)) shouldBe Severity.Warning
        }


        test("is refused without a reason") {
            val builder = SeverityPolicyBuilder()
            builder.downgrade(DiagnosticCodes.FIELD_UNKNOWN, " ")
            val path = "downgrades[\"${DiagnosticCodes.FIELD_UNKNOWN}\"]"

            builder.validate().findings() shouldBe listOf(path to PolicyCodes.REASON_MISSING)
        }
    }
})
