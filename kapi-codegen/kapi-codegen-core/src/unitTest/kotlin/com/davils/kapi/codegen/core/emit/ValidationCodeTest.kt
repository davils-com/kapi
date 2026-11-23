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

package com.davils.kapi.codegen.core.emit

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.fixture.kotlinOf
import com.davils.kapi.codegen.core.fixture.pet
import com.davils.kapi.codegen.core.fixture.refusedOf
import com.davils.kapi.codegen.core.fixture.schemas
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val PATTERNED: String = "a: {type: string, pattern: '^x'}"

private const val DEPENDENT: String = "  dependentRequired: {a: [b]}"

private const val MISFIT: String = "const that does not fit its type"

private const val FIRST: String = "a: {type: string}"

private const val SECOND: String = "b: {type: string}"

private const val SHORT: String = "a: {type: string, minLength: 2}"

private val rules: Map<String, String> = mapOf(
    SHORT to
        """if (Constraints.length(a) < 2) fail(::a, "must have at least 2 characters"""",
    "a: {type: string, maxLength: 9}" to
        """if (Constraints.length(a) > 9) fail(::a, "must have at most 9 characters"""",
    PATTERNED to
        """if (!Constraints.matches(a, A_PATTERN)) fail(::a, "must match ^x", ConstraintViolationCodes.PATTERN)""",
    "a: {type: integer, minimum: 1}" to """if (a < 1L) fail(::a, "must be at least 1"""",
    "a: {type: integer, exclusiveMinimum: 0}" to
        """if (a <= 0L) fail(::a, "must be greater than 0", ConstraintViolationCodes.MINIMUM)""",
    "a: {type: integer, maximum: 9.5}" to
        """if (a.toDouble() > 9.5) fail(::a, "must be at most 9.5", ConstraintViolationCodes.MAXIMUM)""",
    "a: {type: number, exclusiveMaximum: 10}" to
        """if (a >= 10.0) fail(::a, "must be less than 10", ConstraintViolationCodes.MAXIMUM)""",
    "a: {type: number, format: float, minimum: 1}" to
        """if (a.toDouble() < 1.0) fail(::a, "must be at least 1", ConstraintViolationCodes.MINIMUM)""",
    "a: {type: integer, multipleOf: 3}" to
        """if (!Constraints.isMultipleOf(a, 3L)) fail(::a, "must be a multiple of 3"""",
    "a: {type: integer, format: int32, multipleOf: 3}" to "if (!Constraints.isMultipleOf(a.toLong(), 3L))",
    "a: {type: integer, multipleOf: 0.5}" to "if (!Constraints.isMultipleOf(a.toDouble(), 0.5))",
    "a: {type: number, multipleOf: 0.5}" to "if (!Constraints.isMultipleOf(a, 0.5))",
    "a: {type: array, minItems: 1}" to
        """if (a.size < 1) fail(::a, "must have at least 1 items", ConstraintViolationCodes.MIN_ITEMS)""",
    "a: {type: array, maxItems: 4}" to
        """if (a.size > 4) fail(::a, "must have at most 4 items", ConstraintViolationCodes.MAX_ITEMS)""",
    "a: {type: array, uniqueItems: true}" to
        """if (!Constraints.isUnique(a)) fail(::a, "must not repeat an item"""",
    "a: {type: object, minProperties: 1}" to
        """if (a.size < 1) fail(::a, "must have at least 1 entries", ConstraintViolationCodes.MIN_PROPERTIES)""",
    "a: {type: object, maxProperties: 4}" to
        """if (a.size > 4) fail(::a, "must have at most 4 entries", ConstraintViolationCodes.MAX_PROPERTIES)""",
    "a: {enum: [x]}" to
        """if (!a.isKnown) fail(::a, "must be one of the values this API knows", ConstraintViolationCodes.ENUM)""",
    "a: {type: object, properties: {b: {type: string}}}" to "within(::a) { with(a) { validate() } }",
    "a: {const: fixed}" to """if (a != "fixed") fail(::a, "must be fixed", ConstraintViolationCodes.CONST)""",
    "a: {type: integer, const: 5}" to """if (a != 5L) fail(::a, "must be 5", ConstraintViolationCodes.CONST)""",
    "a: {type: integer, format: int32, const: 5}" to "if (a.toLong() != 5L) fail(",
    "a: {type: number, const: 5}" to "if (a != 5.0) fail(",
    "a: {const: 1.5}" to "if (a != 1.5) fail(",
    "a: {const: 7}" to "public val a: Long,",
    "a: {const: true}" to """if (a != true) fail(::a, "must be true", ConstraintViolationCodes.CONST)""",
    "a: {type: array, items: {type: string, pattern: '^x'}}" to
        """if (!Constraints.matches(item, A_1_PATTERN)) fail("must match ^x", ConstraintViolationCodes.PATTERN)""",
    "a: {type: array, items: {type: array, items: {type: string, minLength: 1}}}" to
        """if (Constraints.length(item1) < 1) fail("must have at least 1 characters"""",
    "a: {type: array, items: {type: array, items: {type: string, maxLength: 1}}}" to "item.forEach { item1 ->",
    "a: {type: array, items: {type: object, additionalProperties: {type: array, maxItems: 2}}}" to
        "item.values.forEach { item1 ->",
    "a: {type: array, items: {type: [string, 'null'], minLength: 1}}" to "if (item != null) {",
    "a: {additionalProperties: {type: object, properties: {b: {type: string}}}}" to "with(item) { validate() }",
)

