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
import com.davils.kapi.codegen.core.fixture.LIST_PETS_RESULT
import com.davils.kapi.codegen.core.fixture.OK
import com.davils.kapi.codegen.core.fixture.clientGenerationOf
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.parameters
import com.davils.kapi.codegen.core.fixture.paths
import com.davils.kapi.codegen.core.fixture.schemas
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val TEXT: String = "schema: {type: string}"

private const val NUMBER: String = "schema: {type: integer, format: int32}"

private const val FORMAT: String = "ParameterFormat"

private const val STYLE: String = "ParameterStyle"

private const val VALUE: String = "ParameterValue"

private const val FILTER_FIELDS: String = "client.fields(PetstoreListPetsFilter.serializer(), filter)"

private const val NO_PARAMETER: String = "public suspend fun listPets(): PetstoreListPetsResult {"

private val placed: Map<String, String> = mapOf(
    "name: limit, in: query, $NUMBER" to
        "if (limit != null) query($FORMAT(\"limit\", $STYLE.Form, true), $VALUE.Text(limit.toString()))",
    "name: limit, in: query, required: true, $NUMBER" to
        "            query($FORMAT(\"limit\", $STYLE.Form, true), $VALUE.Text(limit.toString()))",
    "name: X-Trace, in: header, $TEXT" to
        "if (xTrace != null) header($FORMAT(\"X-Trace\", $STYLE.Simple, false), $VALUE.Text(xTrace))",
    "name: session, in: cookie, $TEXT" to
        "if (session != null) cookie($FORMAT(\"session\", $STYLE.Form, true), $VALUE.Text(session))",
    "name: tag, in: query, style: pipeDelimited, explode: false, schema: {type: array, items: {type: string}}" to
        "if (tag != null) query($FORMAT(\"tag\", $STYLE.PipeDelimited, false), $VALUE.Items(tag))",
    "name: id, in: query, schema: {type: array, items: {type: integer}}" to
        "if (id != null) query($FORMAT(\"id\", $STYLE.Form, true), $VALUE.Items(id.map { it.toString() }))",
    "name: id, in: query, schema: {type: string, x-kapi-type: kotlin.uuid.Uuid}" to "$VALUE.Text(id.toString())",
    "name: state, in: query, schema: {type: string, enum: [open, closed]}" to "$VALUE.Text(state.toString())",
    "name: page-size, in: query, x-kapi-name: size, $NUMBER" to
        "if (size != null) query($FORMAT(\"page-size\", $STYLE.Form, true), $VALUE.Text(size.toString()))",
)

private val objects: Map<String, String> = mapOf(
    "name: filter, in: query, schema: {type: object, properties: {a: {type: string}}}" to
        "if (filter != null) query($FORMAT(\"filter\", $STYLE.Form, true), $FILTER_FIELDS)",
    "name: filter, in: query, style: deepObject, schema: {type: object, properties: {a: {type: string}}}" to
        "if (filter != null) query($FORMAT(\"filter\", $STYLE.DeepObject, false), $FILTER_FIELDS)",
    "name: filter, in: query, required: true, schema: {type: object, additionalProperties: {type: string}}" to
        "client.fields(MapSerializer(String.serializer(), String.serializer()), filter)",
)

private val unsupported: List<String> = listOf(
    "name: filter, in: query, schema: {type: object, properties: {a: {type: object, properties: {b: {}}}}}",
    "name: filter, in: query, schema: {type: object, additionalProperties: {type: array, items: {type: string}}}",
    "name: filter, in: query, schema: {type: array, items: {type: object, properties: {a: {type: string}}}}",
    "name: filter, in: query, content: {application/json: {schema: {type: string}}}",
    "name: filter, in: query, schema: {type: array, items: {type: [string, 'null']}}",
    "name: filter, in: query, schema: {type: string, format: byte}",
    "name: filter, in: query, schema: {}",
)

