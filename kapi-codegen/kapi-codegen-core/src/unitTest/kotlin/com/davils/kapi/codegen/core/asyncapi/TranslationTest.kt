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

package com.davils.kapi.codegen.core.asyncapi

import com.davils.kapi.codegen.core.asyncapi.schema.JsonText
import com.davils.kapi.codegen.core.asyncapi.schema.SchemaTranslation
import com.davils.kapi.codegen.core.pipeline.Findings
import com.davils.kapi.spec.asyncapi.model.schema.SchemaFormat
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val WIDE_CONTROL: Int = 0x85

private const val LEFT_TO_RIGHT: Int = 0x200E

private const val TYPE: String = "type"

private const val REF: String = "\$ref"

private const val QUOTE: String = "\""

private const val BACKSLASH: String = "\\"

private const val KIND: String = "kind"

private const val STRING: String = "string"

private const val DISCRIMINATOR: String = "discriminator"

private const val ODD: String = "odd"

private const val MAXIMUM: Long = 9

private fun text(value: String): Value = Value.Text(value)

private fun number(value: Long): Value = Value.Number(NumberValue.of(value))

private fun mapping(vararg entries: Pair<String, Value>): Value.Mapping = Value.Mapping(entries.toMap())

private fun sequence(vararg items: Value): Value.Sequence = Value.Sequence(items.toList())

private fun translated(format: SchemaFormat, value: Value, report: Findings = Findings()): Value {
    val translation = SchemaTranslation(report, format) { reference -> "#/components/schemas/" + reference.value }
    return translation.schema(value)
}

class TranslationTest : FunSpec({
    context("a schema of OpenAPI 3.0") {
        test("is nullable as a type with null and an enumeration with null") {
            val schema = mapping(
                TYPE to text(STRING),
                "nullable" to Value.Flag(true),
                "enum" to sequence(text("a")),
            )

            translated(SchemaFormat.OpenApi, schema) shouldBe mapping(
                TYPE to sequence(text(STRING), text("null")),
                "enum" to sequence(text("a"), Value.Null()),
            )
        }

        test("has exclusive bounds as numbers and drops a bound that is not exclusive") {
            val schema = mapping(
                "minimum" to number(1),
                "exclusiveMinimum" to Value.Flag(true),
                "maximum" to number(MAXIMUM),
                "exclusiveMaximum" to Value.Flag(false),
            )

            translated(SchemaFormat.OpenApi, schema) shouldBe mapping(
                "maximum" to number(MAXIMUM),
                "exclusiveMinimum" to number(1),
            )
        }

        test("keeps a nullable enumeration that holds null once") {
            val schema = mapping("nullable" to Value.Flag(true), "enum" to sequence(Value.Null()))

            translated(SchemaFormat.OpenApi, schema) shouldBe mapping("enum" to sequence(Value.Null()))
        }
    }

    context("a schema of JSON Schema draft-07") {
        test("splits dependencies into required properties and schemas") {
            val schema = mapping(
                "dependencies" to mapping("a" to sequence(text("b")), "c" to mapping(TYPE to text("object"))),
            )

            translated(SchemaFormat.JsonSchema, schema) shouldBe mapping(
                "dependentRequired" to mapping("a" to sequence(text("b"))),
                "dependentSchemas" to mapping("c" to mapping(TYPE to text("object"))),
            )
        }

        test("drops what only addresses its definitions and keeps the extensions") {
            val schema = mapping(
                "\$schema" to text("http://json-schema.org/draft-07/schema#"),
                "\$id" to text("urn:x"),
                "definitions" to mapping("A" to mapping()),
                "x-kapi-name" to text("Named"),
            )

            translated(SchemaFormat.JsonSchema, schema) shouldBe mapping("x-kapi-name" to text("Named"))
        }

        test("refuses items as a list and additional items") {
            val report = Findings()
            val schema = mapping("items" to sequence(mapping()), "additionalItems" to Value.Flag(false))
            translated(SchemaFormat.JsonSchema, schema, report)

            val codes = report.sorted().map { diagnostic -> diagnostic.code }

            codes shouldBe listOf(GenerationCodes.SCHEMA_UNSUPPORTED, GenerationCodes.SCHEMA_UNSUPPORTED)
        }

        test("passes keywords that hold no schema as they are") {
            val schema = mapping(
                "allOf" to text(ODD),
                "properties" to text(ODD),
                "dependencies" to text(ODD),
            )

            translated(SchemaFormat.AsyncApi, schema) shouldBe mapping(
                "allOf" to text(ODD),
                "properties" to text(ODD),
            )
        }
    }

    context("a schema of AsyncAPI") {
        test("has a discriminator of OpenAPI and rewrites every reference it holds") {
            val schema = mapping(
                DISCRIMINATOR to text(KIND),
                "oneOf" to sequence(mapping(REF to text("A"))),
                "not" to Value.Flag(true),
            )

            translated(SchemaFormat.AsyncApi, schema) shouldBe mapping(
                DISCRIMINATOR to mapping("propertyName" to text(KIND)),
                "oneOf" to sequence(mapping(REF to text("#/components/schemas/A"))),
                "not" to Value.Flag(true),
            )
        }

        test("keeps an object discriminator as it is") {
            val discriminator = mapping("propertyName" to text(KIND))

            translated(SchemaFormat.AsyncApi, mapping(DISCRIMINATOR to discriminator)) shouldBe
                mapping(DISCRIMINATOR to discriminator)
        }

        test("ignores the keywords beside a reference with a warning, but not its annotations") {
            val report = Findings()
            val schema = mapping(REF to text("A"), "description" to text("d"), TYPE to text(STRING))

            translated(SchemaFormat.AsyncApi, schema, report) shouldBe mapping(REF to text("#/components/schemas/A"))
            report.sorted().map { diagnostic -> diagnostic.code } shouldBe listOf(
                GenerationCodes.SCHEMA_REFERENCE_SIBLING,
            )
        }
    }

    context("the text of the synthetic document") {
        test("escapes quotes, backslashes, controls and invisible characters") {
            val title = listOf("A", QUOTE + "b" + QUOTE, BACKSLASH, "\u0001", "\u202E").joinToString(separator = " ")
            val document = JsonText.document(title, emptyList())

            document shouldContain """"title":"A \"b\" \\ \u0001 \u202e""""
        }

        test("writes every kind of value") {
            val value = mapping("a" to sequence(Value.Null(), Value.Flag(false), number(2)), "b" to mapping())

            JsonText.text(value) shouldBe """{"a":[null,false,2],"b":{}}"""
        }

        test("escapes wide controls and invisible characters in a text") {
            val text = "a" + Char(WIDE_CONTROL) + "b" + Char(LEFT_TO_RIGHT) + "c"

            JsonText.text(Value.Text(text)) shouldBe """"a\u0085b\u200ec""""
        }
    }
})
