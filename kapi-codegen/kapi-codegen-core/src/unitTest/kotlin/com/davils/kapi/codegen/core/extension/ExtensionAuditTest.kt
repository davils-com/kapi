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

package com.davils.kapi.codegen.core.extension

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.generatedOf
import com.davils.kapi.codegen.core.fixture.generationOf
import com.davils.kapi.codegen.core.fixture.operation
import com.davils.kapi.codegen.core.fixture.refusedOf
import com.davils.kapi.codegen.core.fixture.schema
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val ENUM: String = "enum: [a, b]"

private const val MONEY: String = "  type: com.example.Money"

private const val TIMEOUT: String = "x-kapi-timeout:"

private const val PAGINATION: String = "x-kapi-pagination:"

private const val REQUEST: String = "  request: PT30S"

private const val TYPE: String = "x-kapi-type:"

private const val PAGED: String = "  style: page"

private const val SOON: String = "x-kapi-timeout: soon"

private const val AFTER: String = "  parameter: after"

private const val TWO_NAMES: String = "x-kapi-enum-names: [FIRST, SECOND]"

private val value: List<Any> = listOf(CodegenCodes.EXTENSION_VALUE)

private val accepted: Map<String, List<String>> = mapOf(
    "a name on a schema" to schema("x-kapi-name: Animal"),
    "a type on a schema" to schema("x-kapi-type: kotlin.uuid.Uuid"),
    "a type with a serializer" to
        schema(TYPE, MONEY, "  serializer: com.example.MoneySerializer"),
    "flags on a schema" to schema("x-kapi-ignore: false", "x-kapi-sensitive: true"),
    "names for entries" to schema(ENUM, TWO_NAMES),
    "the common alias for entry names" to schema(ENUM, "x-enum-varnames: [FIRST, SECOND]"),
    "flags on an operation" to operation("x-kapi-idempotent: true", "x-kapi-tri-state: false", "x-kapi-ignore: false"),
    "a name and a group on an operation" to operation("x-kapi-name: pets", "x-kapi-group: Catalog"),
    "a timeout as a duration" to operation("x-kapi-timeout: PT30S"),
    "a timeout per phase" to operation(TIMEOUT, REQUEST, "  connect: PT5S", "  socket: PT1M"),
    "a pagination" to operation(PAGINATION, "  style: cursor", AFTER, "  items: data"),
    "a keyword another part of kide owns" to schema("x-kapi-requires-restart: true", "x-kapi-deprecation: soon"),
    "an extension of somebody else" to operation("x-vendor-anything:", "  nested: [1, 2]"),
)

private val mistyped: Map<String, List<String>> = mapOf(
    "a name that is no identifier" to schema("x-kapi-name: 1st"),
    "a name that is a keyword" to schema("x-kapi-name: class"),
    "a name that is no text" to schema("x-kapi-name: 7"),
    "a type that is no qualified name" to schema("x-kapi-type: kotlin..Uuid"),
    "a type mapping whose type is no qualified name" to schema(TYPE, "  type: 1x"),
    "a type mapping without a type" to schema(TYPE, "  serializer: com.example.MoneySerializer"),
    "a type mapping with another key" to schema(TYPE, MONEY, "  nullable: true"),
    "a flag that is a text" to schema("x-kapi-sensitive: 'yes'"),
    "entry names that repeat" to schema(ENUM, "x-kapi-enum-names: [FIRST, FIRST]"),
    "entry names that are no identifiers" to schema(ENUM, "x-kapi-enum-names: [FIRST, 2nd]"),
    "entry names that are empty" to schema("x-kapi-enum-names: []"),
    "entry names that are no list" to schema("x-kapi-enum-names: FIRST"),
    "a blank group" to operation("x-kapi-group: '  '"),
    "a timeout that is no duration" to operation(SOON),
    "a timeout of zero" to operation("x-kapi-timeout: PT0S"),
    "a timeout without an end" to operation("x-kapi-timeout: PT999999999999999H"),
    "a timeout mapping without a phase" to operation("x-kapi-timeout: {}"),
    "a timeout mapping with another phase" to operation(TIMEOUT, "  total: PT30S"),
    "a timeout mapping with a phase that is no duration" to operation(TIMEOUT, "  request: 30"),
    "a pagination without a style" to operation(PAGINATION, AFTER),
    "a pagination with another style" to operation(PAGINATION, "  style: keyset"),
    "a pagination with a blank text" to operation(PAGINATION, PAGED, "  items: ''"),
    "a pagination with another key" to operation(PAGINATION, PAGED, "  total: count"),
    "a pagination that is no mapping" to operation("x-kapi-pagination: cursor"),
)

