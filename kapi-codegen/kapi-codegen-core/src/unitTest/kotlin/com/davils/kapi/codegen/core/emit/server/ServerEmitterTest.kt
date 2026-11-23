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

package com.davils.kapi.codegen.core.emit.server

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.fixture.LIST_PETS
import com.davils.kapi.codegen.core.fixture.OK
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.get
import com.davils.kapi.codegen.core.fixture.parameters
import com.davils.kapi.codegen.core.fixture.paths
import com.davils.kapi.codegen.core.fixture.responses
import com.davils.kapi.codegen.core.fixture.schemas
import com.davils.kapi.codegen.core.fixture.serverFilesOf
import com.davils.kapi.codegen.core.fixture.serverGenerationOf
import com.davils.kapi.codegen.core.fixture.serverOf
import com.davils.kapi.codegen.core.options.Visibility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val SERVICE: String = "PetstoreDefaultService"

private const val RESPONSE: String = "PetstoreListPetsResponse"

private const val TEXT: String = "schema: {type: string}"

private const val JSON: String = "content: {application/json: {schema: {type: string}}}"

private const val FORMAT: String = "ParameterFormat"

private const val STYLE: String = "ParameterStyle"

private const val INPUT: String = "input.value(Place"

private const val NUMBER: String = "ParameterType(Long.serializer(), false)"

private val inputs: Map<String, String> = mapOf(
    "name: limit, in: query, schema: {type: integer, format: int32}" to
        "$INPUT.Query, $FORMAT(\"limit\", $STYLE.Form, true), ParameterType(Int.serializer(), false), false)",
    "name: limit, in: query, required: true, $TEXT" to
        "$INPUT.Query, $FORMAT(\"limit\", $STYLE.Form, true), ParameterType(String.serializer(), true), true)",
    "name: X-Trace, in: header, $TEXT" to
        "$INPUT.Header, $FORMAT(\"X-Trace\", $STYLE.Simple, false), ParameterType(String.serializer(), true), false)",
    "name: sid, in: cookie, $TEXT" to "$INPUT.Cookie, $FORMAT(\"sid\", $STYLE.Form, true)",
    "name: open, in: query, schema: {type: boolean}" to "ParameterType(Boolean.serializer(), false), false)",
    "name: state, in: query, schema: {type: string, enum: [a, b]}" to
        "ParameterType(PetstoreListPetsState.serializer(), true), false)",
    "name: level, in: query, schema: {type: integer, enum: [1, 2]}" to
        "ParameterType(PetstoreListPetsLevel.serializer(), false), false)",
    "name: tag, in: query, schema: {type: array, items: {type: integer}}" to
        "input.items(Place.Query, $FORMAT(\"tag\", $STYLE.Form, true), $NUMBER, false)",
)

private const val MULTIPART: String =
    "requestBody: {content: {multipart/form-data: {schema: {type: object, properties: "

private val refused: Map<String, List<String>> = mapOf(
    "a parameter that is a map spread over the query" to
        parameters("name: filter, in: query, schema: {type: object, additionalProperties: {type: string}}"),
    "parts that are texts but no strings" to get(LIST_PETS, "$MULTIPART{a: {type: array, items: {type: integer}}}}}}}"),
)

