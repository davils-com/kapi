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

package com.davils.kapi.spec.asyncapi.compatibility

import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

private const val DEPTH: Int = 40

private const val SHALLOW: Int = 10

private const val VALUES: Int = 20_000

private const val PAYLOAD: String = "      payload: {type: string}"

private const val INCOMPLETE: String = "incomplete"

private const val INLINE: String = "      payload: "

private val everything: String = """
    |      payload:
    |        type: [object, 'null']
    |        format: record
    |        pattern: '^a'
    |        enum: [a, b]
    |        const: a
    |        minLength: 1
    |        maxLength: 9
    |        exclusiveMinimum: 1
    |        exclusiveMaximum: 9
    |        multipleOf: 2
    |        minItems: 1
    |        maxItems: 9
    |        uniqueItems: true
    |        minProperties: 1
    |        maxProperties: 9
    |        required: [a]
    |        properties: {a: {type: string, readOnly: true, writeOnly: false}}
    |        additionalProperties: false
    |        items: {type: string}
    |        allOf: [{type: object}]
    |        anyOf: [{type: object}]
    |        oneOf: [{type: object}]
    |        discriminator: kind
    |        deprecated: true
""".trimMargin()

private val AVRO: String = """
    |      payload:
    |        schemaFormat: application/vnd.apache.avro;version=1.9.0
    |        schema: int
""".trimMargin()

private val openApiFormat: String = """
    |      payload:
    |        schemaFormat: application/vnd.oai.openapi;version=3.0.0
    |        schema: {type: string, nullable: true, minimum: 1, exclusiveMinimum: true, discriminator: {propertyName: kind}}
""".trimMargin()

private fun left(payload: String): String = base.edited(PAYLOAD to payload).edited(
    "      - ${'$'}ref: '#/channels/chat/messages/said'" to "      - ${'$'}ref: '#/channels/chat/messages/left'",
)

private fun nested(depth: Int, leaf: String): String = "{type: array, items: ".repeat(depth) + leaf + "}".repeat(depth)

private fun enumOf(count: Int, skip: Int): String =
    (skip until count).joinToString(prefix = "{type: string, enum: [", postfix = "]}") { index -> "v$index" }

class SchemaRulesTest : FunSpec({
    context("the payload of a message") {
        test("is compared keyword by keyword") {
            val rules = (left(everything) to left(PAYLOAD)).rules()

            rules shouldContainAll listOf("type-changed", "format", "pattern", "enum", "const", "min-length", "maximum")
            rules shouldContainAll listOf("multiple-of", "unique-items", "max-properties", "property-removed")
            rules shouldContainAll listOf("additional-properties", "items", "all-of", "discriminator")
            (left(everything) to left(everything)).rules().shouldBeEmpty()
        }

        test("in the format of OpenAPI tells null and the bounds of OpenAPI 3.0") {
            val openApi = left(openApiFormat)
            val notNull = openApi.edited("nullable: true" to "nullable: false")
            val inclusive = openApi.edited("exclusiveMinimum: true" to "exclusiveMinimum: false")

            (openApi to notNull).rules() shouldBe listOf("nullable-removed")
            (openApi to inclusive).rules() shouldBe listOf("minimum")
        }

        test("in a format the comparison does not read is compared as written") {
            val avro = left(AVRO)
            val changed = avro.edited("schema: int" to "schema: long")

            (avro to changed).rules() shouldBe listOf("opaque")
            (avro to avro).rules().shouldBeEmpty()
        }

        test("follows references inside schemas and reports those that lead nowhere") {
            val referenced = left("      payload: {${'$'}ref: '#/components/schemas/Text'}") +
                "\n  schemas:\n    Text: {type: string, maxLength: 9}"
            val shorter = referenced.edited("maxLength: 9" to "maxLength: 5")
            val nowhere = left("      payload: {properties: {a: {${'$'}ref: '#/components/schemas/Missing'}}}")

            (referenced to shorter).rules() shouldBe listOf("max-length")
            (nowhere to nowhere).rules() shouldBe listOf(INCOMPLETE)
        }

        test("may be a boolean schema or a value that is no schema") {
            val accepting = left("      payload: true")
            val refusing = left("      payload: false")

            (accepting to refusing).rules() shouldBe listOf("accepts")
            (left("      payload: {items: 5}") to left("      payload: {items: 6}")).rules() shouldBe listOf("opaque")
            (left("      payload: {items: [true]}") to left("      payload: {items: [false]}")).rules().shouldBeEmpty()
        }
    }

    context("a hostile pair of payloads") {
        test("of deep schemas is compared within the depth of the limits") {
            val before = left(INLINE + nested(DEPTH, "{type: string}"))
            val after = left(INLINE + nested(DEPTH, "{type: integer}"))

            (before to after).rules() shouldBe listOf("type-changed")
            (before to after).rules { limits { maximumDepth = SHALLOW } } shouldBe listOf(INCOMPLETE)
        }

        test("of cyclic schemas ends") {
            val cyclic = left("      payload: {${'$'}ref: '#/components/schemas/Node'}") +
                "\n  schemas:\n    Node: {type: object, properties: {next: {${'$'}ref: '#/components/schemas/Node'}}}"
            val changed = cyclic.edited("type: object" to "type: [object, 'null']")

            (cyclic to changed).rules() shouldBe listOf("nullable-added")
        }

        test("of huge enums is compared within the steps of the limits") {
            val before = left(INLINE + enumOf(count = VALUES, skip = 0))
            val after = left(INLINE + enumOf(count = VALUES, skip = 1))

            (before to after).rules() shouldBe listOf("enum-value-removed")
            (before to after).rules { limits { maximumNodes = VALUES.toLong() } } shouldBe listOf(INCOMPLETE)
        }
    }

    context("the view of a schema in a format the comparison does not read") {
        test("has no keywords and leads nowhere") {
            val view = OpaqueView(location = null, value = Value.Text("int"))
            val nothing = listOf(
                view.target,
                view.isAccepting,
                view.types,
                view.format,
                view.pattern,
                view.enumeration,
                view.constant,
                view.minLength,
                view.maxLength,
                view.minimum,
                view.maximum,
                view.multipleOf,
                view.minItems,
                view.maxItems,
                view.minProperties,
                view.maxProperties,
                view.additionalProperties,
                view.items,
                view.discriminator,
            )
            val flags = listOf(
                view.isUnresolved,
                view.isMinimumExclusive,
                view.isMaximumExclusive,
                view.hasUniqueItems,
                view.isDeprecated,
                view.isReadOnly,
                view.isWriteOnly,
            )
            val empty = listOf(view.properties.keys, view.required, view.allOf, view.anyOf, view.oneOf)

            nothing.all { value -> value == null } shouldBe true
            flags.none { flag -> flag } shouldBe true
            empty.all(Collection<Any>::isEmpty) shouldBe true
            view.opaque shouldBe Value.Text("int")
        }
    }
})
