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

package com.davils.kapi.codegen.core

import com.davils.kapi.codegen.core.fixture.generatedOf
import com.davils.kapi.codegen.core.fixture.paths
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kore.core.validation.violation.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class GenerationEqualityTest : FunSpec({
    context("a generation") {
        test("is equal by its files, findings, provenance and catalog") {
            val lines = paths("/pets: {get: {operationId: a, responses: {'204': {description: ok}}}}")
            val generated = generatedOf(lines)
            val noted = Diagnostic(
                code = CodegenCodes.NAME_COLLISION,
                severity = Severity.Warning,
                message = "a note",
                location = null,
            )
            val other = Generation.Generated(
                files = generated.files,
                diagnostics = listOf(noted),
                provenance = generated.provenance,
                catalog = generated.catalog,
            )

            generatedOf(lines) shouldBe generated
            other shouldNotBe generated
        }
    }
})
