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

package com.davils.kapi.codegen.core.model

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.generatedOf
import com.davils.kapi.codegen.core.fixture.generationOf
import com.davils.kapi.codegen.core.fixture.generator
import com.davils.kapi.codegen.core.fixture.kotlinOf
import com.davils.kapi.codegen.core.fixture.partialOf
import com.davils.kapi.codegen.core.fixture.pet
import com.davils.kapi.codegen.core.fixture.read
import com.davils.kapi.codegen.core.fixture.readWith
import com.davils.kapi.codegen.core.fixture.refusedOf
import com.davils.kapi.codegen.core.fixture.schemas
import com.davils.kapi.codegen.core.options.Visibility
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val STATUS: String = "Status: {type: string, enum: [open, closed]}"

private const val STRING: String = "a: {type: string}"

private const val LINE: String = "\n"

private const val TEXT: String = "public val a: String? = null"

private const val ANY: String = "public val a: JsonElement? = null"

private val types: Map<String, String> = mapOf(
    STRING to TEXT,
    "a: {type: integer}" to "public val a: Long? = null",
    "a: {type: integer, format: int32}" to "public val a: Int? = null",
    "a: {type: number}" to "public val a: Double? = null",
    "a: {type: number, format: float}" to "public val a: Float? = null",
    "a: {type: boolean}" to "public val a: Boolean? = null",
    "a: {type: string, format: date-time}" to "public val a: Instant? = null",
    "a: {type: string, format: uuid}" to TEXT,
    "a: {}" to ANY,
    "a: true" to ANY,
    "a: {anyOf: [true, {type: 'null'}]}" to ANY,
    "a: {\$ref: '#/components/schemas/Anything'}" to ANY,
    "a: {type: string, x-kapi-ignore: false, x-kapi-sensitive: false}" to TEXT,
    "a: {type: 'null'}" to ANY,
    "a: {type: object}" to "public val a: Map<String, JsonElement>? = null",
    "a: {additionalProperties: {type: integer}}" to "public val a: Map<String, Long>? = null",
    "a: {type: array}" to "public val a: List<JsonElement>? = null",
    "a: {type: array, items: {type: [string, 'null']}}" to "public val a: List<String?>? = null",
    "a: {type: string, x-kapi-type: kotlin.uuid.Uuid}" to "public val a: Uuid? = null",
    "a: {type: string, x-kapi-type: money}" to "public val a: money? = null",
    "a: {x-kapi-type: {type: com.example.Money, serializer: com.example.MoneySerializer}}" to
        "public val a: @Serializable(with = MoneySerializer::class) Money? = null",
    "a: {type: string, x-kapi-name: label}" to "public val label: String? = null",
    "a: {\$ref: '#/components/schemas/Status'}" to "public val a: PetstoreStatus? = null",
    "a: {\$ref: '#/components/schemas/Id'}" to TEXT,
    "a: {\$ref: '#/components/schemas/Ids'}" to "public val a: List<String>? = null",
    "a: {\$ref: '#/components/schemas/Pet'}" to "public val a: PetstorePet? = null",
    "a: {type: object, properties: {b: {type: string}}}" to "public val a: PetstorePetA? = null",
    "a: {type: array, items: {enum: [x, y]}}" to "public val a: List<PetstorePetAItem>? = null",
    "a: {type: object, additionalProperties: {type: object, properties: {b: {type: string}}}}" to
        "public val a: Map<String, PetstorePetAValue>? = null",
)

private val shared: List<String> = listOf(
    STATUS,
    "Id: {\$ref: '#/components/schemas/Text'}",
    "Text: {type: string}",
    "Ids: {type: array, items: {type: string}}",
    "Anything: true",
)

private val unsupported: Map<String, String> = mapOf(
    "allOf with a member that is no object of this document" to
        "a: {allOf: [{type: string}, {type: object, properties: {b: {type: string}}}]}",
    "prefixItems" to "a: {type: array, prefixItems: [{type: string}]}",
    "patternProperties" to "a: {type: object, patternProperties: {'^x': {type: string}}}",
    "additionalProperties with a schema next to properties" to
        "a: {type: object, properties: {b: {type: string}}, additionalProperties: {type: string}}",
    "more than one type" to "a: {type: [string, integer]}",
    "a discriminator on a schema that nothing extends or chooses from" to
        "a: {type: object, discriminator: {propertyName: kind}}",
    "\$dynamicRef" to "a: {\$dynamicRef: '#node'}",
    "a reference next to properties" to "a: {\$ref: '#/components/schemas/Pet', properties: {b: {type: string}}}",
    "a schema that accepts nothing" to "a: {type: array, items: false}",
)

