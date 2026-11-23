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

import com.davils.kapi.codegen.core.fixture.generatedOf
import com.davils.kapi.codegen.core.fixture.kotlinOf
import com.davils.kapi.codegen.core.fixture.pet
import com.davils.kapi.codegen.core.fixture.schemas
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain

private const val STATUS: String = "Status: {type: string, enum: [open, closed]}"

private const val NAMED: String = "Named: {type: object, properties: {name: {type: string}}}"

private const val TO_NAMED: String = "{\$ref: '#/components/schemas/Named'}"

private const val LOOSE: String = "{type: object, example: {name: rex}}"

private const val TAGS: String = "Tags: {type: array, items: {type: object, properties: {tag: {type: string}}}}"

private const val TO_TAGS: String = "{\$ref: '#/components/schemas/Tags'}"

class AliasTest : FunSpec({
    context("an inline schema that is all of one other") {
        test("is that other, so a description next to a reference keeps its type") {
            val status = "status: {description: The state, allOf: [{\$ref: '#/components/schemas/Status'}]}"

            kotlinOf(pet(status) + STATUS) shouldContain "public val status: PetstoreStatus? = null"
        }

        test("ignores a member that constrains nothing beyond being an object") {
            val owner = "owner: {allOf: [$TO_NAMED, $LOOSE]}"

            kotlinOf(pet(owner) + NAMED) shouldContain "public val owner: PetstoreNamed? = null"
        }
    }

    context("a schema that is all of others") {
        test("composes no property out of a member that constrains nothing") {
            val generated = generatedOf(schemas(listOf("Pet: {allOf: [$TO_NAMED, $LOOSE]}", NAMED)))

            generated.diagnostics.shouldBeEmpty()
            kotlinOf(listOf("Pet: {allOf: [$TO_NAMED, $LOOSE]}", NAMED)) shouldContain "public val name: String? = null"
        }
    }

    context("an inline schema inside a component that is no model") {
        test("is planned once, however often the component is referred to") {
            val generated = generatedOf(schemas(pet("a: $TO_TAGS", "b: $TO_TAGS") + TAGS))
            val items = generated.files.filter { file -> file.path.endsWith("TagsItem.kt") }

            items shouldHaveSize 1
        }
    }
})
