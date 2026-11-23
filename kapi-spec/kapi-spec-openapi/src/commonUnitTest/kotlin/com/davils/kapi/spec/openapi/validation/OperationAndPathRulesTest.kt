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

package com.davils.kapi.spec.openapi.validation

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.reading31
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val PATHS: String = "paths:"

private const val FINE: String = "responses: {'200': {description: Fine}}"

private const val ID: String = "{name: id, in: path, required: true, schema: {type: string}}"

class OperationAndPathRulesTest : FunSpec({
    context("an operation id") {
        test("names one operation only") {
            val first = "  /pets: {get: {operationId: list, $FINE}}"
            val second = "  /toys: {get: {operationId: list, $FINE}}"
            val reading = reading31(PATHS, first, second)
            val finding = reading.diagnostics.single()
            val location = finding.location.shouldNotBeNull()

            reading.codes() shouldBe listOf(DiagnosticCodes.OPERATION_ID_DUPLICATE)
            location.pointer.toString() shouldBe "/paths/~1toys/get"
        }
    }

    context("an operation without a response") {
        test("is read with a warning") {
            val reading = reading31(PATHS, "  /pets: {get: {operationId: list}}")

            reading.codes() shouldBe listOf(DiagnosticCodes.OPERATION_RESPONSES_EMPTY)
            reading.document()
        }
    }

    context("a path template") {
        test("names every parameter once between a pair of braces") {
            listOf("/pets/{id", "/pets/id}", "/pets/{}", "/pets/{id}/{id}").forEach { path ->
                val reading = reading31(PATHS, "  '$path': {}")

                reading.codes() shouldBe listOf(DiagnosticCodes.PATH_TEMPLATE)
            }
        }

        test("differs from every other template by more than the names of its parameters") {
            val reading = reading31(PATHS, "  /pets/{id}: {}", "  /pets/{petId}: {}")

            reading.codes() shouldBe listOf(DiagnosticCodes.PATH_AMBIGUOUS)
            reading.diagnostics.single().message shouldContain "/pets/{petId}"
        }
    }

    context("the parameters of a path") {
        test("are declared by the operation or by the path item above it") {
            val own = "  /pets/{id}: {get: {parameters: [$ID], $FINE}}"
            val shared = "  /toys/{id}: {parameters: [$ID], get: {$FINE}}"

            reading31(PATHS, own, shared).diagnostics.shouldBeEmpty()
        }

        test("are an error when the operation does not declare them") {
            val reading = reading31(PATHS, "  /pets/{id}: {get: {$FINE}}")

            reading.codes() shouldBe listOf(DiagnosticCodes.PATH_PARAMETER_UNDECLARED)
        }

        test("are an error when the path does not name them") {
            val reading = reading31(PATHS, "  /pets: {get: {parameters: [$ID], $FINE}}")

            reading.codes() shouldBe listOf(DiagnosticCodes.PATH_PARAMETER_UNUSED)
        }
    }
})
