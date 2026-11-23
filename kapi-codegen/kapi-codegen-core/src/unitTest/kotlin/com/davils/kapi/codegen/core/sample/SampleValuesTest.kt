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

package com.davils.kapi.codegen.core.sample

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.fixture.UNMATCHED
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.mockGenerationOf
import com.davils.kapi.codegen.core.fixture.pet
import com.davils.kapi.codegen.core.fixture.sampleOf
import com.davils.kapi.codegen.core.fixture.schemas
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val EMPTY: String = "get() = PetstorePet()"
private const val TEXTS: String = "type: array, items: {type: string}"
private const val UNMATCHED_ITEMS: String = "type: array, items: $UNMATCHED"
private const val COUNTS: String = "type: object, additionalProperties: {type: integer}"

class SampleValuesTest : FunSpec({
    context("a property that may be null") {
        test("is filled with a value when there is one, and with null when there is none") {
            sampleOf("a: {type: [string, 'null']}", required = "a") shouldContain "a = \"string\","
            sampleOf("a: {type: [string, 'null'], pattern: '^[0-9]{4}\$'}", required = "a") shouldContain "a = null,"
        }
    }

    context("a property that may be absent") {
        test("is filled with a value when there is one, and left out when there is none") {
            sampleOf("a: {type: string}") shouldContain "a = \"string\","
            sampleOf("a: $UNMATCHED") shouldContain EMPTY
        }

        test("is left out when it holds a type the generator did not write") {
            sampleOf("a: {type: string, x-kapi-type: kotlin.time.Duration}") shouldContain EMPTY
        }
    }

    context("a property that must be there") {
        test("leaves the model without a sample when there is no value for it") {
            val generation = mockGenerationOf(schemas(pet("a: $UNMATCHED", required = "a")))

            generation.shouldBeInstanceOf<Generation.Generated>()
            generation.codes() shouldBe listOf(CodegenCodes.SAMPLE_UNAVAILABLE)
            generation.diagnostics.single().message shouldContain "PetstorePet"
            generation.diagnostics.single().message shouldContain "its property \"a\";"
        }
    }

    context("a list") {
        test("has one item, or as many as it must have") {
            sampleOf("a: {$TEXTS}") shouldContain "a = listOf(\"string\"),"
            sampleOf("a: {$TEXTS, minItems: 2}") shouldContain """a = listOf("string", "string"),"""
        }

        test("is empty when it may hold nothing or there is no value for its items") {
            sampleOf("a: {$TEXTS, maxItems: 0}") shouldContain "a = emptyList(),"
            sampleOf("a: {$UNMATCHED_ITEMS}") shouldContain "a = emptyList(),"
        }

        test("is left out when it needs items there is no value for, or more than one that must differ") {
            sampleOf("a: {$UNMATCHED_ITEMS, minItems: 1}") shouldContain EMPTY
            sampleOf("a: {$TEXTS, minItems: 2, uniqueItems: true}") shouldContain EMPTY
        }

        test("holds lists of its own") {
            sampleOf("a: {type: array, items: {$TEXTS}}") shouldContain "a = listOf(listOf(\"string\")),"
        }
    }

    context("a map") {
        test("has one entry, or as many as it must have") {
            sampleOf("a: {$COUNTS}") shouldContain "a = mapOf(\"key1\" to 0L),"
            sampleOf("a: {$COUNTS, minProperties: 2}") shouldContain """a = mapOf("key1" to 0L, "key2" to 0L),"""
        }

        test("is empty when it may hold nothing or there is no value for its entries") {
            sampleOf("a: {$COUNTS, maxProperties: 0}") shouldContain "a = emptyMap(),"
            sampleOf("a: {type: object, additionalProperties: $UNMATCHED}") shouldContain "a = emptyMap(),"
        }

        test("is left out when it needs entries there is no value for") {
            sampleOf("a: {type: object, minProperties: 1, additionalProperties: $UNMATCHED}") shouldContain EMPTY
        }
    }

    context("an enumeration") {
        test("is its first value") {
            sampleOf("a: {type: string, enum: [x, y]}") shouldContain "a = PetstorePetA.X,"
        }
    }

    context("a scalar") {
        test("is written as the literal of its type") {
            val sample = sampleOf(
                "a: {type: integer, format: int32, minimum: 10}",
                "b: {type: integer, maximum: -2}",
                "c: {type: number, format: float}",
                "d: {type: number, minimum: 1.5}",
                "e: {type: boolean}",
                "f: {type: string, format: date-time}",
                "g: {type: string, format: byte}",
                "h: {}",
            )

            sample shouldContain "a = 10,"
            sample shouldContain "b = -2L,"
            sample shouldContain "c = 0.0f,"
            sample shouldContain "d = 1.5,"
            sample shouldContain "e = false,"
            sample shouldContain "f = Instant.parse(\"2026-01-01T00:00:00Z\"),"
            sample shouldContain "g = ByteString(),"
            sample shouldContain "h = JsonObject(emptyMap()),"
        }

        test("is the example or the default its schema gives") {
            val sample = sampleOf(
                "a: {type: string, examples: [Rex]}",
                "b: {type: integer, default: 7}",
                "c: {type: string, const: card}",
            )

            sample shouldContain "a = \"Rex\","
            sample shouldContain "b = 7L,"
            sample shouldContain "c = \"card\","
        }
    }
})
