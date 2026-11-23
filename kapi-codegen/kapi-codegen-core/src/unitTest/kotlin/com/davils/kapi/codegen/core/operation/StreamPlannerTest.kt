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
import com.davils.kapi.codegen.core.fixture.LIST_PETS
import com.davils.kapi.codegen.core.fixture.OK
import com.davils.kapi.codegen.core.fixture.clientGenerationOf
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.get
import com.davils.kapi.codegen.core.fixture.paths
import com.davils.kapi.codegen.core.fixture.schemas
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val EVENT: String = "StreamEvent"

private const val SSE: String = "text/event-stream"

private const val PET: String = "{\$ref: '#/components/schemas/Pet'}"

private const val STREAM: String = "listPetsStream"

private val pets: List<String> = schemas(listOf("Pet: {type: object, properties: {name: {type: string}}}"))

private val records: List<String> = listOf("application/x-ndjson", "application/jsonl", "application/x-jsonlines")

private fun streaming(content: String, vararg more: String): List<String> {
    val response = "responses: {'200': {description: ok, content: {$content}}}"
    return paths("/pets: {get: {${(listOf(LIST_PETS) + more + response).joinToString()}}}") + pets
}

class StreamPlannerTest : FunSpec({
    context("an operation that answers with events") {
        test("has a function that reads each event as what the document declares") {
            val kotlin = clientOf(streaming("$SSE: {schema: $PET}"))

            kotlin shouldContain "public fun $STREAM(): Flow<$EVENT<PetstorePet>> {"
            kotlin shouldContain "return client.events(PetstorePet.serializer(), call)"
            kotlin shouldContain """accept("text/event-stream")"""
        }

        test("is recognised whatever the case and the parameters of the media type") {
            val kotlin = clientOf(streaming("'Text/Event-Stream; charset=utf-8': {schema: $PET}"))

            kotlin shouldContain "return client.events(PetstorePet.serializer(), call)"
        }

        test("reads any JSON when the document declares no schema for an event") {
            val kotlin = clientOf(streaming("$SSE: {}"))

            kotlin shouldContain "Flow<$EVENT<JsonElement>>"
            kotlin shouldContain "client.events(JsonElement.serializer(), call)"
        }

        test("names the type of an event after the operation when the document gives it none") {
            val kotlin = clientOf(streaming("$SSE: {schema: {type: object, properties: {a: {type: string}}}}"))

            kotlin shouldContain "client.events(PetstoreListPetsOkItem.serializer(), call)"
        }

        test("takes the parameters of the operation") {
            val parameters = "parameters: [{name: since, in: query, schema: {type: string}}]"
            val kotlin = clientOf(streaming("$SSE: {schema: $PET}", parameters))

            kotlin shouldContain "public fun $STREAM(since: String? = null): Flow<$EVENT<PetstorePet>> {"
        }
    }

    context("an operation that answers with one record per line") {
        records.forEach { type ->
            test("has a function that reads each line: $type") {
                val kotlin = clientOf(streaming("$type: {schema: $PET}"))

                kotlin shouldContain "return client.records(PetstorePet.serializer(), call)"
            }
        }
    }

    context("an operation that answers with JSON") {
        test("has no function for a stream, even when it declares events next to it") {
            val kotlin = clientOf(streaming("application/json: {schema: $PET}, $SSE: {schema: $PET}"))

            kotlin shouldNotContain STREAM
        }

        test("has none without any stream") {
            clientOf(get(LIST_PETS)) shouldNotContain STREAM
        }
    }

    context("the function for the stream of an operation") {
        test("is refused under the name of another operation of its client") {
            val response = "responses: {'200': {description: ok, content: {$SSE: {schema: {type: string}}}}}"
            val lines = paths(
                "/pets: {get: {$LIST_PETS, $response}}",
                "/streams: {get: {operationId: $STREAM, $OK}}",
            )

            clientGenerationOf(lines).codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
        }
    }
})
