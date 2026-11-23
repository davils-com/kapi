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
import com.davils.kapi.codegen.core.fixture.clientGenerationOf
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.paths
import com.davils.kapi.codegen.core.fixture.schemas
import com.davils.kapi.codegen.core.fixture.serverGenerationOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val UPGRADED: String = "operationId: attach, x-kapi-upgrade: tcp"

private const val OK: String = "responses: {'200': {description: ok}}"

private const val BUILD: String = "operationId: build"

private const val EVENTS: String = "operationId: watch"

private const val PET: String = "{\$ref: '#/components/schemas/Pet'}"

private const val RAW: String = "application/vnd.docker.raw-stream"

private const val BYTES: String = "Flow<StreamEvent<ByteString>>"

private val pets: List<String> = schemas(listOf("Pet: {type: object, properties: {name: {type: string}}}"))

private val attach: List<String> = operation(
    "post",
    UPGRADED,
    "parameters: [{name: stream, in: query, schema: {type: boolean}}]",
    "responses: {'101': {description: switched, content: {$RAW: {x-kapi-stream: bytes}}}, '404': {description: none}}",
)

private val build: List<String> = operation(
    "post",
    BUILD,
    "requestBody: {content: {application/x-tar: {x-kapi-stream: bytes}}}",
    OK,
)

private fun operation(method: String, vararg fields: String): List<String> =
    paths("/pets: {$method: {${fields.joinToString()}}}") + pets

private fun answering(content: String, vararg more: String): List<String> =
    operation("get", EVENTS, *more, "responses: {'200': {description: ok, content: {$content}}}")

class TransferPlannerTest : FunSpec({
    context("a response that names how it streams") {
        test("is read as records although it is declared as JSON") {
            val kotlin = clientOf(answering("application/json: {schema: $PET, x-kapi-stream: records}"))

            kotlin shouldContain "public fun watchStream(): Flow<StreamEvent<PetstorePet>> {"
            kotlin shouldContain "return client.records(PetstorePet.serializer(), call)"
            kotlin shouldContain """accept("application/json")"""
        }

        test("is read as events when it says so") {
            clientOf(answering("application/json: {schema: $PET, x-kapi-stream: events}")) shouldContain
                "return client.events(PetstorePet.serializer(), call)"
        }

        test("is read as bytes whatever its schema") {
            val kotlin = clientOf(
                answering("application/x-tar: {schema: {type: string, format: binary}, x-kapi-stream: bytes}"),
            )

            kotlin shouldContain "public fun watchStream(): $BYTES {"
            kotlin shouldContain "return client.bytes(call)"
        }
    }

    context("a request body that streams its bytes") {
        test("is taken as a flow of chunks and sent as an upload") {
            val kotlin = clientOf(build)

            kotlin shouldContain "body: Flow<ByteString>"
            kotlin shouldContain """upload("application/x-tar", body)"""
        }

        test("is optional when the document does not require it") {
            clientOf(build) shouldContain """if (body != null) upload("application/x-tar", body)"""
        }

        test("is always sent when the document requires it") {
            val required = operation(
                "post",
                BUILD,
                "requestBody: {required: true, content: {application/x-tar: {x-kapi-stream: bytes}}}",
                OK,
            )
            val kotlin = clientOf(required)

            kotlin shouldContain """upload("application/x-tar", body)"""
            kotlin shouldNotContain "if (body != null)"
        }

        test("streams nothing but bytes") {
            val refused = operation(
                "post",
                BUILD,
                "requestBody: {content: {application/json: {x-kapi-stream: records}}}",
                OK,
            )

            clientGenerationOf(refused).codes() shouldBe listOf(CodegenCodes.EXTENSION_VALUE)
        }
    }

    context("an operation that upgrades its connection") {
        test("switches the protocol in its stream and writes the input after the switch") {
            val kotlin = clientOf(attach)

            kotlin shouldContain
                "public fun attachStream(stream: Boolean? = null, input: Flow<ByteString> = emptyFlow()): $BYTES {"
            kotlin shouldContain """upgrade("tcp", input)"""
        }

        test("does not switch the protocol in the function that reads the whole answer") {
            val kotlin = clientOf(attach)
            val whole = kotlin.substringAfter("public suspend fun attach(").substringBefore("public fun attachStream")

            whole shouldNotContain "upgrade("
        }

        test("needs a response whose bytes are streamed") {
            val plain = operation(
                "post",
                UPGRADED,
                "responses: {'101': {description: switched}}",
            )

            clientGenerationOf(plain).codes() shouldBe listOf(CodegenCodes.EXTENSION_VALUE)
        }

        test("refuses a parameter named after the input it writes") {
            val named = operation(
                "post",
                UPGRADED,
                "parameters: [{name: input, in: query, schema: {type: string}}]",
                "responses: {'101': {description: switched, content: {$RAW: {x-kapi-stream: bytes}}}}",
            )

            clientGenerationOf(named).codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
        }
    }

    context("a server") {
        test("is not generated for a connection that is upgraded") {
            val messages = serverGenerationOf(attach).diagnostics.map { diagnostic -> diagnostic.message }

            messages shouldContain "The generator has no Kotlin for a connection that is upgraded on a server yet"
        }

        test("is not generated for an upload") {
            val messages = serverGenerationOf(build).diagnostics.map { diagnostic -> diagnostic.message }

            messages shouldBe listOf("The generator has no Kotlin for a body that is streamed on a server yet")
        }
    }

    context("the keywords of a stream") {
        test("belong to a media type and name records, events or bytes") {
            val misplaced = operation("get", "$EVENTS, x-kapi-stream: bytes", OK)
            val unknown = answering("$RAW: {x-kapi-stream: lines}")

            clientGenerationOf(misplaced).codes() shouldBe listOf(CodegenCodes.EXTENSION_SITE)
            clientGenerationOf(unknown).codes() shouldBe listOf(CodegenCodes.EXTENSION_VALUE)
        }
    }
})
