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

package com.davils.kapi.codegen.core.emit.mock

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.fixture.DEFAULT_MOCK
import com.davils.kapi.codegen.core.fixture.LIST_PETS
import com.davils.kapi.codegen.core.fixture.SAMPLES
import com.davils.kapi.codegen.core.fixture.UNMATCHED
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.mockFileOf
import com.davils.kapi.codegen.core.fixture.mockGenerationOf
import com.davils.kapi.codegen.core.fixture.parameters
import com.davils.kapi.codegen.core.fixture.paths
import com.davils.kapi.codegen.core.fixture.pet
import com.davils.kapi.codegen.core.fixture.responses
import com.davils.kapi.codegen.core.fixture.schemas
import com.davils.kapi.codegen.core.options.Visibility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val RESPONSE: String = "PetstoreListPetsResponse"
private const val ANSWER: String = "override suspend fun listPets(): $RESPONSE = $RESPONSE"
private const val PLAIN: String = "description: ok"
private const val OK: String = "'200': {$PLAIN}"
private const val TEXT: String = "content: {application/json: {schema: {type: string}}}"
private const val PET: String = "content: {application/json: {schema: {\$ref: '#/components/schemas/Pet'}}}"
private const val PICTURE: String = "content: {image/png: {schema: {type: string, format: binary}}}"
private const val UNSAMPLED: String = "content: {application/json: {schema: $UNMATCHED}}"
private const val COUNT: String = "X-Total: {required: true, schema: {type: integer, format: int32}}"
private const val NEXT: String = "X-Next: {schema: {type: string}}"

class MockEmitterTest : FunSpec({
    context("the mock of a group") {
        test("is an open class in the testing package that implements the service") {
            val files = mockGenerationOf(
                responses(OK),
            ).shouldBeInstanceOf<Generation.Generated>()
            val paths = files.files.map { file -> file.path.substringAfter("petstore/") }
            val mock = mockFileOf(responses(OK))

            paths shouldContainAll listOf("testing/$DEFAULT_MOCK.kt", "testing/$SAMPLES.kt")
            mock shouldContain "package com.example.petstore.testing"
            mock shouldContain "public open class $DEFAULT_MOCK : PetstoreDefaultService {"
        }

        test("is internal together with its samples when the api is") {
            val generation = mockGenerationOf(
                responses(OK),
            ) { visibility = Visibility.Internal }
            val files = generation.shouldBeInstanceOf<Generation.Generated>().files
            val testing = files.filter { file -> "/testing/" in file.path }

            val kotlin = testing.joinToString("") { file -> file.content }

            kotlin shouldContain "internal open class $DEFAULT_MOCK"
            kotlin shouldContain "internal object $SAMPLES"
        }

        test("takes the parameters of the service without their defaults") {
            val mock = mockFileOf(parameters("name: limit, in: query, schema: {type: integer, format: int32}"))

            mock shouldContain "override suspend fun listPets(limit: Int?): $RESPONSE"
        }
    }

    context("the answer of a mock") {
        test("is the response without content as it stands") {
            mockFileOf(responses("'204': {$PLAIN}")) shouldContain "$ANSWER.NoContent\n"
        }

        test("is the first success the document declares") {
            mockFileOf(responses("'404': {$PLAIN}, '201': {$PLAIN}, '200': {$PLAIN}")) shouldContain "$ANSWER.Ok\n"
            mockFileOf(responses("'404': {$PLAIN}, 2XX: {$PLAIN}, '201': {$PLAIN}")) shouldContain "$ANSWER.Created\n"
        }

        test("is a successful range with its first status") {
            val mock = mockFileOf(responses("'404': {$PLAIN}, 2XX: {$PLAIN}, default: {$PLAIN}"))

            mock shouldContain "$ANSWER.Successful(\n        status = 200,\n    )"
        }

        test("is the default response with the status 200 when no success is declared") {
            val mock = mockFileOf(responses("'404': {$PLAIN}, default: {$PLAIN}"))

            mock shouldContain "$ANSWER.Other(\n        status = 200,\n    )"
        }

        test("is the first response when the document declares neither a success nor a default") {
            mockFileOf(responses("'503': {$PLAIN}, '404': {$PLAIN}")) shouldContain "$ANSWER.NotFound\n"
            mockFileOf(responses("5XX: {$PLAIN}")) shouldContain "$ANSWER.ServerError(\n        status = 500,\n    )"
        }

        test("carries the sample of its body") {
            val lines = responses("'200': {$PLAIN, $PET}") + schemas(pet("name: {type: string}"))

            mockFileOf(responses("'200': {$PLAIN, $TEXT}")) shouldContain "body = \"string\","
            mockFileOf(lines) shouldContain "body = $SAMPLES.pet,"
        }

        test("carries no bytes for a content that is no JSON") {
            mockFileOf(responses("'200': {$PLAIN, $PICTURE}")) shouldContain "content = ByteString(),"
        }

        test("carries a value for a header the response requires and none for the others") {
            val mock = mockFileOf(responses("'200': {$PLAIN, headers: {$COUNT, $NEXT}}"))

            mock shouldContain "xNext = null,"
            mock shouldContain "xTotal = 0,"
        }
    }

    context("an operation without a sample for its answer") {
        test("is left to a subclass when it declares no response at all") {
            val generation = mockGenerationOf(paths("/pets: {get: {$LIST_PETS, responses: {}}}"))

            generation.diagnostics.single().message shouldContain "because it declares no response"
        }

        test("is left to a subclass of a mock that is abstract, with a warning") {
            val lines = paths(
                "/pets: {get: {$LIST_PETS, responses: {'200': {$PLAIN, $UNSAMPLED}}}}",
                "/cats: {get: {operationId: listCats, responses: {'200': {$PLAIN}}}}",
            )
            val generation = mockGenerationOf(lines)
            val mock = mockFileOf(lines)

            generation.codes() shouldBe listOf(CodegenCodes.MOCK_UNANSWERED)
            generation.diagnostics.single().message shouldContain "The mock of listPets is left to a subclass"
            mock shouldContain "public abstract class $DEFAULT_MOCK"
            mock shouldContain "override suspend fun listCats()"
            mock shouldNotContain "fun listPets"
        }
    }
})