class ServerEmitterTest : FunSpec({
    context("the files of a server") {
        test("are a service and its routes per group and one response per operation") {
            val lines = paths(
                "/pets: {get: {$LIST_PETS, tags: [pets], $OK}, post: {operationId: addPet, tags: [pets], $OK}}",
                "/cats: {get: {operationId: listCats, tags: [cats], $OK}}",
            )

            serverFilesOf(lines) shouldBe listOf(
                "server/PetstoreCatsRoutes.kt",
                "server/PetstoreCatsService.kt",
                "server/PetstorePetsRoutes.kt",
                "server/PetstorePetsService.kt",
                "operation/PetstoreAddPetResponse.kt",
                "operation/PetstoreListCatsResponse.kt",
                "operation/PetstoreListPetsResponse.kt",
                "META-INF/kapi/openapi/petstore.provenance.json",
            )
        }

        test("are as visible as the options say") {
            val generation = serverGenerationOf(get(LIST_PETS)) { visibility = Visibility.Internal }
            val files = generation.shouldBeInstanceOf<Generation.Generated>().files
            val kotlin = files.dropLast(1).joinToString("") { file -> file.content }

            kotlin shouldContain "internal class PetstoreDefaultRoutes("
            kotlin shouldContain "override val operations: List<Operation>"
            kotlin shouldContain "internal interface $SERVICE {"
            kotlin shouldContain "internal sealed interface $RESPONSE {"
            kotlin shouldNotContain "public class"
            kotlin shouldNotContain "public interface"
        }
    }

    context("the service of a group") {
        test("has one function per operation that takes what the operation takes and answers with its response") {
            val limit = "parameters: [{name: limit, in: query, $TEXT}]"
            val lines = get(LIST_PETS, limit, "requestBody: {required: true, $JSON}")
            val kotlin = serverOf(lines, SERVICE)

            kotlin shouldContain "public interface $SERVICE {"
            kotlin shouldContain "public suspend fun listPets(body: String, limit: String?): $RESPONSE"
        }
    }

    context("the routes of a group") {
        test("bind each operation with its method, its path and the handler that serves it") {
            val kotlin = serverOf(get(LIST_PETS))

            kotlin shouldContain "private val service: $SERVICE,\n) : Routes {"
            kotlin shouldContain "listOf(\n        Operation(\n"
            kotlin shouldContain """name = "listPets","""
            kotlin shouldContain """method = "GET","""
            kotlin shouldContain """path = "/pets","""
            kotlin shouldContain "security = listOf(),\n            handler = ::listPets,"
            kotlin shouldContain "private suspend fun listPets(request: Request): Reply {"
        }

        test("name the schemes, where their credentials are and the scopes an operation requires") {
            val schemes = "oauth: {type: http, scheme: bearer}, key: {type: apiKey, name: X-Key, in: header}"
            val lines = get(LIST_PETS, "security: [{oauth: [read, write], key: []}, {}]") +
                listOf("components:", "  securitySchemes: {$schemes}")
            val kotlin = serverOf(lines)

            kotlin shouldContain """SchemeUse("oauth", CredentialSource.Bearer, setOf("read", "write"))"""
            kotlin shouldContain """SchemeUse("key", CredentialSource.Header("X-Key"), setOf())"""
            kotlin shouldContain "), listOf()),"
        }

        test("require the certificate of the connection for mutual TLS instead of letting anyone in") {
            val lines = get(LIST_PETS, "security: [{tls: []}]") +
                listOf("components:", "  securitySchemes: {tls: {type: mutualTLS}}")
            val use = """SchemeUse("tls", CredentialSource.Certificate, setOf())"""
            val kotlin = serverOf(lines)

            kotlin shouldContain "security = listOf(listOf($use)),"
            kotlin shouldNotContain "security = listOf(listOf()),"
        }
    }

    context("the input of an operation") {
        inputs.forEach { (parameter, code) ->
            test("is read as the document declares it: $parameter") {
                serverOf(parameters(parameter)) shouldContain code
            }
        }

        test("is refused as a whole before the service is asked, and what is required is there after") {
            val lines = get(
                LIST_PETS,
                "parameters: [{name: a, in: query, required: true, $TEXT}, {name: b, in: query, $TEXT}]",
                "requestBody: {required: true, $JSON}",
            )
            val kotlin = serverOf(lines)

            kotlin shouldContain
                "val body = input.body(String.serializer(), true, listOf(MediaRange.of(\"application/json\")))"
            kotlin shouldContain "if (!input.isValid || a == null || body == null) return input.refused()"
            kotlin shouldContain "service.listPets(a = a, b = b, body = body)"
        }

        test("is checked for validity alone when nothing is required") {
            serverOf(get(LIST_PETS)) shouldContain "if (!input.isValid) return input.refused()"
        }

        test("has an optional body, and bytes for what is no JSON") {
            val bytes = "requestBody: {content: {application/octet-stream: {schema: {type: string, format: binary}}}}"

            serverOf(
                get(LIST_PETS, "requestBody: {$JSON}"),
            ) shouldContain "input.body(String.serializer(), false, listOf("
            serverOf(get(LIST_PETS, bytes)) shouldContain
                "val body = input.bytes(false, listOf(MediaRange.of(\"application/octet-stream\")))"
        }
    }

    context("what a server reads beyond single values") {
        test("reads an object from the fields the model declares") {
            val lines = parameters("name: filter, in: query, schema: {type: object, properties: {a: {type: string}}}")

            serverOf(
                lines,
            ) shouldContain """input.fields(Place.Query, ParameterFormat("filter", ParameterStyle.Form, """ +
                """true), PetstoreListPetsFilter.serializer(), false, setOf("a"))"""
        }

        test("reads a form into a class of its own and passes its fields") {
            val form = "{type: object, required: [a], properties: {a: {type: string}, b: {type: object, " +
                "properties: {c: {type: string}}}}}"
            val kotlin = serverOf(
                get(LIST_PETS, "requestBody: {content: {application/x-www-form-urlencoded: {schema: $form}}}"),
            )

            kotlin shouldContain "private class ListPetsForm("
            kotlin shouldContain "val form = input.body(ListPetsForm.serializer(), true,"
            kotlin shouldContain "val b = form?.b?.let { text -> Codecs.Default.decode("
            kotlin shouldContain "service.listPets(a = form.a, b = b?.value)"
        }

        test("reads the parts of a multipart body by their names") {
            val parts = "{f: {type: string, format: binary}, n: {type: integer}, t: {type: string}}, required: [f]"
            val kotlin = serverOf(get(LIST_PETS, "$MULTIPART$parts}}}}"))

            kotlin shouldContain "val parts = input.parts(true)"
            kotlin shouldContain "val f = parts?.firstOrNull { part -> part.name == \"f\" }?.let { part -> FilePart("
            kotlin shouldContain "Codecs.Default.decode(Long.serializer(), part.content, \"text/plain\")"
            kotlin shouldContain "f == null || (n != null && n !is Decoding.Decoded)"
        }

        test("reads repeated parts as texts and a form whose fields are all optional") {
            val tags = serverOf(get(LIST_PETS, "$MULTIPART{tags: {type: array, items: {type: string}}}}}}}"))
            val form = "{type: object, properties: {a: {type: string}}}"
            val optional = serverOf(
                get(LIST_PETS, "requestBody: {content: {application/x-www-form-urlencoded: {schema: $form}}}"),
            )

            tags shouldContain "val tags = parts?.filter { part -> part.name == \"tags\" }?.map { part ->"
            optional shouldContain "service.listPets(a = form.a)"
        }

        test("streams the bytes of a response as they come") {
            val bytes = "'200': {description: ok, content: {application/x-tar: {x-kapi-stream: bytes}}}"

            serverOf(responses(bytes)) shouldContain "stream(MediaType.parse(\"application/x-tar\"), response.items)"
        }

        test("streams the items of a response") {
            val events = "'200': {description: ok, content: {text/event-stream: {schema: {type: string}}}}"
            val lines = "'200': {description: ok, content: {application/x-ndjson: {schema: {type: string}}}}"

            serverOf(responses(events)) shouldContain "events {\n"
            serverOf(responses(lines)) shouldContain "lines(String.serializer(), response.items, MediaType.parse("
        }
    }

    context("the response of an operation") {
        test("is one answer per declared response, an object when it carries nothing") {
            val headers = "headers: {ETag: {required: true, $TEXT}, X-Count: {schema: {type: integer}}}"
            val absent = "{description: no}"
            val lines = responses("'200': {description: ok, $JSON, $headers}, '404': $absent, 5XX: $absent")
            val kotlin = serverOf(lines, RESPONSE)

            kotlin shouldContain "public data class Ok(\n        public val body: String,"
            kotlin shouldContain "public val eTag: String,"
            kotlin shouldContain "public val xCount: Long?,"
            kotlin shouldContain "public data object NotFound : $RESPONSE"
            kotlin shouldContain "public data class ServerError(\n        public val status: Int,"
        }

        test("carries the bytes of a body that is no JSON") {
            val lines = responses("'200': {description: ok, content: {image/png: {}}}")

            serverOf(lines, RESPONSE) shouldContain "public data class Ok(\n        public val content: ByteString,"
        }

        test("is replied with its status, its headers and its body") {
            val headers = "headers: {ETag: {required: true, $TEXT}, X-Count: {schema: {type: integer}}, X-Max: " +
                "{required: true, schema: {type: integer}}}"
            val lines = responses("'200': {description: ok, $JSON, $headers}, '404': {description: no}")
            val kotlin = serverOf(lines)

            kotlin shouldContain "return when (val response = service.listPets()) {"
            kotlin shouldContain
                "is $RESPONSE.Ok -> request.conditional(RepresentationState(entityTag = EntityTag.parseOrNull("
            kotlin shouldContain "reply(200) {"
            kotlin shouldContain "header(\"ETag\", response.eTag)"
            kotlin shouldContain "header(\"X-Count\", response.xCount?.toString())"
            kotlin shouldContain "header(\"X-Max\", response.xMax.toString())"
            kotlin shouldContain "body(MediaType.parse(\"application/json\"), String.serializer(), response.body)"
            kotlin shouldContain "is $RESPONSE.NotFound -> reply(404)"
        }

        test("is replied with the status the service chose for a range or the default, and with bytes as they are") {
            val lines = responses("4XX: {description: no, content: {text/plain: {}}}, default: {description: other}")
            val kotlin = serverOf(lines)

            kotlin shouldContain "is $RESPONSE.ClientError -> reply(response.status) {"
            kotlin shouldContain "bytes(MediaType.parse(\"text/plain\"), response.content)"
            kotlin shouldContain "is $RESPONSE.Other -> reply(response.status)"
        }
    }

    context("what a server cannot serve yet") {
        refused.forEach { (what, lines) ->
            test("is refused: $what") {
                val generation = serverGenerationOf(lines)

                generation.shouldBeInstanceOf<Generation.Refused>()
                generation.codes() shouldBe listOf(CodegenCodes.OPERATION_UNSUPPORTED)
            }
        }

        listOf("input", "request", "response", "service", "form", "parts", "reply").forEach { name ->
            test("is a parameter under a name the generated server uses itself: $name") {
                serverGenerationOf(parameters("name: $name, in: query, $TEXT")).codes() shouldBe
                    listOf(CodegenCodes.NAME_COLLISION)
            }
        }

        test("is served once the parameter has another name") {
            val kotlin = serverOf(parameters("name: request, in: query, x-kapi-name: asked, $TEXT"), SERVICE)

            kotlin shouldContain "listPets(asked: String?)"
        }

        test("is an operation under the name of a member of the routes") {
            serverGenerationOf(get("operationId: operations")).codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
        }
    }

    context("a document with operations and models alone") {
        test("has no server and no client") {
            serverFilesOf(schemas(listOf("Pet: {type: object, properties: {a: {type: string}}}"))) shouldBe
                listOf("model/PetstorePet.kt", "META-INF/kapi/openapi/petstore.provenance.json")
        }
    }
})
