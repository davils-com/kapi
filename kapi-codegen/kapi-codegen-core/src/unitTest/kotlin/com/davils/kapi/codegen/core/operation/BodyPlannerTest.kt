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
import com.davils.kapi.codegen.core.fixture.clientGenerationOf
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.get
import com.davils.kapi.codegen.core.fixture.schemas
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val TEXT: String = "{schema: {type: string}}"

private const val JSON: String = "application/json: $TEXT"

private const val BYTES: String = "application/octet-stream: {schema: {type: string, format: binary}}"

private const val FORM: String = "application/x-www-form-urlencoded"

private const val PARTS: String = "multipart/form-data"

private const val FLAT: String = "{schema: {type: object, properties: {a: {type: string}}}}"

private const val ACCOUNT: String =
    "Account: {type: object, properties: {id: {type: string, readOnly: true}, email: {type: string}}}"

private val whole: Map<String, String> = mapOf(
    "requestBody: {required: true, content: {$JSON}}" to
        "            body(\"application/json\", client.json(String.serializer(), body))",
    "requestBody: {content: {$JSON}}" to
        "if (body != null) body(\"application/json\", client.json(String.serializer(), body))",
    "requestBody: {required: true, content: {application/merge-patch+json: $TEXT}}" to
        "body(\"application/merge-patch+json\", client.json(String.serializer(), body))",
    "requestBody: {required: true, content: {'application/json; charset=utf-8': $TEXT}}" to
        "            body(\"application/json\", client.json(String.serializer(), body))",
    "requestBody: {required: true, content: {application/json: {}}}" to
        "body(\"application/json\", client.json(JsonElement.serializer(), body))",
    "requestBody: {content: {application/json: {}}}" to "listPets(body: JsonElement? = null)",
    "requestBody: {required: true, content: {application/json: {schema: {type: [string, 'null']}}}}" to
        "listPets(body: String? = null)",
    "requestBody: {required: true, content: {application/json: {schema: {type: object, properties: {a: {}}}}}}" to
        "listPets(body: PetstoreListPetsRequest)",
    "requestBody: {required: true, content: {$BYTES}}" to "            body(\"application/octet-stream\", body)",
    "requestBody: {required: true, content: {image/*: $TEXT}}" to "body(\"application/octet-stream\", body)",
    "requestBody: {content: {text/plain: $TEXT}}" to "if (body != null) body(\"text/plain\", body)",
)

private val preferred: Map<String, String> = mapOf(
    "text/plain: $TEXT, $FORM: $FLAT, $PARTS: $FLAT, $JSON" to "client.json(String.serializer(), body)",
    "text/plain: $TEXT, $FORM: $FLAT, $PARTS: $FLAT" to "multipart(\"multipart/form-data\") {",
    "text/plain: $TEXT, $FORM: $FLAT" to "            form {",
)

private fun body(content: String): List<String> = get(LIST_PETS, "requestBody: {required: true, content: {$content}}")

class BodyPlannerTest : FunSpec({
    context("the body of a request") {
        whole.forEach { (body, code) ->
            test("is sent as the document declares it: $body") {
                clientOf(get(LIST_PETS, body)) shouldContain code
            }
        }

        test("is bytes when it is no JSON, no form and no parts") {
            clientOf(body(BYTES)) shouldContain "listPets(body: ByteString)"
        }

        test("comes after what is required and before what is optional") {
            val parameter = "parameters: [{name: limit, in: query, schema: {type: string}}]"
            val kotlin = clientOf(get(LIST_PETS, parameter, "requestBody: {required: true, content: {$JSON}}"))

            kotlin shouldContain "listPets(body: String, limit: String? = null)"
        }

        test("is the shape to write of a model that is read differently") {
            val content = "application/json: {schema: {\$ref: '#/components/schemas/Account'}}"
            val kotlin = clientOf(body(content) + schemas(listOf(ACCOUNT)))

            kotlin shouldContain "listPets(body: PetstoreAccountWrite)"
            kotlin shouldContain "client.json(PetstoreAccountWrite.serializer(), body)"
        }

        test("is taken from the request body the components declare") {
            val lines = get(LIST_PETS, "requestBody: {\$ref: '#/components/requestBodies/Text'}") +
                listOf("components:", "  requestBodies:", "    Text: {required: true, content: {$JSON}}")

            clientOf(lines) shouldContain "listPets(body: String)"
        }

        test("is none when the document declares no media type for it") {
            clientOf(get(LIST_PETS, "requestBody: {content: {}}")) shouldContain "listPets()"
        }
    }

    context("the media type of a body") {
        preferred.forEach { (content, code) ->
            test("is JSON before parts, parts before a form and a form before bytes: $content") {
                clientOf(body(content)) shouldContain code
            }
        }
    }

    context("a body the generator cannot write") {
        test("is refused as a form without properties") {
            clientGenerationOf(body("$FORM: $TEXT")).codes() shouldBe
                listOf(CodegenCodes.OPERATION_UNSUPPORTED)
        }
    }
})
