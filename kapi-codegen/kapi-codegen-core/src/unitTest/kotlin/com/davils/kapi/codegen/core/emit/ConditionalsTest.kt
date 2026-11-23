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

package com.davils.kapi.codegen.core.emit

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.fixture.LIST_PETS_RESULT
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.paths
import com.davils.kapi.codegen.core.fixture.responses
import com.davils.kapi.codegen.core.fixture.serverGenerationOf
import com.davils.kapi.codegen.core.fixture.serverOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val TEXT: String = "schema: {type: string}"

private const val ETAG: String = "headers: {ETag: {required: true, $TEXT}}"

private const val MODIFIED: String = "headers: {Last-Modified: {$TEXT}}"

private const val JSON: String = "content: {application/json: {$TEXT}}"

private const val VALIDATORS: String = "validators: Validators? = null"

class ConditionalsTest : FunSpec({
    context("an operation that reads a representation with validators") {
        test("takes the validators of a copy the client holds and reads a 304 as NotModified") {
            val kotlin = clientOf(responses("'200': {description: ok, $JSON, $ETAG}"))
            val result = clientOf(responses("'200': {description: ok, $JSON, $ETAG}"), LIST_PETS_RESULT)

            kotlin shouldContain "listPets($VALIDATORS)"
            kotlin shouldContain "if (validators != null) revalidate(validators)"
            kotlin shouldContain "304 to { exchange -> $LIST_PETS_RESULT.NotModified(Validators.of(exchange.headers),"
            result shouldContain "public data class NotModified(\n        public val validators: Validators,"
        }

        test("keeps the response the document declares for 304 instead of one of its own") {
            val lines = responses("'200': {description: ok, $JSON, $MODIFIED}, '304': {description: same}")
            val kotlin = clientOf(lines)

            kotlin shouldContain "listPets($VALIDATORS)"
            kotlin shouldContain
                "304 to { exchange -> $LIST_PETS_RESULT.NotModified(exchange.content, exchange.headers) },"
        }

        test("is answered by the server through the preconditions of the request") {
            val kotlin = serverOf(responses("'200': {description: ok, $JSON, $MODIFIED}"))

            kotlin shouldContain "request.conditional(RepresentationState(entityTag = null, lastModified = " +
                "response.lastModified?.let { text -> HttpDate.parseOrNull(text) })) {"
        }

        test("is no conditional request when it changes the representation") {
            val lines = paths("/pets: {put: {operationId: putPet, responses: {'200': {description: ok, $ETAG}}}}")

            clientOf(lines) shouldNotContain "validators"
            serverOf(lines) shouldNotContain "conditional"
        }
    }

    context("a response the server writes") {
        test("streams JSON text sequences as records of their own framing") {
            val lines = responses("'200': {description: ok, content: {application/json-seq: {$TEXT}}}")

            clientOf(lines) shouldContain "client.records(String.serializer(), call, RecordFraming.JsonSequence)"
            serverOf(lines) shouldContain "MediaType.parse(\"application/json-seq\"))"
        }

        test("is refused on a server when a header has a name that HTTP does not allow") {
            listOf("'X (bad)'", "X-\u00C4").forEach { name ->
                val lines = responses("'200': {description: ok, headers: {$name: {$TEXT}}}")

                serverGenerationOf(lines).codes() shouldBe listOf(CodegenCodes.OPERATION_UNSUPPORTED)
            }
        }
    }
})
