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


package com.davils.kapi.codegen.core.emit.client

import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.fixture.DEFAULT_CLIENT
import com.davils.kapi.codegen.core.fixture.LIST_PETS
import com.davils.kapi.codegen.core.fixture.LIST_PETS_RESULT
import com.davils.kapi.codegen.core.fixture.OK
import com.davils.kapi.codegen.core.fixture.clientFilesOf
import com.davils.kapi.codegen.core.fixture.clientGenerationOf
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.get
import com.davils.kapi.codegen.core.fixture.paths
import com.davils.kapi.codegen.core.fixture.responses
import com.davils.kapi.codegen.core.fixture.schemas
import com.davils.kapi.codegen.core.options.Visibility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val PLAIN: String = "description: ok"

private const val FIELD: String = "        public val"

private const val MONEY: String = "{type: com.example.Money, serializer: com.example.MoneySerializer}"

private val serializers: Map<String, String> = mapOf(
    "{type: string}" to "String.serializer()",
    "{type: integer, format: int32}" to "Int.serializer()",
    "{type: boolean}" to "Boolean.serializer()",
    "{type: string, format: byte}" to "Base64Serializer",
    "{type: array, items: {type: string}}" to "ListSerializer(String.serializer())",
    "{type: object, additionalProperties: {type: integer}}" to "MapSerializer(String.serializer(), Long.serializer())",
    "{type: [string, 'null']}" to "String.serializer().nullable",
    "{type: array, items: {type: [boolean, 'null']}}" to "ListSerializer(Boolean.serializer().nullable)",
    "{\$ref: '#/components/schemas/Pet'}" to "PetstorePet.serializer()",
    "{type: string, x-kapi-type: kotlin.uuid.Uuid}" to "Uuid.serializer()",
    "{x-kapi-type: $MONEY}" to "client.decode(MoneySerializer, exchange)",
)

private fun body(schema: String): List<String> {
    val response = "'200': {$PLAIN, content: {application/json: {schema: $schema}}}"
    return responses(response) + schemas(listOf("Pet: {type: object, properties: {name: {type: string}}}"))
}

class ClientEmitterTest : FunSpec({
    context("the files of the clients") {
        test("are one client per group and one result per operation, in the order of their names") {
            val lines = paths(
                "/pets: {get: {$LIST_PETS, tags: [pets], $OK}, post: {operationId: addPet, tags: [pets], $OK}}",
                "/cats: {get: {operationId: listCats, tags: [cats], $OK}}",
            )

            clientFilesOf(lines) shouldBe listOf(
                "client/PetstoreCatsClient.kt",
                "client/PetstorePetsClient.kt",
                "operation/PetstoreAddPetResult.kt",
                "operation/PetstoreListCatsResult.kt",
                "operation/PetstoreListPetsResult.kt",
                "META-INF/kapi/openapi/petstore.provenance.json",
            )
        }
    }

    context("a client") {
        test("takes the client of the runtime and executes one call per operation") {
            val kotlin = clientOf(get(LIST_PETS))

            kotlin shouldContain "public class $DEFAULT_CLIENT(\n    private val client: Client,\n) {"
            kotlin shouldContain """        val call = apiCall("listPets", "GET") {"""
            kotlin shouldContain """            path("/pets")"""
            kotlin shouldContain "        return when (val exchange = client.execute(call)) {\n"
            kotlin shouldContain "            is Exchange.Failed -> $LIST_PETS_RESULT.Failed(exchange.failure)\n"
            kotlin shouldContain "            is Exchange.Answered -> listPetsResult(exchange)\n"
        }

        test("names where it was generated from") {
            val kotlin = clientOf(get(LIST_PETS))

            kotlin shouldContain """    from = "petstore.yaml","""
        }

        test("is as visible as the options say") {
            val generation = clientGenerationOf(get(LIST_PETS)) { visibility = Visibility.Internal }
            val files = generation.shouldBeInstanceOf<Generation.Generated>().files
            val kotlin = files.take(2).joinToString("") { file -> file.content }

            kotlin shouldContain "internal class PetstoreDefaultClient("
            kotlin shouldContain "internal suspend fun listPets(): $LIST_PETS_RESULT {"
            kotlin shouldContain "internal sealed interface $LIST_PETS_RESULT {"
            kotlin shouldContain "internal data class Failed(\n        internal val failure: CallFailure,"
            kotlin shouldNotContain "public"
        }
    }

    context("the result of an operation") {
        test("is one answer per response, and one each for what no response covers") {
            val content = "content: {application/json: {schema: {type: string}}}"
            val lines = responses("'200': {$PLAIN, $content}, '404': {$PLAIN}, default: {$PLAIN}")
            val kotlin = clientOf(lines, LIST_PETS_RESULT)

            kotlin shouldContain "public sealed interface $LIST_PETS_RESULT {"
            kotlin shouldContain "data class Ok(\n$FIELD body: String,\n$FIELD headers: Headers,"
            kotlin shouldContain "data class NotFound(\n$FIELD content: ByteString,\n$FIELD headers: Headers,"
            kotlin shouldContain "data class Other(\n$FIELD status: Int,\n$FIELD content: ByteString,"
            kotlin shouldContain "data class Unexpected(\n$FIELD status: Int,\n$FIELD headers: Headers,"
            kotlin shouldContain "data class Malformed(\n$FIELD status: Int,\n$FIELD reason: String,"
            kotlin shouldContain "data class Failed(\n$FIELD failure: CallFailure,"
        }
    }

    context("the serializer of a body") {
        serializers.forEach { (schema, serializer) ->
            test("is $serializer for $schema") {
                clientOf(body(schema)) shouldContain serializer
            }
        }
    }
})
