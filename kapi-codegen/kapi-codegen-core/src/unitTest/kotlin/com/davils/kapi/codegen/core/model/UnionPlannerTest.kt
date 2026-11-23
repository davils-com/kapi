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

import com.davils.kapi.codegen.core.fixture.kotlinOf
import com.davils.kapi.codegen.core.fixture.refusedOf
import com.davils.kapi.codegen.core.fixture.schemas
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

private const val CAT: String = "Cat: {type: object, required: [kind], properties: {kind: {type: string}}}"

private const val DOG: String = "Dog: {type: object, required: [kind], properties: {kind: {type: string}}}"

private const val MEMBERS: String = "[{\$ref: '#/components/schemas/Cat'}, {\$ref: '#/components/schemas/Dog'}]"

private const val ANIMAL: String = "Animal: {oneOf: $MEMBERS, discriminator: {propertyName: kind}}"

private const val TAG: String = "discriminator: {propertyName: kind}"

private const val ALL: String = "All: {type: object, required: [since], properties: {since: {type: string}}}"

private const val ONE: String = "One: {type: object, required: [id], properties: {id: {type: string}}}"

private const val WATCHES: String = "[{\$ref: '#/components/schemas/All'}, {\$ref: '#/components/schemas/One'}]"

private const val UNION: String = "PetstoreAnimal"

private const val SERIALIZER: String = "PetstoreAnimalSerializer"

private suspend fun message(lines: List<String>): String = refusedOf(schemas(lines)).diagnostics.first().message

