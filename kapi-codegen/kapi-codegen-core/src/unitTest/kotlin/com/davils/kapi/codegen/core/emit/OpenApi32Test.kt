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
import io.kotest.matchers.types.shouldBeInstanceOf

private const val LINE: String = "\n"

private const val OBJECT: String = "{type: object, required: [name], properties: {name: {type: string}}}"

private val head: List<String> = listOf("openapi: 3.2.0", "info: {title: Petstore, version: 1.0.0}", "paths:")

private fun document(vararg lines: String): String = (head + lines).joinToString(LINE, postfix = LINE)

private fun searched(content: String): String = document(
    "  /pets:",
    "    query:",
    "      operationId: searchPets",
    "      parameters:",
    "        - {name: criteria, in: querystring, content: {$content: {schema: $OBJECT}}}",
    "      responses:",
    "        '200':",
    "          description: ok",
    "          content: {text/event-stream: {itemSchema: $OBJECT}}",
)

private suspend fun generationOf(text: String): Generation {
    val reading = OpenApi.reader().read(Source.Text(text, DocumentUri("file:/work/specs/petstore.yaml")))
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
            val kotlin = kotlinOf(text, "PetstoreDefaultClient")

            kotlin shouldContain """apiCall("searchPets", "QUERY")"""
            kotlin shouldContain """query(ParameterFormat("criteria", ParameterStyle.Form, true), client.fields("""
        }

        test("is served with its method and reads the whole query string") {
            val kotlin = kotlinOf(text, "PetstoreDefaultRoutes")

            kotlin shouldContain """method = "QUERY","""
            kotlin shouldContain "input.queryString(MediaType.parse(\"application/x-www-form-urlencoded\"), " +
                "PetstoreSearchPetsCriteria.serializer(), false)"
        }

        test("reads the items of a stream by the schema of its items") {
            kotlinOf(text, "PetstoreDefaultClient") shouldContain "client.events(PetstoreSearchPetsOkItem.serializer()"
        }

        test("is refused for a query string that is no form, which the client runtime cannot send") {
            generationOf(searched("application/json")).codes() shouldBe listOf(CodegenCodes.OPERATION_UNSUPPORTED)
        }
    }
})
