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

import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.kotlinOf
import com.davils.kapi.codegen.core.fixture.parameters
import com.davils.kapi.codegen.core.fixture.pet
import com.davils.kapi.codegen.core.fixture.sampleOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

private const val UNSIGNED: String = "{type: integer, format: uint64}"

class UnsignedTypesTest : FunSpec({
    context("an integer of the format uint64") {
        test("is an unsigned long, so a value above the largest long fits") {
            kotlinOf(pet("index: $UNSIGNED")) shouldContain "val index: ULong? = null"
        }

        test("is checked against its bounds as a number") {
            kotlinOf(pet("index: {type: integer, format: uint64, minimum: 1}")) shouldContain "index.toDouble() < 1.0"
        }

        test("has samples written as unsigned literals") {
            sampleOf("index: $UNSIGNED", required = "index") shouldContain "UL"
        }

        test("is a parameter like every other number") {
            clientOf(parameters("name: index, in: query, schema: $UNSIGNED")) shouldContain "index: ULong? = null"
        }
    }
})
