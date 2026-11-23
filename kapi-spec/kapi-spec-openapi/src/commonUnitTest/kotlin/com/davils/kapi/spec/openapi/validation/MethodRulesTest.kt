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
import com.davils.kapi.spec.openapi.fixture.reading32
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val PATHS: String = "paths:"

private const val RESPONSES: String = "responses: {'200': {description: Fine}}"

private const val FINE: String = "{$RESPONSES}"

private fun additional(vararg methods: String): String {
    val entries = methods.joinToString(
        separator = ", ",
    ) { method -> "'${method.replace(oldValue = "'", newValue = "''")}': $FINE" }
    return "  /pets: {additionalOperations: {$entries}}"
}

class MethodRulesTest : FunSpec({
    context("the name of an additional operation") {
        test("is any method token in the case it is sent") {
            val reading = reading32(PATHS, additional("COPY", "link", "X-Purge.v2", "M!#$%&'*+-.^_`|~"))

            reading.diagnostics.shouldBeEmpty()
        }

        test("is an error when it is no method token") {
            listOf("", "CO PY", "COPY:", "CÖPY", "{COPY}").forEach { method ->
                val reading = reading32(PATHS, additional(method))

                reading.codes() shouldBe listOf(DiagnosticCodes.PATH_METHOD_TOKEN)
            }
        }

        test("is an error when it names a method with a field of its own, in any case") {
            listOf("GET", "get", "Post", "QUERY", "trace").forEach { method ->
                val reading = reading32(PATHS, additional(method))
                val location = reading.diagnostics
                    .single()
                    .location
                    .shouldNotBeNull()

                reading.codes() shouldBe listOf(DiagnosticCodes.PATH_METHOD_FIXED)
                location.pointer.toString() shouldBe "/paths/~1pets/additionalOperations/$method"
            }
        }

        test("is checked in webhooks and components as well") {
            val webhook = "  hook: {additionalOperations: {GET: $FINE}}"
            val component = "    Item: {additionalOperations: {'A B': $FINE}}"
            val reading = reading32("webhooks:", webhook, "components:", "  pathItems:", component)

            val expected = setOf(DiagnosticCodes.PATH_METHOD_FIXED, DiagnosticCodes.PATH_METHOD_TOKEN)

            reading.codes().toSet() shouldBe expected
        }
    }

    context("an additional operation") {
        test("declares the parameters its path names") {
            val reading = reading32(PATHS, "  /pets/{id}: {additionalOperations: {COPY: $FINE}}")

            reading.codes() shouldBe listOf(DiagnosticCodes.PATH_PARAMETER_UNDECLARED)
        }

        test("keeps its operation id apart from every other operation") {
            val first = "  /pets: {get: {operationId: copy, $RESPONSES}}"
            val second = "  /toys: {additionalOperations: {COPY: {operationId: copy, $RESPONSES}}}"
            val reading = reading32(PATHS, first, second)

            reading.codes() shouldBe listOf(DiagnosticCodes.OPERATION_ID_DUPLICATE)
        }

        test("names security schemes that the components declare") {
            val operation = "{security: [{missing: []}], $RESPONSES}"
            val reading = reading32(PATHS, "  /pets: {additionalOperations: {COPY: $operation}}")

            reading.codes() shouldBe listOf(DiagnosticCodes.SECURITY_UNKNOWN_SCHEME)
        }
    }
})