class ParameterPlannerTest : FunSpec({
    context("a parameter") {
        placed.forEach { (parameter, code) ->
            test("is placed where the document says: $parameter") {
                clientOf(parameters(parameter)) shouldContain code
            }
        }

        test("is a parameter of the function, with what is required first") {
            val kotlin = clientOf(parameters("name: a, in: query, $TEXT", "name: b, in: query, required: true, $TEXT"))

            kotlin shouldContain "public suspend fun listPets(b: String, a: String? = null): $LIST_PETS_RESULT {"
        }

        test("that is required stays optional in Kotlin when its schema allows null") {
            val schema = "schema: {type: [string, 'null']}"

            clientOf(parameters("name: filter, in: query, required: true, $schema")) shouldContain
                "listPets(filter: String? = null)"
        }

        test("of the operation replaces the one its path declares under the same name") {
            val lines = paths(
                "/pets:",
                "  parameters: [{name: limit, in: query, $TEXT}, {name: X-Trace, in: header, $TEXT}]",
                "  get: {operationId: listPets, parameters: [{name: limit, in: query, $NUMBER}], $OK}",
            )

            clientOf(lines) shouldContain "listPets(limit: Int? = null, xTrace: String? = null)"
        }

        test("named by a schema of the components takes its type") {
            val lines = parameters("name: state, in: query, schema: {\$ref: '#/components/schemas/Pet'}") +
                schemas(listOf("Pet: {type: string, enum: [open, closed]}"))

            clientOf(lines) shouldContain "state: PetstorePet? = null"
        }
    }

    context("a parameter that is an object") {
        objects.forEach { (parameter, code) ->
            test("is written as its fields: $parameter") {
                clientOf(parameters(parameter)) shouldContain code
            }
        }

        test("named by a schema of the components is written with the serializer of that model") {
            val lines = parameters("name: filter, in: query, schema: {\$ref: '#/components/schemas/Pet'}") +
                schemas(listOf("Pet: {type: object, properties: {name: {type: string}, age: {type: integer}}}"))

            clientOf(lines) shouldContain "client.fields(PetstorePet.serializer(), filter))"
        }
    }

    context("a parameter of the path") {
        test("is written where its place is, between the text of the path") {
            val lines = paths(
                "/pets/{petId}/toys/{toyId}/parts:",
                "  parameters: [{name: petId, in: path, required: true, $TEXT}]",
                "  get: {operationId: listPets, parameters: [{name: toyId, in: path, required: true, $NUMBER}], $OK}",
            )
            val kotlin = clientOf(lines)

            kotlin shouldContain "listPets(petId: String, toyId: Int)"
            kotlin shouldContain listOf(
                "            path(\"/pets/\")",
                "            path($FORMAT(\"petId\", $STYLE.Simple, false), $VALUE.Text(petId))",
                "            path(\"/toys/\")",
                "            path($FORMAT(\"toyId\", $STYLE.Simple, false), $VALUE.Text(toyId.toString()))",
                "            path(\"/parts\")",
            ).joinToString("\n")
        }
    }

    context("a header the call sets itself") {
        listOf("Accept", "content-type", "Authorization").forEach { header ->
            test("is left out with a warning: $header") {
                val lines = parameters("name: $header, in: header, $TEXT")
                val generation = clientGenerationOf(lines)

                generation.shouldBeInstanceOf<Generation.Generated>()
                generation.codes() shouldBe listOf(CodegenCodes.PARAMETER_RESERVED_HEADER)
                clientOf(lines) shouldContain NO_PARAMETER
            }
        }

        test("is a parameter like any other outside the headers") {
            clientOf(parameters("name: accept, in: query, $TEXT")) shouldContain "accept: String? = null"
        }
    }

    context("a name the generated client uses itself") {
        listOf("body", "call", "client", "exchange", "it").forEach { name ->
            test("is refused for a parameter: $name") {
                val generation = clientGenerationOf(parameters("name: $name, in: query, $TEXT"))

                generation.codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
            }
        }

        test("is free again once x-kapi-name names the parameter") {
            val kotlin = clientOf(parameters("name: body, in: query, x-kapi-name: text, $TEXT"))

            kotlin shouldContain "text: String? = null"
            kotlin shouldContain "$FORMAT(\"body\", $STYLE.Form, true)"
        }
    }

    context("two parameters") {
        test("are refused when they become one name") {
            val generation = clientGenerationOf(
                parameters("name: pet-id, in: query, $TEXT", "name: petId, in: query, $TEXT"),
            )

            generation.codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
        }
    }

    context("a sensitive parameter") {
        test("needs no finding, because a call never renders what it carries") {
            val generation = clientGenerationOf(parameters("name: token, in: query, x-kapi-sensitive: true, $TEXT"))

            generation.shouldBeInstanceOf<Generation.Generated>()
            generation.codes().shouldBeEmpty()
        }
    }

    context("a parameter that is no text, list or flat object") {
        unsupported.forEach { parameter ->
            test("is refused: $parameter") {
                val generation = clientGenerationOf(parameters(parameter))

                generation.shouldBeInstanceOf<Generation.Refused>()
                generation.codes() shouldBe listOf(CodegenCodes.OPERATION_UNSUPPORTED)
            }
        }
    }
})