class ExtensionAuditTest : FunSpec({
    context("a keyword of the catalogue") {
        accepted.forEach { (case, lines) ->
            test("accepts $case") {
                generatedOf(lines).diagnostics.shouldBeEmpty()
            }
        }

        mistyped.forEach { (case, lines) ->
            test("refuses $case") {
                refusedOf(lines).codes() shouldBe value
            }
        }

        test("says what it expects") {
            val generation = generationOf(operation(SOON))

            generation.diagnostics.single().message shouldBe
                "x-kapi-timeout must be ${ExtensionShape.Timeout.expectation}"
        }

        test("is refused where it has no meaning") {
            val generation = generationOf(schema("x-kapi-idempotent: true"))

            generation.codes() shouldBe listOf(CodegenCodes.EXTENSION_SITE)
            generation.diagnostics.single().message shouldBe
                "x-kapi-idempotent has no meaning here, it belongs to Operation"
        }

        test("is refused on what is neither a schema nor an operation, a parameter, a header or a path") {
            val generation = generationOf(listOf("tags:", "  - name: pets", "    x-kapi-name: animals", "paths: {}"))

            generation.codes() shouldBe listOf(CodegenCodes.EXTENSION_SITE)
        }

        test("reports a wrong place and a wrong value together") {
            val generation = generationOf(schema("x-kapi-group: ''"))

            generation.codes() shouldContainExactlyInAnyOrder
                listOf(CodegenCodes.EXTENSION_SITE, CodegenCodes.EXTENSION_VALUE)
        }
    }

    context("the namespace x-kapi-") {
        test("is closed to keywords the generator does not know") {
            val generation = generationOf(operation("x-kapi-retries: 3"))

            generation.codes() shouldBe listOf(CodegenCodes.EXTENSION_UNKNOWN)
            generation.diagnostics.single().message shouldContain "reserved for the keywords of kide"
        }

        test("says that WebSocket is not generated") {
            val generation = generationOf(operation("x-kapi-websocket: true"))

            generation.codes() shouldBe listOf(CodegenCodes.EXTENSION_UNKNOWN)
            generation.diagnostics.single().message shouldContain "WebSocket endpoints are not generated"
        }

        test("is watched outside of schemas and operations as well") {
            val generation = generationOf(listOf("tags:", "  - name: pets", "    x-kapi-retries: 3", "paths: {}"))

            generation.codes() shouldBe listOf(CodegenCodes.EXTENSION_UNKNOWN)
        }
    }

    context("names for the entries of an enumeration") {
        test("must be as many as the entries") {
            val generation = generationOf(schema("enum: [a, b, c]", TWO_NAMES))

            generation.codes() shouldBe value
            generation.diagnostics.single().message shouldBe "The schema has 3 entries but 2 names for them"
        }

        test("need an enumeration") {
            generationOf(schema("x-kapi-enum-names: [FIRST]")).codes() shouldBe value
        }

        test("are given once, under one of both keywords") {
            val generation = generationOf(schema("enum: [a]", "x-kapi-enum-names: [FIRST]", "x-enum-varnames: [FIRST]"))

            generation.codes() shouldBe value
            generation.diagnostics.single().message shouldContain "both name the entries"
        }
    }

    context("the catalogue") {
        test("holds every public keyword once and none of another owner") {
            val names = ExtensionCatalogue.keywords.map(ExtensionKeyword::name)

            names shouldBe Keywords.all
            val texts = names.map { name -> name.value }.toSet()

            texts.intersect(ExtensionCatalogue.foreign).shouldBeEmpty()
            texts.intersect(ExtensionCatalogue.unsupported.keys).shouldBeEmpty()
        }
    }
})
