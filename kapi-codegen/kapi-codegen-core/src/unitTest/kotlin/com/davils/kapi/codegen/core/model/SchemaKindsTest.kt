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

import com.davils.kapi.codegen.core.fixture.read
import com.davils.kapi.codegen.core.fixture.schemas
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val OPEN: String = "Open: {type: object}"

private val kinds: Map<String, Boolean> = mapOf(
    OPEN to true,
    "Any: {}" to true,
    "Referring: {\$ref: '#/components/schemas/Open'}" to false,
    "Listed: {type: object, enum: [{}]}" to false,
    "Itemed: {items: {type: string}}" to false,
    "Composed: {allOf: [{type: object}]}" to false,
    "Chosen: {oneOf: [{type: object}, {type: string}]}" to false,
    "Propertied: {type: object, properties: {a: {type: string}}}" to false,
    "Additional: {type: object, additionalProperties: {type: string}}" to false,
    "Text: {type: string}" to false,
)

class SchemaKindsTest : FunSpec({
    context("a schema that leaves its object open") {
        kinds.forEach { (declared, isUnconstrained) ->
            test("$declared is unconstrained: $isUnconstrained") {
                val name = declared.substringBefore(':')
                val declarations = listOf(OPEN, declared).distinct()
                val document = read(schemas(declarations)).document
                val components = document.components.shouldNotBeNull()
                val schema = components.schemas.getValue(name)

                SchemaKinds.isUnconstrained(schema) shouldBe isUnconstrained
            }
        }
    }
})
