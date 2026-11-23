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

package com.davils.kapi.spec.openapi.read.content

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.operation
import com.davils.kapi.spec.openapi.fixture.readable
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.reading32
import com.davils.kapi.spec.openapi.fixture.rewritten
import com.davils.kapi.spec.openapi.model.content.Response
import com.davils.kapi.spec.openapi.model.path.Method
import com.davils.kapi.spec.openapi.resolve
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val PATHS: String = "paths:"

private const val SUMMARISED: String = "  /pets: {get: {responses: {'200': {summary: Pets}}}}"

private const val DESCRIBED: String = "  /pets: {get: {responses: {'200': {summary: Pets, description: All pets}}}}"

private fun Document.ok(): Response {
    val operation = operation("/pets", Method.Get)
    return resolve(operation.responses.entries.values.single()).shouldNotBeNull()
}

class ResponseReadingTest : FunSpec({
    context("a response of OpenAPI 3.2") {
        test("may leave out its description and carries a summary") {
            val reading = reading32(PATHS, SUMMARISED)
            val response = reading.document().ok()

            reading.diagnostics.shouldBeEmpty()
            response.summary shouldBe "Pets"
            response.description.shouldBeNull()
        }

        test("is written as it was read and reads back") {
            val summarised = reading32(PATHS, SUMMARISED).document()
            val described = reading32(PATHS, DESCRIBED).document()

            OpenApi.write(summarised) shouldNotContain "description"
            OpenApi.write(described) shouldContain "summary: Pets"
            summarised.rewritten() shouldBe summarised
            described.rewritten() shouldBe described
        }
    }

    context("a response before OpenAPI 3.2") {
        test("needs a description and has no summary") {
            val reading = reading31(PATHS, SUMMARISED)
            val response = reading.readable().ok()

            val expected = listOf(DiagnosticCodes.FIELD_VERSION, DiagnosticCodes.FIELD_MISSING)

            reading.codes() shouldContainExactlyInAnyOrder expected
            response.summary.shouldBeNull()
        }
    }
})
