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

import com.davils.kapi.codegen.core.fixture.LIST_PETS_RESULT
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.responses
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val TEXT: String = "content: {application/json: {schema: {type: string}}}"

private const val PLAIN: String = "description: ok"

private const val RAW: String = "exchange.content, exchange.headers)"

private const val DECODED: String = "decoding.value, exchange.headers)"

private const val CONTENT: String = "exchange.content)"

private const val REASON: String = "decoding.reason)"

private val statuses: Map<String, String> = mapOf(
    "200" to "Ok",
    "201" to "Created",
    "204" to "NoContent",
    "404" to "NotFound",
    "418" to "Status418",
    "503" to "ServiceUnavailable",
)

private val ranges: Map<String, String> = mapOf(
    "1XX" to "1 to { exchange -> $LIST_PETS_RESULT.Informational(exchange.status, $RAW }",
    "2XX" to "2 to { exchange -> $LIST_PETS_RESULT.Successful(exchange.status, $RAW }",
    "3XX" to "3 to { exchange -> $LIST_PETS_RESULT.Redirection(exchange.status, $RAW }",
    "4XX" to "4 to { exchange -> $LIST_PETS_RESULT.ClientError(exchange.status, $RAW }",
    "5XX" to "5 to { exchange -> $LIST_PETS_RESULT.ServerError(exchange.status, $RAW }",
)

private fun answering(content: String): List<String> = responses("'200': {$PLAIN, $content}")

class ResponsePlannerTest : FunSpec({
    context("a response with a status") {
        statuses.forEach { (status, name) ->
            test("is the answer $name for $status") {
                val kotlin = clientOf(responses("'$status': {$PLAIN}"))

                kotlin shouldContain "$status to { exchange -> $LIST_PETS_RESULT.$name($RAW },"
            }
        }

        test("is found by its status alone when the document names no range") {
            val kotlin = clientOf(responses("'200': {$PLAIN}"))

            kotlin shouldContain "val read = listPetsReaders[exchange.status]\n"
            kotlin shouldNotContain "listPetsRanges"
        }
    }

    context("a response for a range of statuses") {
        ranges.forEach { (range, reader) ->
            test("is an answer that keeps the status: $range") {
                clientOf(responses("$range: {$PLAIN}")) shouldContain reader
            }
        }

        test("is found after the status itself") {
            val kotlin = clientOf(responses("'404': {$PLAIN}, 4XX: {$PLAIN}"))

            kotlin shouldContain "val read = listPetsReaders[exchange.status] ?: listPetsRanges[exchange.status / 100]"
        }
    }

    context("the default response") {
        test("answers every other status with its body") {
            val kotlin = clientOf(responses("default: {$PLAIN, $TEXT}"))

            kotlin shouldContain "return when (val decoding = client.decode(String.serializer(), exchange)) {"
            kotlin shouldContain "$LIST_PETS_RESULT.Other(exchange.status, $DECODED"
        }

        test("answers every other status with what was sent when it declares no JSON") {
            val kotlin = clientOf(responses("default: {$PLAIN}"))

            kotlin shouldContain "return $LIST_PETS_RESULT.Other(exchange.status, $RAW\n"
        }

        test("is the unexpected answer when the document has none") {
            val kotlin = clientOf(responses("'200': {$PLAIN}"))

            kotlin shouldContain "return $LIST_PETS_RESULT.Unexpected(exchange.status, exchange.headers, $CONTENT"
        }
    }

    context("the body of a response") {
        test("is decoded as the JSON the document declares, and asked for") {
            val kotlin = clientOf(answering(TEXT))

            kotlin shouldContain "when (val decoding = client.decode(String.serializer(), exchange)) {"
            kotlin shouldContain "is Decoding.Decoded -> $LIST_PETS_RESULT.Ok($DECODED"
            kotlin shouldContain "is Decoding.Failed -> $LIST_PETS_RESULT.Malformed(exchange.status, $REASON"
            kotlin shouldContain "            accept(MediaRange.of(\"application/json\"))"
        }

        test("is JSON under every media type that ends in +json, whatever its parameters") {
            val content = "content: {'application/problem+json; charset=utf-8': {schema: {type: string}}}"
            val kotlin = clientOf(answering(content))

            kotlin shouldContain "accept(MediaRange.of(\"application/problem+json; charset=utf-8\"))"
            kotlin shouldContain "$LIST_PETS_RESULT.Ok($DECODED"
        }

        test("is any JSON when its media type has no schema") {
            val kotlin = clientOf(responses("'200': {$PLAIN, content: {application/json: {}}}"))

            kotlin shouldContain "client.decode(JsonElement.serializer(), exchange)"
        }

        test("stays what was sent when no media type is JSON, and every declared one is asked for") {
            val content = "content: {text/plain: {schema: {type: string}}, text/csv: {schema: {type: string}}}"
            val kotlin = clientOf(answering(content))

            kotlin shouldContain "200 to { exchange -> $LIST_PETS_RESULT.Ok($RAW },"
            kotlin shouldContain listOf(
                "            accept(MediaRange.of(\"text/csv\"))",
                "            accept(MediaRange.of(\"text/plain\"))",
            ).joinToString("\n")
        }

        test("is asked for once, however many responses declare it") {
            val kotlin = clientOf(responses("'200': {$PLAIN, $TEXT}, '201': {$PLAIN, $TEXT}"))

            kotlin.split("accept(").size shouldBe 2
        }

        test("is named after the operation and the answer when the document gives it no name") {
            val content = "content: {application/json: {schema: {type: object, properties: {a: {type: string}}}}}"
            val kotlin = clientOf(answering(content))

            kotlin shouldContain "client.decode(PetstoreListPetsOk.serializer(), exchange)"
        }

        test("is taken from the response the components declare") {
            val lines = responses("'404': {\$ref: '#/components/responses/Missing'}") +
                listOf("components:", "  responses:", "    Missing: {description: none, $TEXT}")

            clientOf(lines) shouldContain "is Decoding.Decoded -> $LIST_PETS_RESULT.NotFound($DECODED"
        }
    }
})
