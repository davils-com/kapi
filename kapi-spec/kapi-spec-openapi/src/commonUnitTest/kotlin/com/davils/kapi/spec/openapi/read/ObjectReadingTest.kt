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

package com.davils.kapi.spec.openapi.read

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.errors
import com.davils.kapi.spec.openapi.fixture.operation
import com.davils.kapi.spec.openapi.fixture.partial
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.model.path.Method
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val PATHS: String = "paths:"

private const val PETS: String = "  /pets:"

private const val PETS_PATH: String = "/pets"

private const val GET: String = "    get:"

private val mistyped = DiagnosticCodes.FIELD_TYPE

private val limit: List<String> = listOf(PATHS, PETS, "    parameters:", "      - name: limit", "        schema: {}")

class ObjectReadingTest : FunSpec({
    context("a parameter") {
        test("is refused in a place that does not exist") {
            val reading = reading31(limit + "        in: body")

            reading.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_LOCATION)
            reading.diagnostics.single().message shouldContain "'body' is no place of a parameter"
        }

        test("is refused with a style that does not exist") {
            val reading = reading31(limit + listOf("        in: query", "        style: wide"))

            reading.codes() shouldBe listOf(DiagnosticCodes.PARAMETER_STYLE)
            reading.diagnostics.single().message shouldBe "'wide' is no style of a parameter"
        }
    }

    context("the responses of an operation") {
        test("are refused under a status that does not exist") {
            val responses = listOf("      responses:", "        '600':", "          description: Odd")
            val reading = reading31(listOf(PATHS, PETS, GET) + responses)
            val get = reading.partial().operation(PETS_PATH, Method.Get)

            reading.errors() shouldBe listOf(DiagnosticCodes.RESPONSE_STATUS)
            get.responses.entries shouldBe emptyMap()
        }

        test("must be an object") {
            reading31(PATHS, PETS, GET, "      responses: none").errors() shouldBe listOf(mistyped)
        }

        test("are empty when an operation declares none") {
            val reading = reading31(PATHS, PETS, GET, "      operationId: listPets")
            val get = reading.document().operation(PETS_PATH, Method.Get)

            get.responses.entries shouldBe emptyMap()
        }
    }

    context("a path") {
        test("starts with a slash") {
            val reading = reading31(PATHS, "  pets: {}")

            reading.codes() shouldBe listOf(DiagnosticCodes.PATH_NOT_ROOTED)
            reading.partial().paths?.items shouldBe emptyMap()
        }
    }

    context("a list of texts") {
        test("holds nothing but strings") {
            val reading = reading31(PATHS, PETS, GET, "      tags: [pets, 1]")

            reading.errors() shouldBe listOf(mistyped)
        }
    }

    context("an object that is keyed by names") {
        test("must be an object") {
            val callback = reading31(PATHS, PETS, GET, "      callbacks:", "        onChange: none")

            reading31("paths: none").errors() shouldContain mistyped
            callback.errors() shouldBe listOf(mistyped)
        }
    }
})
