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

package com.davils.kapi.spec.openapi.read.path

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.DocumentBuilder
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Version
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.operation
import com.davils.kapi.spec.openapi.fixture.path
import com.davils.kapi.spec.openapi.fixture.readable
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.reading32
import com.davils.kapi.spec.openapi.fixture.rewritten
import com.davils.kapi.spec.openapi.model.path.Method
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val PATHS: String = "paths:"

private const val PETS: String = "/pets"

private const val FINE: String = "responses: {'200': {description: Fine}}"

private const val SEARCH: String = "  /pets: {query: {operationId: search, $FINE}}"

private const val COPY: String = "  /pets: {additionalOperations: {COPY: {operationId: copy, $FINE}}}"

class MethodReadingTest : FunSpec({
    context("the QUERY method") {
        test("is read from a path item of OpenAPI 3.2") {
            val reading = reading32(PATHS, SEARCH)

            reading.diagnostics.shouldBeEmpty()
            reading.document().operation(PETS, Method.Query).operationId shouldBe "search"
        }

        test("is no field of a path item before OpenAPI 3.2") {
            val reading = reading31(PATHS, SEARCH)

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_VERSION)
            reading.readable().path(PETS).operations shouldBe emptyMap()
        }

        test("is written for OpenAPI 3.2 and reads back") {
            val document = reading32(PATHS, SEARCH).document()

            OpenApi.write(document) shouldContain "query:"
            document.rewritten() shouldBe document
        }
    }

    context("an additional operation") {
        test("is read by the name of its method as it is sent") {
            val reading = reading32(PATHS, COPY)
            val item = reading.document().path(PETS)

            reading.diagnostics.shouldBeEmpty()
            item.additionalOperations.keys shouldBe setOf("COPY")
            item.additionalOperations.getValue("COPY").operationId shouldBe "copy"
        }

        test("is no field of a path item before OpenAPI 3.2") {
            val reading = reading31(PATHS, COPY)

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_VERSION)
            reading.readable().path(PETS).additionalOperations shouldBe emptyMap()
        }

        test("is written for OpenAPI 3.2 and reads back") {
            val document = reading32(PATHS, COPY).document()

            OpenApi.write(document) shouldContain "additionalOperations:"
            document.rewritten() shouldBe document
        }

        test("is not written for an older version, which has no such field") {
            val read = reading32(PATHS, COPY, "  /toys: {query: {operationId: search, $FINE}}").document()
            val builder = DocumentBuilder()
            builder.version = checkNotNull(Version.parseOrNull("3.1.0"))
            builder.info = read.info
            builder.paths = read.paths
            val written = OpenApi.write(builder.build())

            written shouldNotContain "additionalOperations"
            written shouldNotContain "query"
        }
    }
})
