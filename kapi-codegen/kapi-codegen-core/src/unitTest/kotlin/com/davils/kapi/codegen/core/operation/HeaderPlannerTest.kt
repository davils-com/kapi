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

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.fixture.LIST_PETS_RESULT
import com.davils.kapi.codegen.core.fixture.clientGenerationOf
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.responses
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val TEXT: String = "schema: {type: string}"

private const val COUNT: String = "X-Count"

private const val MALFORMED: String = "$LIST_PETS_RESULT.Malformed(exchange.status, "

private const val RAW: String = "public val content: ByteString,\n        public val headers: Headers,"

private const val FIELDS: String = "\n        "

private val readers: Map<String, String> = mapOf(
    "{type: integer, format: int32}" to "int",
    "{type: integer}" to "long",
    "{type: number}" to "double",
    "{type: number, format: float}" to "float",
    "{type: boolean}" to "flag",
)

private val kinds: Map<String, String> = mapOf(
    "{type: integer, format: int32}" to "a whole number",
    "{type: number}" to "a number",
    "{type: boolean}" to "true or false",
)

private val untyped: Map<String, String> = mapOf(
    "a list" to "X-Tags: {schema: {type: array, items: {type: string}}}",
    "an enum" to "X-State: {schema: {type: string, enum: [open, closed]}}",
    "sensitive" to "X-Token: {x-kapi-sensitive: true, $TEXT}",
    "the content type" to "Content-Type: {$TEXT}",
    "described by a media type" to "X-Meta: {content: {application/json: {schema: {type: string}}}}",
)

private fun ok(headers: String, more: String = ""): List<String> =
    responses("'200': {description: ok, headers: {$headers}$more}")

class HeaderPlannerTest : FunSpec({
    context("a header a response requires") {
        test("is a field of the answer, read before the body, and its absence makes the answer malformed") {
            val lines = ok("ETag: {required: true, $TEXT}")
            val kotlin = clientOf(lines)

            kotlin shouldContain "200 to ::listPetsOk,"
            kotlin shouldContain "private fun listPetsOk(exchange: Exchange.Answered): $LIST_PETS_RESULT {"
            kotlin shouldContain """val eTag = exchange.headers["ETag"]"""
            kotlin shouldContain """?: return $MALFORMED"The header ETag is missing")"""
            kotlin shouldContain "return $LIST_PETS_RESULT.Ok(exchange.content, eTag, exchange.headers)"
            clientOf(lines, LIST_PETS_RESULT) shouldContain "public val eTag: String,"
        }

        readers.forEach { (schema, reader) ->
            test("is read as what its schema says: $schema") {
                val kotlin = clientOf(ok("$COUNT: {required: true, schema: $schema}"))

                kotlin shouldContain """val xCount = exchange.headers.$reader("$COUNT")"""
            }
        }

        kinds.forEach { (schema, kind) ->
            test("makes the answer malformed when it is missing or not $kind") {
                val kotlin = clientOf(ok("$COUNT: {required: true, schema: $schema}"))

                kotlin shouldContain """?: return $MALFORMED"The header $COUNT is missing or not $kind")"""
            }
        }

        test("stays optional in Kotlin when its schema allows null") {
            val lines = ok("ETag: {required: true, schema: {type: [string, 'null']}}")

            clientOf(lines, LIST_PETS_RESULT) shouldContain "public val eTag: String?,"
        }
    }

    context("a header a response may carry") {
        test("is a field that is null without it") {
            val lines = ok("ETag: {$TEXT}")
            val kotlin = clientOf(lines)

            kotlin shouldContain """val eTag = exchange.headers["ETag"]"""
            kotlin shouldNotContain "The header ETag"
            clientOf(lines, LIST_PETS_RESULT) shouldContain "public val eTag: String?,"
        }

        test("makes the answer malformed when it is there and not what its schema says") {
            val kotlin = clientOf(ok("$COUNT: {schema: {type: integer, format: int32}}"))

            kotlin shouldContain """val xCount = exchange.headers.int("$COUNT")"""
            kotlin shouldContain """if (xCount == null && "$COUNT" in exchange.headers) return $MALFORMED"""
            kotlin shouldContain """"The header $COUNT is not a whole number")"""
        }
    }

    context("the headers of a response") {
        test("are fields in the order of their names, between the body and every header it came with") {
            val content = ", content: {application/json: {schema: {type: string}}}"
            val lines = ok(headers = "X-B: {$TEXT}, X-A: {$TEXT}", more = content)

            clientOf(lines) shouldContain "$LIST_PETS_RESULT.Ok(decoding.value, xA, xB, exchange.headers)"
            clientOf(lines, LIST_PETS_RESULT) shouldContain
                listOf(
                    "public val body: String,",
                    "public val xA: String?,",
                    "public val xB: String?,",
                ).joinToString(FIELDS)
        }

        test("are read for the response of a range and for the default one as well") {
            val headers = "headers: {ETag: {$TEXT}}"
            val lines = responses("4XX: {description: no, $headers}, default: {description: other, $headers}")
            val kotlin = clientOf(lines)

            kotlin shouldContain "4 to ::listPetsClientError,"
            kotlin shouldContain "return listPetsOther(exchange)"
            kotlin shouldContain "$LIST_PETS_RESULT.Other(exchange.status, exchange.content, eTag, exchange.headers)"
        }

        test("are taken from the headers the components declare") {
            val lines = ok("ETag: {\$ref: '#/components/headers/Revision'}") +
                listOf("components:", "  headers:", "    Revision: {required: true, $TEXT}")

            clientOf(lines, LIST_PETS_RESULT) shouldContain "public val eTag: String,"
        }

        test("get another name when theirs is one the answer has already") {
            val lines = ok("Status: {$TEXT}, Body: {$TEXT}")
            val kotlin = clientOf(lines, LIST_PETS_RESULT)

            kotlin shouldContain "public val bodyHeader: String?,"
            kotlin shouldContain "public val statusHeader: String?,"
        }

        test("are refused when two of them become one name") {
            val lines = ok("$COUNT: {$TEXT}, x_count: {$TEXT}")

            clientGenerationOf(lines).codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
        }
    }

    context("a header that is no text, number or flag") {
        untyped.forEach { (what, header) ->
            test("stays among the headers of the answer when it is $what") {
                val lines = ok(header)

                clientOf(lines, LIST_PETS_RESULT) shouldContain RAW
                clientOf(lines) shouldNotContain "listPetsOk"
            }
        }
    }
})
