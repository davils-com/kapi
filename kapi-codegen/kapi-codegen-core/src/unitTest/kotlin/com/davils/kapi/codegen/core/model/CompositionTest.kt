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
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.kotlinOf
import com.davils.kapi.codegen.core.fixture.refusedOf
import com.davils.kapi.codegen.core.fixture.schemas
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val NAMED: String = "Named: {type: object, required: [name], properties: {name: {type: string}}}"

private const val NAME: String = "public val name: String,"

private const val TO_NAMED: String = "{\$ref: '#/components/schemas/Named'}"

private const val AGED: String = "Aged: {allOf: [$TO_NAMED], properties: {age: {type: integer}}}"

private const val TO_TEXT: String = "{\$ref: '#/components/schemas/Text'}"

private suspend fun message(lines: List<String>): String = refusedOf(schemas(lines)).diagnostics.first().message

class CompositionTest : FunSpec({
    context("a schema that is all of others") {
        test("has the properties of every member and its own, required as each of them says") {
            val inline = "{type: object, required: [id], properties: {id: {type: integer}, tag: {type: string}}}"
            val pet = "Pet: {allOf: [$TO_NAMED, $inline], properties: {own: {type: boolean}}}"
            val kotlin = kotlinOf(listOf(pet, NAMED))

            kotlin shouldContain "public val own: Boolean? = null"
            kotlin shouldContain NAME
            kotlin shouldContain "public val id: Long,"
            kotlin shouldContain "public val tag: String? = null"
        }

        test("follows a member that is all of others itself") {
            val kotlin = kotlinOf(listOf("Pet: {allOf: [{\$ref: '#/components/schemas/Aged'}]}", AGED, NAMED))

            kotlin shouldContain "public val age: Long? = null"
            kotlin shouldContain NAME
        }

        test("may name a property twice when both say the same") {
            val same = "{type: object, properties: {name: {type: string}}}"

            kotlinOf(listOf("Pet: {allOf: [$TO_NAMED, $same]}", NAMED)) shouldContain NAME
        }

        test("is refused when two members declare one property in two ways") {
            val other = "{type: object, properties: {name: {type: integer}}}"

            message(listOf("Pet: {allOf: [$TO_NAMED, $other]}", NAMED)) shouldContain
                "allOf that declares the property name in two ways"
        }

        test("is refused when a member is no object of this document") {
            val problem = "allOf with a member that is no object of this document"

            message(listOf("Pet: {allOf: [$TO_TEXT]}", "Text: {type: string}")) shouldContain problem
            message(listOf("Pet: {allOf: [true]}")) shouldContain problem
            message(listOf("Pet: {allOf: [{enum: [a]}]}")) shouldContain problem
        }

        test("is refused when it leads back to itself") {
            val first = "A: {allOf: [{\$ref: '#/components/schemas/B'}]}"
            val second = "B: {allOf: [{\$ref: '#/components/schemas/A'}]}"
            val refused = refusedOf(schemas(listOf(first, second)))

            refused.codes().distinct() shouldBe listOf(CodegenCodes.SCHEMA_UNSUPPORTED)
            refused.diagnostics.first().message shouldContain "allOf that leads back to itself"
        }

        test("reports what the generator has no Kotlin for inside a member") {
            val member = "{type: object, properties: {a: {type: string}}, patternProperties: {'^x': {type: string}}}"

            message(listOf("Pet: {allOf: [$member]}")) shouldContain "no Kotlin for patternProperties yet"
        }
    }
})