class UnionPlannerTest : FunSpec({
    context("a union with a discriminator") {
        test("is a sealed interface that its members implement") {
            val lines = listOf(ANIMAL, CAT, DOG)

            kotlinOf(lines, UNION) shouldContain "@Serializable(with = PetstoreAnimalSerializer::class)"
            kotlinOf(lines, UNION) shouldContain
                "public sealed interface PetstoreAnimal : DataTransferObject, Validatable"
            kotlinOf(lines, "PetstoreCat") shouldContain "    PetstoreAnimal {"
            kotlinOf(lines, "PetstoreDog") shouldContain "    PetstoreAnimal {"
        }

        test("reads the member the discriminator names, by the name of its schema when nothing maps it") {
            val kotlin = kotlinOf(listOf(ANIMAL, CAT, DOG), SERIALIZER)

            kotlin shouldContain "internal object PetstoreAnimalSerializer : JsonContentPolymorphicSerializer"
            kotlin shouldContain """"Cat" to PetstoreCat.serializer(),"""
            kotlin shouldContain """"Dog" to PetstoreDog.serializer(),"""
            kotlin shouldContain """val tag = fields?.get("kind") as? JsonPrimitive"""
            kotlin shouldContain """throw SerializationException("The property kind names none of Cat, Dog")"""
        }

        test("reads a member under every value that is mapped to it") {
            val mapping = "mapping: {cat: '#/components/schemas/Cat', kitten: Cat}"
            val animal = "Animal: {anyOf: $MEMBERS, discriminator: {propertyName: kind, $mapping}}"
            val kotlin = kotlinOf(listOf(animal, CAT, DOG), SERIALIZER)

            kotlin shouldContain """"cat" to PetstoreCat.serializer(),"""
            kotlin shouldContain """"kitten" to PetstoreCat.serializer(),"""
            kotlin shouldContain """"Dog" to PetstoreDog.serializer(),"""
        }

        test("is validated as the member it is where a property holds it") {
            val owner = "Owner: {type: object, properties: {pet: {\$ref: '#/components/schemas/Animal'}}}"
            val kotlin = kotlinOf(listOf(ANIMAL, CAT, DOG, owner), "PetstoreOwner")

            kotlin shouldContain "public val pet: PetstoreAnimal? = null"
            kotlin shouldContain "within(::pet) { with(pet) { validate() } }"
        }

        test("is declared where a property holds it inline as well") {
            val pet = "{oneOf: $MEMBERS, discriminator: {propertyName: kind}}"
            val owner = "Owner: {type: object, properties: {pet: $pet}}"
            val kotlin = kotlinOf(listOf(owner, CAT, DOG), "PetstoreOwnerPet")

            kotlin shouldContain "public sealed interface PetstoreOwnerPet"
        }
    }

    context("a schema with a discriminator that others extend") {
        test("is the sealed interface of the schemas that are all of it") {
            val shape = "Shape: {type: object, required: [kind], properties: {kind: {type: string}}, $TAG}"
            val base = "{\$ref: '#/components/schemas/Shape'}"
            val circle = "Circle: {allOf: [$base], properties: {radius: {type: integer}}}"
            val loose = "Loose: {allOf: [{type: object, properties: {a: {type: string}}}]}"
            val lines = listOf(shape, circle, "Anything: true", loose)

            kotlinOf(lines, "PetstoreShape") shouldContain "public sealed interface PetstoreShape"
            kotlinOf(lines, "PetstoreShapeSerializer") shouldContain
                """"Circle" to PetstoreCircle.serializer(),"""
            kotlinOf(lines, "PetstoreCircle") shouldContain "public val kind: String,"
            kotlinOf(lines, "PetstoreCircle") shouldContain "    PetstoreShape {"
        }
    }

    context("a union without a discriminator") {
        test("reads the member whose own required property the object has, and exactly one for oneOf") {
            val kotlin = kotlinOf(listOf("Command: {oneOf: $WATCHES}", ALL, ONE), "PetstoreCommandSerializer")

            kotlin shouldContain """"since" to PetstoreAll.serializer(),"""
            kotlin shouldContain """"id" to PetstoreOne.serializer(),"""
            kotlin shouldContain "val present = members.keys.filter { name -> fields?.containsKey(name) == true }"
            kotlin shouldContain "val key = present.singleOrNull().orEmpty()"
            kotlin shouldContain """throw SerializationException("The object has not exactly one of since, id")"""
        }

        test("reads the first member that fits for anyOf") {
            val kotlin = kotlinOf(listOf("Command: {anyOf: $WATCHES}", ALL, ONE), "PetstoreCommandSerializer")

            kotlin shouldContain "val key = present.firstOrNull().orEmpty()"
            kotlin shouldContain """throw SerializationException("The object has none of since, id")"""
        }

        test("is refused when a member has no required property the others lack") {
            val twin = "One: {type: object, required: [since], properties: {since: {type: string}}}"

            message(listOf("Command: {oneOf: $WATCHES}", ALL, twin)) shouldContain
                "a union whose member All has no required property the others lack"
        }
    }

    context("a union the generator cannot write") {
        test("has a member that is no object in the components") {
            val problem = "a union with a member that is no object in the components"
            val inline = "[{type: object, properties: {kind: {type: string}}}]"

            message(listOf("Animal: {oneOf: $inline, $TAG}")) shouldContain problem
            message(listOf("Animal: {oneOf: [true], $TAG}")) shouldContain problem
            val anything = "[{\$ref: '#/components/schemas/Anything'}]"

            message(listOf("Animal: {oneOf: $anything, $TAG}", "Anything: true")) shouldContain problem
            val text = "[{\$ref: '#/components/schemas/Text'}]"

            message(listOf("Animal: {oneOf: $text, $TAG}", "Text: {type: string}")) shouldContain problem
        }

        test("has a member that does not require the discriminator") {
            val loose = "Dog: {type: object, properties: {kind: {type: string}}}"

            message(listOf(ANIMAL, CAT, loose)) shouldContain "a union whose member Dog does not require kind"
        }

        test("maps a value to what is no member") {
            val animal = "Animal: {oneOf: $MEMBERS, discriminator: {propertyName: kind, mapping: {bird: Bird}}}"
            val bird = "Bird: {type: object, required: [kind], properties: {kind: {type: string}}}"

            message(listOf(animal, CAT, DOG, bird)) shouldContain "a discriminator that maps bird to no member"
        }
    }
})