private val unchecked: Map<String, String> = mapOf(
    "not" to "a: {not: {type: string}}",
    "if" to "a: {if: {type: string}}",
    "then" to "a: {then: {type: string}}",
    "else" to "a: {else: {type: string}}",
    "contains" to "a: {type: array, contains: {type: string}}",
    "propertyNames" to "a: {type: object, propertyNames: {maxLength: 3}}",
    "dependentRequired" to "a: {type: object, dependentRequired: {b: [c]}}",
    "dependentSchemas" to "a: {type: object, dependentSchemas: {b: {type: object}}}",
    "unevaluatedItems" to "a: {type: array, unevaluatedItems: false}",
    "unevaluatedProperties" to "a: {type: object, unevaluatedProperties: false}",
)

class ModelPlannerTest : FunSpec({
    context("the type of a property") {
        types.forEach { (property, declaration) ->
            test("is written as '$declaration' for $property") {
                kotlinOf(pet(property) + shared) shouldContain declaration
            }
        }

        test("keeps the name of the document as its serial name") {
            val kotlin = kotlinOf(pet("first-name: {type: string}"))

            kotlin shouldContain """@SerialName("first-name")"""
            kotlin shouldContain "public val firstName: String? = null"
        }

        test("has no default when the property is required, even when it may be null") {
            val kotlin = kotlinOf(pet(STRING, "b: {type: [integer, 'null']}", required = "a, b"))

            kotlin shouldContain "public val a: String,"
            kotlin shouldContain "public val b: Long?,"
        }

        test("may be null when one of two alternatives is null") {
            val any = "a: {anyOf: [{\$ref: '#/components/schemas/Status'}, {type: 'null'}]}"
            val one = "b: {oneOf: [{type: 'null'}, {type: integer}]}"
            val kotlin = kotlinOf(pet(any, one, required = "a, b") + shared)

            kotlin shouldContain "public val a: PetstoreStatus?,"
            kotlin shouldContain "public val b: Long?,"
        }

        test("may be null when its enumeration lists null") {
            kotlinOf(pet("a: {enum: [x, null]}", required = "a")) shouldContain "public val a: PetstorePetA?,"
        }
    }

    context("a property that is ignored") {
        test("is left out") {
            val kotlin = kotlinOf(pet(STRING, "b: {type: string, x-kapi-ignore: true}"))

            kotlin shouldNotContain "val b"
        }

        test("is left out as well when its schema accepts nothing") {
            kotlinOf(pet(STRING, "b: false")) shouldNotContain "val b"
        }

        test("is refused when its schema accepts nothing and it is required") {
            refusedOf(schemas(pet("b: false", required = "b"))).codes() shouldBe
                listOf(CodegenCodes.PROPERTY_IGNORED)
        }

        test("leaves a class without properties, equal to every other one and without copy") {
            val kotlin = kotlinOf(pet("a: {type: string, x-kapi-ignore: true}"))

            kotlin shouldContain "public class PetstorePet() :"
            kotlin shouldContain "override fun equals(other: Any?): Boolean = other is PetstorePet\n"
            kotlin shouldContain "override fun hashCode(): Int = 0"
            kotlin shouldNotContain "fun copy("
        }

        test("is refused when the property is required") {
            val refused = refusedOf(schemas(pet("a: {type: string, x-kapi-ignore: true}", required = "a")))

            refused.codes() shouldBe listOf(CodegenCodes.PROPERTY_IGNORED)
        }
    }

    context("two properties with one name") {
        test("are a collision") {
            val refused = refusedOf(schemas(pet("first_name: {type: string}", "firstName: {type: string}")))

            refused.codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
        }

        test("are a collision of the types they declare as well") {
            val inline = "{type: object, properties: {x: {type: string}}}"
            val refused = refusedOf(schemas(pet("a: $inline") + listOf("PetA: $inline")))

            refused.codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
        }
    }

    context("what the generator has no Kotlin for") {
        unsupported.forEach { (keyword, property) ->
            test("is refused for $keyword") {
                val refused = refusedOf(schemas(pet(property)))

                refused.codes() shouldBe listOf(CodegenCodes.SCHEMA_UNSUPPORTED)
                refused.diagnostics.single().message shouldBe "The generator has no Kotlin for $keyword yet"
            }
        }

        test("is refused on a component that is no model as well") {
            refusedOf(schemas(listOf("Name: {type: [string, integer]}"))).codes() shouldBe
                listOf(CodegenCodes.SCHEMA_UNSUPPORTED)
        }

        test("is refused on a component that is a model as well") {
            val lines = pet(STRING) + listOf("  patternProperties: {'^x': {type: string}}")

            refusedOf(schemas(lines)).codes() shouldBe listOf(CodegenCodes.SCHEMA_UNSUPPORTED)
        }
    }

    context("what validate() does not check") {
        unchecked.forEach { (keyword, property) ->
            test("is a warning for $keyword and no reason to refuse") {
                val diagnostic = generatedOf(schemas(pet(property))).diagnostics.single()

                diagnostic.code shouldBe CodegenCodes.SCHEMA_UNCHECKED
                diagnostic.isError shouldBe false
                diagnostic.message shouldBe "The generated validate() does not check $keyword"
            }
        }
    }

    context("a reference") {
        test("is refused when it points into a schema") {
            val refused = refusedOf(schemas(pet(STRING, "b: {\$ref: '#/components/schemas/Pet/properties/a'}")))

            refused.codes() shouldBe listOf(CodegenCodes.REFERENCE_UNSUPPORTED)
            refused.diagnostics.single().message shouldContain "points at something other than a schema"
        }

        test("is refused when it points into another document") {
            val shared = DocumentUri("file:/work/specs/shared.yaml")
            val other = listOf("components:", "  schemas:", "    Pet: {type: string}").joinToString("\n")
            val lines = schemas(pet("a: {\$ref: 'shared.yaml#/components/schemas/Pet'}"))
            val reading = readWith(mapOf(shared to other), lines)
            val generation = generator().generate(reading.document)

            generation.codes() shouldBe listOf(CodegenCodes.REFERENCE_UNSUPPORTED)
        }

        test("is refused when it leads back to itself") {
            val lines = listOf("A: {\$ref: '#/components/schemas/B'}", "B: {\$ref: '#/components/schemas/A'}")
            val partial = partialOf(schemas(lines))
            val generation = generator().generate(partial)

            generation.codes().distinct() shouldBe listOf(CodegenCodes.REFERENCE_UNSUPPORTED)
            generation.diagnostics.first().message shouldContain "leads back to itself"
        }
    }

    context("the visibility") {
        test("is internal on every declaration when the options say so") {
            val reading = read(schemas(pet(STRING, "b: {enum: [x]}")))
            val generation = generator { visibility = Visibility.Internal }.generate(reading.document)
            val files = generation.shouldBeInstanceOf<Generation.Generated>().files
            val kotlin = files.joinToString("\n") { file -> file.content }

            kotlin shouldContain "internal class PetstorePet("
            kotlin shouldContain "internal val a: String? = null"
            kotlin shouldContain "internal value class PetstorePetB("
            kotlin shouldNotContain "public "
        }
    }

    context("the KDoc") {
        test("describes a model, its properties and an enum with their title and description") {
            val lines = listOf(
                "Pet: {title: Pet, description: 'A pet */ /*', type: object, properties: " +
                    """{a: {type: string, description: 'The @name'}, b: {description: "Kind \u202E", enum: [x]}}}""",
            )

            val model = listOf("/**", " * Pet", " *", " * A pet *&#47; &#47;*", " */")
            val property = listOf("    /**", "     * The &#64;name", "     */")

            kotlinOf(lines) shouldContain model.joinToString(LINE)
            kotlinOf(lines) shouldContain property.joinToString(LINE)
            kotlinOf(lines, "PetstorePetB") shouldContain """ * Kind \u202e"""
        }

        test("is left out where the schema describes nothing, so only copy has one") {
            val kotlin = kotlinOf(pet(STRING))

            kotlin.split("/**").size shouldBe 2
        }
    }

    context("a document without schemas") {
        test("has no models") {
            generationOf(listOf("paths: {}")).diagnostics.shouldBeEmpty()
        }
    }
})
