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

package com.davils.kapi.spec.asyncapi.structure

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.StructureCheck
import com.davils.kapi.spec.asyncapi.diagnostic.Findings
import com.davils.kapi.spec.asyncapi.fixture.apiUri
import com.davils.kapi.spec.asyncapi.fixture.codes
import com.davils.kapi.spec.asyncapi.fixture.document
import com.davils.kapi.spec.asyncapi.fixture.head
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import com.davils.kapi.spec.asyncapi.fixture.yamlOf
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.core.policy.ReadLimitsBuilder
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.io.bytestring.ByteString

private const val ACCEPTED: String = "The broker accepts it."

private const val KNOWN: String = "The reader knows this place."

private const val SHALLOW: Int = 3

private const val MAXIMUM_DIAGNOSTICS: Int = 100

private const val QOS_POINTER: String = "/servers/broker/bindings/mqtt/qos"

private val broker: String = """
    |servers:
    |  broker:
    |    host: broker.example.com
    |    protocol: mqtt
    |    bindings:
    |      mqtt:
    |        qos: 7
""".trimMargin()

private const val DEEP: String = "x-deep: {a: {b: {c: {d: 1}}}}"

private suspend fun checked(body: String, limits: ReadLimits, reported: JsonPointer?): List<Diagnostic> {
    val text = yamlOf(head + body.lines())
    val parsed = Yaml.Default.parse(text).shouldBeInstanceOf<YamlParseResult.Parsed>()
    val single = parsed.stream.single.shouldNotBeNull()
    val root = single.root
    val document = LoadedDocument(text, root, SourceDocument(apiUri, text.length, ByteString()))
    val model = readingOf(body) { structure = StructureCheck.Reader }.document()
    val findings = Findings(SeverityPolicy.Default, MAXIMUM_DIAGNOSTICS)
    reported?.let { pointer ->
        val location = document.locate(pointer = pointer, range = null)
        findings.report(code = DiagnosticCodes.FIELD_UNKNOWN, message = KNOWN, location = location)
    }
    StructureValidation(findings = findings, document = document, model = model).check(OfficialSchema(limits))
    return findings.sorted(mapOf(apiUri to 0))
}

private fun shallow(): ReadLimits {
    val builder = ReadLimitsBuilder()
    builder.maximumDepth = SHALLOW
    return builder.build()
}

class StructureValidationTest : FunSpec({
    context("the check against the official schema") {
        test("finds what the reader has no rule for, at the place it is written") {
            val reading = readingOf(broker)
            val finding = reading.diagnostics.single()
            val location = finding.location.shouldNotBeNull()

            finding.code shouldBe DiagnosticCodes.STRUCTURE_SCHEMA
            finding.message shouldStartWith "The schema of AsyncAPI 3.0 says: "
            location.pointer.text shouldBe QOS_POINTER
            finding.snippet.shouldNotBeNull()
        }

        test("is silent about a document the reader and the schema accept") {
            readingOf("channels:\n  chat:\n    address: chat").diagnostics.shouldBeEmpty()
        }

        test("leaves references, boolean schemas and schemas of other formats to the reader") {
            val reading = readingOf(
                """
                    |channels:
                    |  room:
                    |    address: room
                    |    bindings:
                    |      ws:
                    |        headers:
                    |          ${'$'}ref: '#/components/schemas/Headers'
                    |components:
                    |  schemas:
                    |    Headers:
                    |      type: object
                    |      properties:
                    |        token:
                    |          type: string
                    |    Anything: true
                    |    Avro:
                    |      schemaFormat: application/vnd.apache.avro;version=1.9.0
                    |      schema: {}
                """.trimMargin(),
            )

            reading.diagnostics.shouldBeEmpty()
        }

        test("says nothing about a place the reader already reported") {
            readingOf("inof: {}").codes() shouldBe listOf(DiagnosticCodes.FIELD_UNKNOWN)
        }

        test("says nothing beside, above or below a place the reader already reported") {
            val places = listOf(QOS_POINTER, "/servers/broker/bindings", "/servers/broker/bindings/mqtt/clientId")
            places.forEach { place ->
                val diagnostics = checked(broker, ReadLimits.Default, JsonPointer.parse(place))

                diagnostics.map(Diagnostic::code) shouldBe listOf(DiagnosticCodes.FIELD_UNKNOWN)
            }
        }

        test("can be switched off") {
            readingOf(broker) { structure = StructureCheck.Reader }.diagnostics.shouldBeEmpty()
        }

        test("can be accepted with a reason") {
            val reading = readingOf(broker) {
                severities { downgrade(DiagnosticCodes.STRUCTURE_SCHEMA, ACCEPTED) }
            }

            reading.document()
            reading.diagnostics.single().isError shouldBe false
        }
    }

    context("a document beyond the limits of the reading") {
        test("is reported once instead of being checked in part") {
            val finding = checked(DEEP, shallow(), null).single()

            finding.code shouldBe DiagnosticCodes.STRUCTURE_SCHEMA
            finding.message shouldContain "could not check the document"
        }

        test("is not reported again where the reader already reported it") {
            val diagnostics = checked(DEEP, shallow(), JsonPointer.parse("/x-deep/a/b"))

            diagnostics.map(Diagnostic::code) shouldBe listOf(DiagnosticCodes.FIELD_UNKNOWN)
        }
    }

    context("the official schema of a reader") {
        test("is compiled once and reused") {
            val schema = OfficialSchema(ReadLimits.Default)
            val first = schema.get()

            schema.get() shouldBeSameInstanceAs first
            first.uri shouldContain "3.0.0"
        }
    }
})
