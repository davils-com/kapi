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

package com.davils.kapi.spec.openapi.read.schema

import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.Reading
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.partial
import com.davils.kapi.spec.openapi.fixture.reading30
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.model.schema.Bound
import com.davils.kapi.spec.openapi.model.schema.FixedSchema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.model.schema.SchemaType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val TEN: Long = 10L

private const val STRING: String = "type: string"

private val head: List<String> = listOf("components:", "  schemas:", "    Subject:")

private val ten: NumberValue = NumberValue.of(TEN)

private val one: NumberValue = NumberValue.of(1L)

private suspend fun schema30(vararg lines: String): Reading = reading30(head + lines.map { line -> "      $line" })

private suspend fun schema31(vararg lines: String): Reading = reading31(head + lines.map { line -> "      $line" })

private fun Reading.subject(): SchemaDefinition {
    val schemas = document().components.shouldNotBeNull().schemas
    return schemas.getValue("Subject").shouldBeInstanceOf<SchemaDefinition>()
}

class SchemaReadingTest : FunSpec({
    context("a schema of OpenAPI 3.0") {
        test("lists null among its types when it is nullable") {
            val subject = schema30(STRING, "nullable: true").subject()

            subject.types shouldBe setOf(SchemaType.String, SchemaType.Null)
        }

        test("makes a bound exclusive with a flag beside it") {
            val subject = schema30("type: integer", "minimum: 1", "exclusiveMinimum: true", "maximum: 10").subject()

            subject.minimum shouldBe Bound(one, isExclusive = true)
            subject.maximum shouldBe Bound(ten)
        }

        test("keeps its example") {
            schema30(STRING, "example: Rex").subject().examples shouldBe listOf(Value.Text("Rex"))
        }

        test("accepts a boolean where it says whether further properties are allowed") {
            val subject = schema30("type: object", "additionalProperties: true").subject()

            subject.additionalProperties.shouldBeInstanceOf<FixedSchema>().isAccepting shouldBe true
        }

        test("knows a fixed set of keywords and refuses every other") {
            val reading = schema30(STRING, "const: Rex")

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_UNKNOWN)
        }

        test("names a single type") {
            schema30("type: [string]").codes() shouldBe listOf(DiagnosticCodes.FIELD_TYPE)
        }
    }

    context("a schema of OpenAPI 3.1") {
        test("has a bound that is exclusive by its own keyword") {
            val subject = schema31("type: integer", "exclusiveMinimum: 1", "exclusiveMaximum: 10").subject()

            subject.minimum shouldBe Bound(one, isExclusive = true)
            subject.maximum shouldBe Bound(ten, isExclusive = true)
        }

        test("keeps the inclusive bound when it is the tighter one") {
            val subject = schema31("minimum: 1", "exclusiveMinimum: 0", "maximum: 10", "exclusiveMaximum: 11").subject()

            subject.minimum shouldBe Bound(one)
            subject.maximum shouldBe Bound(ten)
        }

        test("keeps a keyword it does not know and warns about it") {
            val reading = schema31(STRING, "flavour: sweet")

            reading.codes() shouldBe listOf(DiagnosticCodes.SCHEMA_UNKNOWN_KEYWORD)
            reading.subject().unknownKeywords shouldBe mapOf("flavour" to Value.Text("sweet"))
        }

        test("warns when it is written in the form of OpenAPI 3.0") {
            val reading = schema31(STRING, "nullable: true")

            reading.codes() shouldBe listOf(DiagnosticCodes.SCHEMA_VERSION_FORM)
            reading.diagnostics.single().message shouldContain "lists null among its types"
            reading.subject().types shouldBe setOf(SchemaType.String)
        }

        test("may be a boolean") {
            val schemas = schema31("items: true").subject()

            schemas.items.shouldBeInstanceOf<FixedSchema>().isAccepting shouldBe true
        }
    }

    context("a schema of either version") {
        test("has no type unless it names one") {
            val subject = schema31("description: Anything").subject()

            subject.types.shouldBeEmpty()
            subject.minimum shouldBe null
            subject.enumeration shouldBe null
        }

        test("is refused when it names a type that does not exist") {
            schema31("type: text").codes() shouldBe listOf(DiagnosticCodes.SCHEMA_TYPE)
            schema31("type: [string, 1]").codes() shouldBe listOf(DiagnosticCodes.SCHEMA_TYPE)
            schema31("type: []").codes() shouldBe listOf(DiagnosticCodes.SCHEMA_TYPE)
        }

        test("is refused when a count is negative or no integer") {
            schema31("minLength: -1").codes() shouldBe listOf(DiagnosticCodes.FIELD_TYPE)
            schema31("maxItems: 1.5").codes() shouldBe listOf(DiagnosticCodes.FIELD_TYPE)
            schema31("minItems: many").codes() shouldBe listOf(DiagnosticCodes.FIELD_TYPE)
        }

        test("is refused when a number is written the way JSON cannot express") {
            schema31("maximum: 0x1F").codes() shouldBe listOf(DiagnosticCodes.FIELD_TYPE)
            schema31("multipleOf: ten").codes() shouldBe listOf(DiagnosticCodes.FIELD_TYPE)
            schema31("enum: [0x1F]").codes() shouldBe listOf(DiagnosticCodes.SOURCE_NON_JSON_VALUE)
        }

        test("is refused when a required property is no string") {
            val reading = schema31("required: [name, 1]")

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_TYPE)
            reading.partial()
            reading.diagnostics.single().message shouldBe "A value of required must be a string"
        }

        test("keeps values of every kind") {
            val subject = schema31("default: {a: [1, 1.5, true, null, text]}").subject()
            val items = listOf(
                Value.Number(NumberValue.of(1L)),
                Value.Number(checkNotNull(NumberValue.parseOrNull("1.5"))),
                Value.Flag(true),
                Value.Null(),
                Value.Text("text"),
            )

            subject.default shouldBe Value.Mapping(mapOf("a" to Value.Sequence(items)))
        }

        test("is refused when it is neither an object nor a boolean") {
            val reading = reading31("components:", "  schemas:", "    Subject: text")

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_TYPE)
            reading.diagnostics.single().message shouldBe "A schema must be an object"
        }
    }
})
