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

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.value.ExtensionName
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.fixture.EMPTY_PATHS
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.head31
import com.davils.kapi.spec.openapi.fixture.partial
import com.davils.kapi.spec.openapi.fixture.petstoreUri
import com.davils.kapi.spec.openapi.fixture.reading
import com.davils.kapi.spec.openapi.fixture.reading30
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kore.core.text.TextPosition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val VERSION_LINE: String = "openapi: 3.1.0"

private const val EMPTY_INFO: String = "info: {}"

private const val ACCEPTED: String = "The vendor writes it that way."

private val missing = DiagnosticCodes.FIELD_MISSING

private val mistyped = DiagnosticCodes.FIELD_TYPE

class ReaderTest : FunSpec({
    context("a valid document") {
        test("is read with its version, its info and the source it came from") {
            val reading = reading31()
            val document = reading.document()

            document.version shouldBe Version.parseOrNull("3.1.0")
            document.info.title shouldBe "Petstore"
            document.info.version shouldBe "1.0.0"
            document.paths?.items shouldBe emptyMap()
            document.components shouldBe null
            reading.diagnostics.shouldBeEmpty()
            reading.sources.single().uri shouldBe petstoreUri
        }

        test("is read in the 3.0 family") {
            val document = reading30().document()

            document.version.family shouldBe VersionFamily.V30
            document.paths?.items shouldBe emptyMap()
        }

        test("keeps the place of every element") {
            val info = reading31().document().info

            info.location?.pointer shouldBe JsonPointer.of("info")
            info.location?.position shouldBe TextPosition(2, 2)
        }

        test("keeps the extensions of an object") {
            val document = reading31("x-audited: true").document()

            document.extensions[ExtensionName("x-audited")] shouldBe Value.Flag(true)
        }
    }

    context("what is no document") {
        test("is malformed when it is no YAML") {
            val reading = reading(listOf("openapi: [3.1.0"))

            reading.shouldBeInstanceOf<Reading.Malformed>()
            reading.sources.shouldBeEmpty()
        }

        test("is malformed when its root is no object") {
            reading(listOf("- openapi")).codes() shouldBe listOf(DiagnosticCodes.DOCUMENT_NOT_MAPPING)
        }

        test("is malformed when it names no version") {
            val reading = reading(listOf(EMPTY_INFO))

            reading.shouldBeInstanceOf<Reading.Malformed>()
            reading.codes() shouldBe listOf(DiagnosticCodes.VERSION_MISSING)
            reading.sources.single().uri shouldBe petstoreUri
        }

        test("is malformed when it is Swagger of another version than 2.0") {
            val reading = reading(listOf("swagger: '1.2'", EMPTY_INFO))

            reading.codes() shouldBe listOf(DiagnosticCodes.VERSION_SWAGGER)
            reading.diagnostics.single().message shouldContain "Swagger 1.2 is not read"
        }

        test("reads Swagger 2.0 as OpenAPI 3.1.1") {
            val reading = reading(
                listOf("swagger: '2.0'") + head31.drop(1) + EMPTY_PATHS,
            )

            reading.document().version shouldBe Version.parseOrNull("3.1.1")
            reading.codes() shouldBe listOf(DiagnosticCodes.VERSION_UPGRADED)
        }

        test("is malformed when its version is one the reader does not know") {
            listOf("openapi: 3.3.0", "openapi: 4.0.0", "openapi: three").forEach { line ->
                val reading = reading(listOf(line, EMPTY_INFO))

                reading.shouldBeInstanceOf<Reading.Malformed>()
                reading.codes() shouldBe listOf(DiagnosticCodes.VERSION_UNSUPPORTED)
            }
        }
    }

    context("a version without a patch") {
        test("is an error that names the full version") {
            val reading = reading(listOf("openapi: 3.1") + head31.drop(1) + EMPTY_PATHS)

            reading.codes() shouldBe listOf(DiagnosticCodes.VERSION_PATCH_MISSING)
            reading.partial().version.hasPatch shouldBe false
            reading.diagnostics.single().message shouldContain "3.1.0"
        }

        test("is a warning once the reader was told to accept it") {
            val reading = reading(listOf("openapi: '3.0'") + head31.drop(1) + EMPTY_PATHS) {
                severities { downgrade(DiagnosticCodes.VERSION_PATCH_MISSING, ACCEPTED) }
            }

            reading.document().version.family shouldBe VersionFamily.V30
            reading.diagnostics.single().isError shouldBe false
        }
    }

    context("a field") {
        test("is an error when the object does not know it, with the field that was meant") {
            val lines = listOf(VERSION_LINE, "inof:", "  title: Petstore", "info:", "  title: a", "  version: b")
            val reading = reading(lines + EMPTY_PATHS)
            val diagnostic = reading.diagnostics.single()

            diagnostic.code shouldBe DiagnosticCodes.FIELD_UNKNOWN
            diagnostic.message shouldBe "inof is no field of The document, did you mean info?"
            diagnostic.location?.position shouldBe TextPosition(1, 0)
            diagnostic.snippet?.line shouldBe "inof:"
        }

        test("is an error without a hint when nothing is close to it") {
            val reading = reading31("somethingElse: 1")

            reading.diagnostics.single().message shouldBe "somethingElse is no field of The document"
        }

        test("is an error when it belongs to the other version") {
            val reading = reading30("webhooks: {}")

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_VERSION)
            reading.diagnostics.single().message shouldContain "in OpenAPI 3.0"
        }

        test("is an error when it has the wrong type") {
            val reading = reading(listOf(VERSION_LINE, EMPTY_PATHS, "info:", "  title: Petstore", "  version: 1.0"))

            reading.codes() shouldBe listOf(missing, mistyped)
            reading.diagnostics.last().message shouldBe "version of The info must be a string"
        }

        test("is an error when it is required and missing") {
            val reading = reading(listOf(VERSION_LINE, EMPTY_INFO, EMPTY_PATHS))

            reading.codes() shouldBe listOf(missing, missing)
            reading.partial().info.title shouldBe ""
        }

        test("is an error when an object was expected and something else was written") {
            val reading = reading(listOf(VERSION_LINE, "info: Petstore", EMPTY_PATHS))

            reading.codes() shouldBe listOf(missing, mistyped)
        }

        test("is an error when it is a merge key the reader was not told to accept") {
            val reading = reading31("defaults: &defaults", "  x-a: 1", "<<: *defaults")

            val expected = listOf(DiagnosticCodes.FIELD_UNKNOWN, DiagnosticCodes.SOURCE_MERGE_KEY)

            reading.codes() shouldBe expected
        }

        test("is an error when its key is no scalar") {
            val reading = reading31("? [a, b]", ": 1")

            reading.codes() shouldBe listOf(DiagnosticCodes.SOURCE_KEY_NOT_SCALAR)
        }
    }

    context("a value") {
        test("is an error when JSON cannot express it") {
            val reading = reading31("x-limit: .inf")

            reading.codes() shouldBe listOf(DiagnosticCodes.SOURCE_NON_JSON_VALUE)
        }
    }
})
