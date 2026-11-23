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

private const val STATUS: String = "PetstoreStatus"

private suspend fun status(schema: String): String = kotlinOf(listOf("Status: $schema"), STATUS)

class EnumPlannerTest : FunSpec({
    context("an enumeration of texts") {
        test("is a value class that keeps a value it does not know") {
            val kotlin = status("{type: string, enum: [open, sold-out]}")

            kotlin shouldContain "public value class PetstoreStatus("
            kotlin shouldContain "public val `value`: String,"
            kotlin shouldContain ") : DataTransferObject {"
            kotlin shouldContain "public val OPEN: PetstoreStatus = PetstoreStatus(\"open\")"
            kotlin shouldContain "public val SOLD_OUT: PetstoreStatus = PetstoreStatus(\"sold-out\")"
            kotlin shouldContain "public val entries: List<PetstoreStatus> = listOf(OPEN, SOLD_OUT)"
            kotlin shouldContain "get() = this in entries"
            kotlin shouldContain "override fun toString(): String = `value`"
        }

        test("names its entries as the document declares, under either keyword") {
            status("{enum: [a, b], x-kapi-enum-names: [FIRST, SECOND]}") shouldContain "public val FIRST: "
            status("{enum: [a, b], x-enum-varnames: [ONE, TWO]}") shouldContain "public val TWO: "
        }

        test("leaves null out of its entries") {
            status("{enum: [a, null]}") shouldContain "listOf(A)"
        }
    }

    context("an enumeration of whole numbers") {
        test("is a value class of a long with a name for every number") {
            val kotlin = status("{type: integer, enum: [1, -2]}")

            kotlin shouldContain "public val `value`: Long,"
            kotlin shouldContain "public val VALUE_1: PetstoreStatus = PetstoreStatus(1L)"
            kotlin shouldContain "public val VALUE_MINUS_2: PetstoreStatus = PetstoreStatus(-2L)"
            kotlin shouldContain "override fun toString(): String = `value`.toString()"
        }

        test("is a value class of an int when the format says so") {
            val kotlin = status("{type: integer, format: int32, enum: [7]}")

            kotlin shouldContain "public val `value`: Int,"
            kotlin shouldContain "PetstoreStatus(7)"
        }
    }

    context("an enumeration the generator cannot write") {
        test("mixes texts and numbers, has a fraction or has no value") {
            val refused = listOf(CodegenCodes.ENUM_VALUES)

            refusedOf(schemas(listOf("Status: {enum: [a, 1]}"))).codes() shouldBe refused
            refusedOf(schemas(listOf("Status: {enum: [1.5]}"))).codes() shouldBe refused
            refusedOf(schemas(listOf("Status: {enum: [true]}"))).codes() shouldBe refused
            refusedOf(schemas(listOf("Status: {enum: [null]}"))).codes() shouldBe refused
        }

        test("has two values that become one name") {
            refusedOf(schemas(listOf("Status: {enum: [sold-out, sold_out]}"))).codes() shouldBe
                listOf(CodegenCodes.NAME_COLLISION)
        }
    }
})
