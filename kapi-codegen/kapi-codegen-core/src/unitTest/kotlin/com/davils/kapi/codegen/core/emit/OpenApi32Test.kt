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
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.generator
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val LINE: String = "\n"

private const val PETS: String = "  /pets:"

private const val CLIENT: String = "PetstoreDefaultClient"

private const val ROUTES: String = "PetstoreDefaultRoutes"

private val uri: DocumentUri = DocumentUri("file:/work/specs/petstore.yaml")

private const val OBJECT: String = "{type: object, required: [name], properties: {name: {type: string}}}"

private val head: List<String> = listOf("openapi: 3.2.0", "info: {title: Petstore, version: 1.0.0}", "paths:")

private fun document(vararg lines: String): String = (head + lines).joinToString(LINE, postfix = LINE)

private fun searched(content: String): String = document(
    PETS,
    "    query:",
    "      operationId: searchPets",
    "      parameters:",
    "        - {name: criteria, in: querystring, content: {$content: {schema: $OBJECT}}}",
    "      responses:",
    "        '200':",
    "          description: ok",
    "          content: {text/event-stream: {itemSchema: $OBJECT}}",
)

private fun custom(method: String): String = document(
    PETS,
    "    get: {operationId: listPets, responses: {'200': {description: ok}}}",
    "    additionalOperations:",
    "      '$method': {operationId: copyPet, responses: {'204': {description: ok}}}",
)

private suspend fun generationOf(text: String): Generation {
    val reading = OpenApi.reader().read(Source.Text(text, uri))
    val read = reading.shouldBeInstanceOf<Reading.Read>()
    val generator = generator { artifacts = setOf(Artifact.Models, Artifact.Client, Artifact.Server) }
    return generator.generate(read.document, read.sources)
}

private suspend fun kotlinOf(text: String, name: String): String {
    val generated = generationOf(text).shouldBeInstanceOf<Generation.Generated>()
    return generated.files.first { file -> file.path.endsWith("/$name.kt") }.content
}

class OpenApi32Test : FunSpec({
    context("an operation of the QUERY method with the whole query string as one parameter") {
        val text = searched("application/x-www-form-urlencoded")

        test("is called with its method and writes the fields of the object as the query") {
            val kotlin = kotlinOf(text = text, name = CLIENT)

            kotlin shouldContain """apiCall("searchPets", "QUERY")"""
            kotlin shouldContain """query(ParameterFormat("criteria", ParameterStyle.Form, true), client.fields("""
        }

        test("is served with its method and reads the whole query string") {
            val kotlin = kotlinOf(text = text, name = ROUTES)

            kotlin shouldContain """method = "QUERY","""
            kotlin shouldContain "input.queryString(MediaType.parse(\"application/x-www-form-urlencoded\"), " +
                "PetstoreSearchPetsCriteria.serializer(), false)"
        }

        test("reads the items of a stream by the schema of its items") {
            kotlinOf(text = text, name = CLIENT) shouldContain "client.events(PetstoreSearchPetsOkItem.serializer()"
        }

        test("is sent and read as JSON for a query string of JSON, with its parameters") {
            val text = searched("'application/json; charset=utf-8'")
            val client = kotlinOf(text = text, name = CLIENT)
            val routes = kotlinOf(text = text, name = ROUTES)

            client shouldContain "queryString(client.json(PetstoreSearchPetsCriteria.serializer(), criteria)" +
                ".decodeToString())"
            routes shouldContain "input.queryString(MediaType.parse(\"application/json; charset=utf-8\"), " +
                "PetstoreSearchPetsCriteria.serializer(), false)"
        }

        test("is sent and read as JSON for a query string of a JSON suffix, even a scalar") {
            val text = searched("application/problem+json")
                .replace(oldValue = "schema: $OBJECT}", newValue = "schema: {type: string}}")

            kotlinOf(text = text, name = CLIENT) shouldContain "queryString(client.json(String.serializer(), criteria)"
        }

        test("is refused with its name escaped once, so the name cannot reorder or forge the message") {
            val text = searched("text/plain").replace(oldValue = "name: criteria", newValue = """name: "q\u202E\n"""")
            val message = generationOf(text).diagnostics.single().message

            message shouldContain """The parameter "q\u{202E}\n" takes the whole query string"""
            message shouldNotContain "\u202E"
        }

        test("is refused for a query string that is neither a form nor JSON, which nothing would encode") {
            generationOf(searched("text/plain")).codes() shouldBe listOf(CodegenCodes.OPERATION_UNSUPPORTED)
        }

        test("is refused for a form of the query string that is no object with properties") {
            val map = searched("'application/x-www-form-urlencoded; charset=utf-8'")
                .replace(
                    oldValue = "schema: $OBJECT}",
                    newValue = "schema: {type: object, additionalProperties: true}}",
                )
            val bare = document(
                PETS,
                "    get:",
                "      parameters: [{name: q, in: querystring, content: {application/x-www-form-urlencoded: {}}}]",
                "      responses: {'200': {description: ok}}",
            )

            generationOf(map).codes() shouldBe listOf(CodegenCodes.OPERATION_UNSUPPORTED)
            generationOf(bare).codes() shouldBe listOf(CodegenCodes.OPERATION_UNSUPPORTED)
        }
    }

    context("an operation of additionalOperations") {
        val text = custom("COPY")

        test("is called with its method as the document names it") {
            kotlinOf(text = text, name = CLIENT) shouldContain """apiCall("copyPet", "COPY")"""
        }

        test("is served with its method") {
            kotlinOf(text = text, name = ROUTES) shouldContain """method = "COPY","""
        }

        test("is named after its method and path without an operationId") {
            val unnamed = custom("PURGE").replace(oldValue = "operationId: copyPet, ", newValue = "")

            kotlinOf(text = unnamed, name = CLIENT) shouldContain "public suspend fun purgePets("
        }

        test("is refused for a method in lower case, which the runtimes would send in upper case") {
            val refused = generationOf(custom("purge"))

            refused.codes() shouldBe listOf(CodegenCodes.OPERATION_UNSUPPORTED)
            refused.diagnostics.single().message shouldContain "\"purge\""
        }

        test("is left out when it is ignored") {
            val ignored = custom("COPY")
                .replace(oldValue = "operationId: copyPet, ", newValue = "x-kapi-ignore: true, ")

            kotlinOf(text = ignored, name = CLIENT) shouldNotContain "COPY"
        }
    }
})
