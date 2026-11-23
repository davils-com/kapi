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

package com.davils.kapi.spec.openapi.structure

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.core.policy.ReadLimitsBuilder
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.StructureCheck
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kapi.spec.openapi.fixture.EMPTY_PATHS
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.petstoreUri
import com.davils.kapi.spec.openapi.fixture.reading
import com.davils.kapi.spec.openapi.fixture.reading30
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.reading32
import com.davils.kapi.spec.openapi.fixture.yamlOf
import com.davils.kapi.spec.openapi.upgrade.nodeOfLines
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.io.bytestring.ByteString

private const val ACCEPTED: String = "The vendor names both."

private const val LICENSE_POINTER: String = "/info/license"

private const val SHALLOW: Int = 3

private const val MAXIMUM_DIAGNOSTICS: Int = 100

private const val KNOWN: String = "The reader knows this place."

private val licensed: List<String> = listOf(
    "info:",
    "  title: Petstore",
    "  version: 1.0.0",
    "  license: {name: Apache-2.0, identifier: Apache-2.0, url: 'https://example.com/license'}",
    EMPTY_PATHS,
)

private val twoLicenses: List<String> = listOf("openapi: 3.1.0") + licensed

private val deep: List<String> = listOf(
    "openapi: 3.1.0",
    "info: {title: Petstore, version: 1.0.0}",
    "paths: {}",
    "x-deep: {a: {b: {c: {d: 1}}}}",
)

private fun loadedOf(lines: List<String>): LoadedDocument {
    val text = yamlOf(lines)
    return LoadedDocument(text, nodeOfLines(lines), SourceDocument(petstoreUri, text.length, ByteString()))
}

private suspend fun checked(lines: List<String>, limits: ReadLimits, reported: JsonPointer?): List<Diagnostic> {
    val findings = Findings(SeverityPolicy.Default, MAXIMUM_DIAGNOSTICS)
    val document = loadedOf(lines)
    if (reported != null) {
        val location = document.locate(pointer = reported, range = null)
        findings.report(code = DiagnosticCodes.FIELD_UNKNOWN, message = KNOWN, location = location)
    }
    StructureValidation(findings, document, VersionFamily.V31).check(OfficialSchemas(limits))
    return findings.sorted(mapOf(petstoreUri to 0))
}

private fun shallow(): ReadLimits {
    val builder = ReadLimitsBuilder()
    builder.maximumDepth = SHALLOW
    return builder.build()
}

class StructureValidationTest : FunSpec({
    context("the check against the official schema") {
        test("finds what the reader has no rule for, at the place it is written") {
            val reading = reading(twoLicenses)
            val finding = reading.diagnostics.single()
            val location = finding.location.shouldNotBeNull()

            finding.code shouldBe DiagnosticCodes.STRUCTURE_SCHEMA
            finding.message shouldStartWith "The schema of OpenAPI 3.1 says: "
            location.pointer.text shouldBe LICENSE_POINTER
            finding.snippet.shouldNotBeNull()
        }

        test("checks OpenAPI 3.2 against its own schema") {
            val finding = reading(listOf("openapi: 3.2.0") + licensed).diagnostics.single()
            val location = finding.location.shouldNotBeNull()

            finding.code shouldBe DiagnosticCodes.STRUCTURE_SCHEMA
            finding.message shouldStartWith "The schema of OpenAPI 3.2 says: "
            location.pointer.text shouldBe LICENSE_POINTER
        }

        test("checks OpenAPI 3.0 against its schema of draft-04") {
            val reading = reading30("components:", "  schemas:", "    Pet: {type: object, required: [name, name]}")

            reading.codes() shouldBe listOf(DiagnosticCodes.STRUCTURE_SCHEMA)
            reading.diagnostics.single().message shouldStartWith "The schema of OpenAPI 3.0 says: "
        }

        test("is silent about a document the reader accepts and the schema accepts") {
            reading31().diagnostics.shouldBeEmpty()
            reading30().diagnostics.shouldBeEmpty()
            reading32().diagnostics.shouldBeEmpty()
        }

        test("says nothing about a place the reader already reported") {
            val unknown = reading31("inof: {}")
            val key = reading31("components:", "  securitySchemes:", "    key: {type: apiKey, name: X-Key, in: body}")

            unknown.codes() shouldBe listOf(DiagnosticCodes.FIELD_UNKNOWN)
            key.codes() shouldBe listOf(DiagnosticCodes.SECURITY_LOCATION)
        }

        test("says nothing beside, above or below a place the reader already reported") {
            val places = listOf("/info/license/name", "/info", "/info/version", "/info/title")
            places.forEach { place ->
                val diagnostics = checked(twoLicenses, ReadLimits.Default, JsonPointer.parse(place))

                diagnostics.map(Diagnostic::code) shouldBe listOf(DiagnosticCodes.FIELD_UNKNOWN)
            }
        }

        test("still reports a place away from what the reader reported") {
            val diagnostics = checked(twoLicenses, ReadLimits.Default, JsonPointer.parse("/paths"))

            val codes = diagnostics.map(Diagnostic::code)

            codes.toSet() shouldBe setOf(DiagnosticCodes.STRUCTURE_SCHEMA, DiagnosticCodes.FIELD_UNKNOWN)
        }

        test("can be switched off") {
            val reading = reading(twoLicenses) { structure = StructureCheck.Reader }

            reading.diagnostics.shouldBeEmpty()
        }

        test("can be accepted with a reason") {
            val reading = reading(twoLicenses) {
                severities { downgrade(DiagnosticCodes.STRUCTURE_SCHEMA, ACCEPTED) }
            }

            reading.document()
            reading.diagnostics.single().isError shouldBe false
        }
    }

    context("a document beyond the limits of the reading") {
        test("is reported once instead of being checked in part") {
            val diagnostics = checked(deep, shallow(), null)
            val finding = diagnostics.single()

            finding.code shouldBe DiagnosticCodes.STRUCTURE_SCHEMA
            finding.message shouldContain "could not check the document"
        }

        test("is not reported again where the reader already reported it") {
            val diagnostics = checked(deep, shallow(), JsonPointer.parse("/x-deep/a/b"))

            diagnostics.map(Diagnostic::code) shouldBe listOf(DiagnosticCodes.FIELD_UNKNOWN)
        }
    }

    context("the official schemas of a reader") {
        test("are compiled once and reused") {
            val schemas = OfficialSchemas(ReadLimits.Default)
            VersionFamily.entries.forEach { family ->
                val first = schemas.of(family)

                schemas.of(family) shouldBeSameInstanceAs first
                first.uri shouldContain family.prefix
            }
        }
    }
})
