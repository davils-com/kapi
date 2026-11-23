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

package com.davils.kapi.spec.openapi.upgrade

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kapi.spec.openapi.upgrade.fixture.nodeOf
import com.davils.kapi.spec.openapi.upgrade.fixture.nodeOfLines
import com.davils.kapi.spec.openapi.upgrade.fixture.upgraded
import com.davils.kapi.spec.openapi.upgrade.fixture.upgradedOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val SLOT: String = "<schema>"

private const val STRING: String = "type: string"

private const val NULLABLE: String = "x-nullable: true"

private const val NULLABLE_STRING: String = "type: [string, 'null']"

private const val FILE: String = "{type: file}"

private const val BINARY: String = "{type: string, format: binary}"

private val keywords: List<String> = listOf(
    "properties: {a: $SLOT}",
    "additionalProperties: $SLOT",
    "items: $SLOT",
    "allOf: [$SLOT]",
    "anyOf: [$SLOT]",
    "oneOf: [$SLOT]",
    "not: $SLOT",
)

private suspend fun schemaOf(vararg lines: String): YamlNode? = schemaOfLines(lines.asList())

private suspend fun schemaOfLines(lines: List<String>): YamlNode? {
    val indented = lines.map { line -> "    $line" }
    val root = upgradedOf(listOf("definitions:", "  Subject:") + indented).root
    return root["components"]["schemas"]["Subject"]
}

class SchemaUpgradeTest : FunSpec({
    context("a reference") {
        test("points into the schemas of the components in place of the definitions") {
            schemaOf("${'$'}ref: '#/definitions/Pet'") shouldBe nodeOf("${'$'}ref: '#/components/schemas/Pet'")
        }

        test("keeps the document it points into") {
            schemaOf("${'$'}ref: 'other.yaml#/definitions/Pet'") shouldBe
                nodeOf("${'$'}ref: 'other.yaml#/components/schemas/Pet'")
        }

        test("stays as written when it names no definition") {
            listOf("pet.yaml", "'#/info'", "{}").forEach { reference ->
                schemaOf("${'$'}ref: $reference") shouldBe nodeOf("${'$'}ref: $reference")
            }
        }
    }

    context("the nested schemas") {
        test("are upgraded in properties, additional properties, items, alternatives and negations") {
            val written = keywords.map { keyword -> keyword.replace(oldValue = SLOT, newValue = FILE) }
            val expected = keywords.map { keyword -> keyword.replace(oldValue = SLOT, newValue = BINARY) }

            schemaOfLines(written) shouldBe nodeOfLines(expected)
        }

        test("are upgraded when items list a schema per position") {
            schemaOf("items: [$FILE]") shouldBe nodeOf("items: [$BINARY]")
        }

        test("stay as written when they are no schema") {
            schemaOf("additionalProperties: true", "allOf: none") shouldBe
                nodeOf("additionalProperties: true", "allOf: none")
        }

        test("stay as written when the definition is no object") {
            upgraded("definitions: {Subject: true}").root["components"]["schemas"]["Subject"] shouldBe nodeOf("true")
        }

        test("keep a key that is no scalar") {
            schemaOf("? [a]", ": b") shouldBe nodeOf("? [a]", ": b")
        }
    }

    context("x-nullable") {
        test("adds null to the type of a schema") {
            schemaOf(STRING, NULLABLE) shouldBe nodeOf(NULLABLE_STRING)
        }

        test("adds null to the values of an enumeration") {
            schemaOf(STRING, "enum: ['', a]", NULLABLE) shouldBe nodeOf(NULLABLE_STRING, "enum: ['', a, null]")
        }

        test("keeps an enumeration that already admits null or is no list") {
            listOf("enum: [a, null]", "enum: a").forEach { enumeration ->
                schemaOf(STRING, enumeration, NULLABLE) shouldBe nodeOf(NULLABLE_STRING, enumeration)
            }
        }

        test("wraps a reference in an alternative with null and keeps its annotations outside") {
            schemaOf("${'$'}ref: '#/definitions/Pet'", "description: The owner", "x-go-name: Owner", NULLABLE) shouldBe
                nodeOf(
                    "description: The owner",
                    "x-go-name: Owner",
                    "anyOf: [{${'$'}ref: '#/components/schemas/Pet'}, {type: 'null'}]",
                )
        }

        test("wraps a schema without a type in an alternative with null") {
            schemaOf("additionalProperties: {$STRING}", NULLABLE) shouldBe
                nodeOf("anyOf: [{additionalProperties: {$STRING}}, {type: 'null'}]")
        }

        test("only drops itself from a schema that holds nothing but annotations") {
            schemaOf("description: Anything", NULLABLE) shouldBe nodeOf("description: Anything")
        }

        test("is dropped when it is false") {
            schemaOf(STRING, "x-nullable: false") shouldBe nodeOf(STRING)
        }
    }

    context("the keywords of Swagger") {
        test("turn a file into binary text unless a format is given") {
            schemaOf("type: file") shouldBe nodeOf(STRING, "format: binary")
            schemaOf("type: file", "format: byte") shouldBe nodeOf(STRING, "format: byte")
        }

        test("turn a discriminator into an object with its property") {
            val upgraded = "discriminator: {propertyName: kind}"

            schemaOf("discriminator: kind") shouldBe nodeOf(upgraded)
            schemaOf(upgraded) shouldBe nodeOf(upgraded)
        }

        test("turn exclusive flags into exclusive bounds") {
            schemaOf("maximum: 10", "exclusiveMaximum: true", "minimum: 1", "exclusiveMinimum: false") shouldBe
                nodeOf("exclusiveMaximum: 10", "minimum: 1")
        }

        test("drop an exclusive flag without a bound and keep a numeric one") {
            schemaOf("exclusiveMaximum: true", "exclusiveMinimum: 2") shouldBe nodeOf("exclusiveMinimum: 2")
        }
    }
})
