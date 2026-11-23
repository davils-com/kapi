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


package com.davils.kapi.codegen.core.operation

import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.fixture.LIST_PETS
import com.davils.kapi.codegen.core.fixture.LIST_PETS_RESULT
import com.davils.kapi.codegen.core.fixture.OK
import com.davils.kapi.codegen.core.fixture.clientGenerationOf
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.get
import com.davils.kapi.codegen.core.fixture.paths
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val TEXT: String = "{type: string}"

private const val NUMBER: String = "{type: integer}"

private const val SMALL: String = "{type: integer, format: int32}"

private const val LIST: String = "{type: array, items: {type: string}}"

private const val PAGE: String = "{type: object, properties: {items: $LIST, next: $TEXT}}"

private const val CURSOR: String = "style: cursor, parameter: position, next: next"

private const val OFFSET: String = "style: offset, parameter: position, items: items"

private const val SIGNATURE: String = "public fun listPetsPages(limit: String? = null): Flow<$LIST_PETS_RESULT>"

private const val ANSWER: String = "(it as? $LIST_PETS_RESULT.Ok)?.body"

private const val CALL: String = "listPets(limit = limit, position = it)"

private fun paged(pagination: String, parameter: String, body: String, place: String = "query"): List<String> {
    val position = "{name: position, in: $place, schema: $parameter}"
    val parameters = "parameters: [$position, {name: limit, in: query, schema: $TEXT}]"
    val response = "responses: {'200': {description: ok, content: {application/json: {schema: $body}}}}"
    return paths("/pets: {get: {$LIST_PETS, x-kapi-pagination: {$pagination}, $parameters, $response}}")
}

private val invalid: Map<String, List<String>> = mapOf(
    "a parameter the operation does not have" to paged("style: cursor, parameter: other, next: next", TEXT, PAGE),
    "no parameter at all" to paged("style: cursor, next: next", TEXT, PAGE),
    "a cursor that is a number" to paged(CURSOR, NUMBER, PAGE),
    "an offset that is a text" to paged(OFFSET, TEXT, PAGE),
    "a parameter outside the query" to paged(pagination = CURSOR, parameter = TEXT, body = PAGE, place = "header"),
    "a next the response does not have" to paged("style: cursor, parameter: position, next: other", TEXT, PAGE),
    "a next that is no text" to paged("style: cursor, parameter: position, next: items", TEXT, PAGE),
    "no next" to paged("style: cursor, parameter: position", TEXT, PAGE),
    "items that are no list" to paged("style: offset, parameter: position, items: next", NUMBER, PAGE),
    "no items in a response that is no list" to paged("style: page, parameter: position", NUMBER, PAGE),
)

class PagePlannerTest : FunSpec({
    context("an operation that pages by a cursor") {
        test("has a function that follows the cursor of each page, without the parameter it fills itself") {
            val kotlin = clientOf(paged(CURSOR, TEXT, PAGE))

            kotlin shouldContain SIGNATURE
            kotlin shouldContain "= apiCursorPages(next = { $ANSWER?.next }) {"
            kotlin shouldContain "        $CALL\n    }"
        }

        test("follows a response that is the cursor itself") {
            val kotlin = clientOf(paged("style: cursor, parameter: position", TEXT, TEXT))

            kotlin shouldContain "apiCursorPages(next = { $ANSWER }) {"
        }
    }

    context("an operation that pages by an offset") {
        test("has a function that moves by the items of each page") {
            val kotlin = clientOf(paged(OFFSET, NUMBER, PAGE))

            kotlin shouldContain SIGNATURE
            kotlin shouldContain "= apiOffsetPages(size = { $ANSWER?.items?.size }) {\n        $CALL\n"
        }

        test("hands a small parameter the position as the number it takes") {
            clientOf(paged(OFFSET, SMALL, PAGE)) shouldContain "listPets(limit = limit, position = it.toInt())"
        }
    }

    context("an operation that pages by a number") {
        test("has a function that counts the pages of a response that is the list itself") {
            val kotlin = clientOf(paged("style: page, parameter: position", NUMBER, LIST))

            kotlin shouldContain "= apiNumberedPages(size = { $ANSWER?.size }) {\n        $CALL\n"
        }
    }

    context("an operation that names no pages") {
        test("has no function for them") {
            clientOf(get(LIST_PETS)) shouldNotContain "Pages"
        }
    }

    context("an operation that pages by a link") {
        test("is a warning and has no function for its pages yet") {
            val lines = paged("style: link", TEXT, PAGE)
            val generation = clientGenerationOf(lines)

            generation.shouldBeInstanceOf<Generation.Generated>()
            generation.codes() shouldBe listOf(CodegenCodes.EXTENSION_PENDING)
            clientOf(lines) shouldNotContain "listPetsPages"
        }
    }

    context("pagination the document does not back") {
        invalid.forEach { (what, lines) ->
            test("is refused with $what") {
                val generation = clientGenerationOf(lines)

                generation.shouldBeInstanceOf<Generation.Refused>()
                generation.codes() shouldBe listOf(CodegenCodes.PAGINATION_INVALID)
            }
        }

        test("is refused without a successful response that carries JSON") {
            val parameters = "parameters: [{name: position, in: query, schema: $TEXT}]"
            val lines = get(LIST_PETS, "x-kapi-pagination: {$CURSOR}", parameters)

            clientGenerationOf(lines).codes() shouldBe listOf(CodegenCodes.PAGINATION_INVALID)
        }
    }

    context("the function for the pages of an operation") {
        test("is refused under the name of another operation of its client") {
            val response = "responses: {'200': {description: ok, content: {application/json: {schema: $PAGE}}}}"
            val parameters = "parameters: [{name: position, in: query, schema: $TEXT}]"
            val lines = paths(
                "/pets: {get: {$LIST_PETS, x-kapi-pagination: {$CURSOR}, $parameters, $response}}",
                "/pages: {get: {operationId: listPetsPages, $OK}}",
            )

            clientGenerationOf(lines).codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
        }
    }
})
