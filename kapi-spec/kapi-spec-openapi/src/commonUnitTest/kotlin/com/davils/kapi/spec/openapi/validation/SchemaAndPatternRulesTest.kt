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
import com.davils.kapi.spec.openapi.fixture.head31
import com.davils.kapi.spec.openapi.fixture.reading
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.reading32
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val SHORT: Int = 8

private const val ACCEPTED: String = "The values are checked by the service."

private val schemas: List<String> = listOf("components:", "  schemas:")

private val constraint = DiagnosticCodes.SCHEMA_CONSTRAINT

private suspend fun codesOf(vararg lines: String) = reading31(schemas + lines).codes()

class SchemaAndPatternRulesTest : FunSpec({
    context("the constraints of a schema") {
        test("leave room for at least one value") {
            codesOf("    A: {minimum: 2, maximum: 1}") shouldBe listOf(constraint)
            codesOf("    A: {minimum: 1, exclusiveMaximum: 1}") shouldBe listOf(constraint)
            codesOf("    A: {exclusiveMinimum: 1, maximum: 1}") shouldBe listOf(constraint)
            codesOf("    A: {minLength: 2, maxLength: 1}") shouldBe listOf(constraint)
            codesOf("    A: {minItems: 2, maxItems: 1}") shouldBe listOf(constraint)
            codesOf("    A: {minProperties: 2, maxProperties: 1}") shouldBe listOf(constraint)
            codesOf("    A: {minContains: 2, maxContains: 1}") shouldBe listOf(constraint)
            codesOf("    A: {multipleOf: 0}") shouldBe listOf(constraint)
        }

        test("may meet in a single value") {
            val lines = listOf("    A: {minimum: 1, maximum: 1, minLength: 1, maxLength: 1, multipleOf: 0.5}")

            reading31(schemas + lines).diagnostics.shouldBeEmpty()
        }

        test("may bound one side only") {
            reading31(schemas + "    A: {minimum: 1, maxLength: 1}").diagnostics.shouldBeEmpty()
        }
    }

    context("the mapping of a discriminator") {
        test("names schemas of the components, by name or by pointer") {
            val known = "{cat: Cat, dog: '#/components/schemas/Cat', bird: 'other.yaml#/Bird'}"
            val pet = "    Pet: {discriminator: {propertyName: kind, mapping: $known}}"
            val lines = listOf("    Cat: {type: object}", pet)

            reading31(schemas + lines).diagnostics.shouldBeEmpty()
        }

        test("is an error when it names a schema that does not exist") {
            val unknown = "{cat: Cat, dog: '#/components/schemas/Dog'}"
            val reading = reading31(schemas + "    Pet: {discriminator: {propertyName: kind, mapping: $unknown}}")
            val code = DiagnosticCodes.SCHEMA_DISCRIMINATOR_MAPPING

            reading.codes() shouldBe listOf(code, code)
            reading.diagnostics.last().message shouldContain "maps dog to #/components/schemas/Dog"
        }

        test("names a schema of the components by default, by name or by pointer") {
            val defaults = listOf("Cat", "'#/components/schemas/Cat'", "'other.yaml#/Bird'")
            defaults.forEach { target ->
                val pet = "    Pet: {discriminator: {propertyName: kind, defaultMapping: $target}}"
                val reading = reading32(schemas + "    Cat: {type: object}" + pet)

                reading.diagnostics.shouldBeEmpty()
            }
        }

        test("is an error when it names a schema by default that does not exist") {
            listOf("Dog", "'#/components/schemas/Dog'").forEach { target ->
                val pet = "    Pet: {discriminator: {propertyName: kind, defaultMapping: $target}}"
                val reading = reading32(schemas + pet)

                reading.codes() shouldBe listOf(DiagnosticCodes.SCHEMA_DISCRIMINATOR_MAPPING)
                reading.diagnostics.single().message shouldContain "maps by default to"
            }
        }
    }

    context("schemas that are nothing but references") {
        test("must end somewhere") {
            val first = "    First: {\$ref: '#/components/schemas/Second'}"
            val second = "    Second: {\$ref: '#/components/schemas/First'}"
            val cycle = DiagnosticCodes.REFERENCE_CYCLE

            reading31(schemas + listOf(first, second)).codes() shouldBe listOf(cycle, cycle)
        }

        test("may pass through a few references on the way to a schema") {
            val first = "    First: {\$ref: '#/components/schemas/Second'}"
            val second = "    Second: {type: object, properties: {again: {\$ref: '#/components/schemas/First'}}}"

            reading31(schemas + listOf(first, second)).document()
        }
    }

    context("a pattern") {
        test("is a regular expression of ECMA-262") {
            val reading = reading31(schemas + "    A: {type: string, pattern: '[a-'}")

            reading.codes() shouldBe listOf(DiagnosticCodes.SCHEMA_PATTERN_INVALID)
            reading.diagnostics.single().message shouldContain "is no regular expression"
        }

        test("is invalid when only another dialect of regular expressions knows it") {
            listOf("(?<name>a)\\\\k<other>", "\\\\a", "a{2,1}", "(?i)a", "[\\\\w-z]").forEach { pattern ->
                val reading = reading31(schemas + "    A: {type: string, pattern: \"$pattern\"}")

                reading.codes() shouldBe listOf(DiagnosticCodes.SCHEMA_PATTERN_INVALID)
            }
        }

        test("may repeat a repeated group, because kapi never backtracks") {
            val reading = reading31(schemas + "    A: {patternProperties: {'(a+)+$': {}, '^(a|aa)*b': {}}}")

            reading.diagnostics.shouldBeEmpty()
        }

        test("warns when it is valid but kapi cannot match it in linear time") {
            val unsupported = listOf("(a)\\\\1", "a(?=b)", "(?<!a)b", "\\\\p{Emoji}", "a{100000}")
            unsupported.forEach { pattern ->
                val reading = reading31(schemas + "    A: {type: string, pattern: \"$pattern\"}")
                val finding = reading.diagnostics.single()

                finding.code shouldBe DiagnosticCodes.SCHEMA_PATTERN_UNSUPPORTED
                finding.isError shouldBe false
                finding.message shouldContain "linear time"
                reading.document()
            }
        }

        test("can be accepted when kapi cannot match it") {
            val lines = head31 + schemas + "    A: {type: string, pattern: '(a)\\1'}"
            val reading = reading(lines) {
                severities { downgrade(DiagnosticCodes.SCHEMA_PATTERN_UNSUPPORTED, ACCEPTED) }
            }

            reading.document()
        }

        test("is checked at the length a reading accepts") {
            val lines = head31 + schemas + "    A: {type: string, pattern: '^[a-z]+$'}"
            val reading = reading(lines) { limits { maximumPatternLength = SHORT } }

            reading.diagnostics.shouldBeEmpty()
        }

        test("is refused beyond the length a reading accepts") {
            val lines = head31 + schemas + "    A: {type: string, pattern: '^[a-z]{1,64}$'}"
            val reading = reading(lines) { limits { maximumPatternLength = SHORT } }

            reading.codes() shouldBe listOf(DiagnosticCodes.LIMIT_PATTERN_LENGTH)
        }
    }
})
