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

package com.davils.kapi.spec.core.policy

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val accepted: ViolationCode = ViolationCode("test.accepted")

private val other: ViolationCode = ViolationCode("test.other")

private fun policy(configure: SeverityPolicyBuilder.() -> Unit): SeverityPolicy {
    val builder = SeverityPolicyBuilder()
    builder.configure()
    return builder.build()
}

class SeverityPolicyTest : FunSpec({
    context("the default policy") {
        test("keeps the severity every code has") {
            val policy = SeverityPolicy.Default

            policy.severityOf(other, Severity.Warning) shouldBe Severity.Warning
            policy.severityOf(other, Severity.Error) shouldBe Severity.Error
            policy.isDowngraded(other) shouldBe false
            policy.toString() shouldBe "SeverityPolicy(areWarningsErrors=false, downgrades=[])"
        }
    }

    context("a strict policy") {
        test("turns every warning into an error") {
            val policy = policy { areWarningsErrors = true }

            policy.severityOf(other, Severity.Warning) shouldBe Severity.Error
            policy.areWarningsErrors shouldBe true
        }
    }

    context("a downgrade") {
        test("makes a warning of the code it names, even in a strict policy") {
            val policy = policy {
                areWarningsErrors = true
                downgrade(accepted, "the gateway adds the header")
            }

            policy.severityOf(accepted, Severity.Error) shouldBe Severity.Warning
            policy.severityOf(other, Severity.Error) shouldBe Severity.Error
            policy.isDowngraded(accepted) shouldBe true
            policy.downgrades shouldBe mapOf(accepted.value to "the gateway adds the header")
            policy.toString() shouldBe "SeverityPolicy(areWarningsErrors=true, downgrades=[test.accepted])"
        }

        test("needs a reason that is not blank") {
            listOf("", "   ").forEach { reason ->
                val builder = SeverityPolicyBuilder()
                builder.downgrade(accepted, reason)

                val codes = builder.validate().errors.map { violation -> violation.code }

                codes shouldBe listOf(PolicyCodes.REASON_MISSING)
            }
        }
    }
})