class ValidationCodeTest : FunSpec({
    context("the validation of a property") {
        rules.forEach { (property, check) ->
            test("checks $property") {
                kotlinOf(pet(property, required = "a")) shouldContain check
            }
        }

        test("loops over a list and a map with the path of every item") {
            val kotlin = kotlinOf(pet("a: {type: array, items: {enum: [x]}}", "b: {additionalProperties: {enum: [y]}}"))

            kotlin shouldContain "each(::a, a) { item ->"
            kotlin shouldContain "each(::b, b) { item ->"
        }

        test("is guarded when the property may be absent") {
            val kotlin = kotlinOf(pet(SHORT, "b: {type: array, items: {type: string}}"))

            kotlin shouldContain "if (a != null) {"
            kotlin shouldContain "    if (Constraints.length(a) < 2)"
        }

        test("compiles a pattern once, for the linear engine, in a private value of the file") {
            val kotlin = kotlinOf(pet(PATTERNED))

            kotlin shouldContain """private val A_PATTERN: Pattern = Pattern.compileOrThrow("^x")"""
            kotlin shouldNotContain "Regex"
            kotlin shouldNotContain "companion object"
        }

        test("compiles a pattern two properties share only once") {
            val kotlin = kotlinOf(pet(PATTERNED, "b: {type: string, pattern: '^x'}"))

            kotlin shouldContain "Constraints.matches(b, A_PATTERN)"
            kotlin shouldNotContain "B_PATTERN"
        }

        test("gives two patterns whose names meet a name of their own") {
            val kotlin = kotlinOf(
                pet(
                    "a: {type: string, pattern: '^x', x-kapi-name: aB}",
                    "b: {type: string, pattern: '^y', x-kapi-name: a_B}",
                ),
            )

            kotlin shouldContain """private val A_B_PATTERN: Pattern = Pattern.compileOrThrow("^x")"""
            kotlin shouldContain """private val A_B_2_PATTERN: Pattern = Pattern.compileOrThrow("^y")"""
        }

        test("is refused when the linear engine refuses it, at the schema") {
            val refused = refusedOf(schemas(pet("a: {type: string, pattern: '(?=x)y'}")))
            val diagnostic = refused.diagnostics.single()

            diagnostic.code shouldBe CodegenCodes.SCHEMA_UNSUPPORTED
            diagnostic.message shouldContain "is refused at offset 0"
            diagnostic.location.shouldNotBeNull()
        }
    }

    context("a constant") {
        test("is refused when it does not fit the type of its schema or is no single value") {
            val misfit = refusedOf(schemas(pet("a: {type: string, const: 5}"))).diagnostics.single()
            val fraction = refusedOf(schemas(pet("a: {type: integer, const: 1.5}"))).diagnostics.single()
            val list = refusedOf(schemas(pet("a: {const: [1]}"))).diagnostics.single()
            val flag = refusedOf(schemas(pet("a: {type: string, const: true}"))).diagnostics.single()
            val mapping = refusedOf(schemas(pet("a: {const: {b: 1}}"))).diagnostics.single()

            misfit.message shouldContain MISFIT
            fraction.message shouldContain MISFIT
            list.message shouldContain "const that is no text, number or flag"
            flag.message shouldContain MISFIT
            mapping.message shouldContain "const that is no text, number or flag"
        }
    }

    context("a property another one depends on") {
        test("is required once the other one is given") {
            val kotlin = kotlinOf(pet(FIRST, SECOND) + DEPENDENT)

            kotlin shouldContain """if (a != null && b == null) fail(::b, "is required when a is given","""
        }

        test("is required at once when the other one always is") {
            val kotlin = kotlinOf(pet(FIRST, SECOND, required = "a") + DEPENDENT)

            kotlin shouldContain """if (b == null) fail(::b, "is required when a is given","""
        }

        test("needs no check when it is required anyway") {
            val kotlin = kotlinOf(pet(FIRST, SECOND, required = "b") + DEPENDENT)

            kotlin shouldNotContain "DEPENDENT_REQUIRED"
        }

        test("is refused when the object declares no property of that name") {
            val refused = refusedOf(schemas(pet(FIRST) + DEPENDENT))

            refused.diagnostics.single().message shouldContain
                """dependentRequired that names "b", which is no property"""
        }
    }

    context("a property that is sensitive") {
        test("is masked when the object is rendered") {
            val kotlin = kotlinOf(pet("a: {type: string, x-kapi-sensitive: true}", "b: {type: string}"))

            kotlin shouldContain """= "PetstorePet(" + "a=" + "****" + ", b=" + this.b + ")""""
        }
    }
})
