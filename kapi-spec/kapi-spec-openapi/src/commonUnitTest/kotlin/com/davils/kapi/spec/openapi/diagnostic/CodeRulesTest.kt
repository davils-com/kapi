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

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kore.core.validation.violation.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

class CodeRulesTest : FunSpec({
    context("the rules of the codes") {
        test("only name codes of the catalogue") {
            DiagnosticCodes.all shouldContainAll CodeRules.warnings
            DiagnosticCodes.all shouldContainAll CodeRules.adjustable
        }

        test("keep the codes that guard the reader fixed") {
            val guards = DiagnosticCodes.all.filter { code ->
                val group = code.value.removePrefix("openapi.")
                group.startsWith("limit.") || group.startsWith("load.")
            }

            guards.filter { code -> code in CodeRules.adjustable }.shouldBeEmpty()
            (DiagnosticCodes.REFERENCE_OUTSIDE in CodeRules.adjustable) shouldBe false
        }

        test("make a code an error unless it is listed as a warning") {
            CodeRules.severityOf(DiagnosticCodes.REFERENCE_SIBLING) shouldBe Severity.Warning
            CodeRules.severityOf(DiagnosticCodes.FIELD_MISSING) shouldBe Severity.Error
        }
    }
})
