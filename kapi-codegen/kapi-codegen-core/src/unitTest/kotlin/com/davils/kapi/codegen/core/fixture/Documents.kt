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

package com.davils.kapi.codegen.core.fixture

import com.davils.kapi.codegen.core.Codegen
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.Generator
import com.davils.kapi.codegen.core.options.GeneratorBuilder
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import com.davils.kapi.spec.testing.MemoryResolver
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.matchers.types.shouldBeInstanceOf

internal const val PACKAGE: String = "com.example.petstore"

internal const val API: String = "Petstore"

private const val INDENT: String = "      "

private val uri: DocumentUri = DocumentUri("file:/work/specs/petstore.yaml")

private val head: List<String> = listOf("openapi: 3.1.0", "info:", "  title: Petstore", "  version: 1.0.0")

private val operationHead: List<String> = listOf("paths:", "  /pets:", "    get:", "      operationId: listPets")

private val operationTail: List<String> =
    listOf("      responses:", "        '200':", "          description: The pets")

internal fun operation(vararg lines: String): List<String> {
    val body = lines.map { line -> INDENT + line }
    return operationHead + body + operationTail
}

internal fun schemas(lines: List<String>): List<String> {
    val body = lines.map { line -> "    $line" }
    return listOf("components:", "  schemas:") + body
}

internal fun schema(vararg lines: String): List<String> {
    val keywords = lines.map { line -> "  $line" }
    return schemas(listOf("Pet:", "  type: string") + keywords)
}

internal fun generator(configure: GeneratorBuilder.() -> Unit = {}): Generator = Codegen.openApi {
    packageName = PACKAGE
    apiName = API
    configure()
}

internal suspend fun read(lines: List<String>): Reading.Read {
    val text = (head + lines).joinToString("\n", postfix = "\n")
    val reading = OpenApi.reader().read(Source.Text(text, uri))
    return reading.shouldBeInstanceOf<Reading.Read>()
}

internal fun textOf(lines: List<String>): String = (head + lines).joinToString("\n", postfix = "\n")

internal suspend fun readAt(location: DocumentUri, lines: List<String>): Reading.Read {
    val reading = OpenApi.reader().read(Source.Text(textOf(lines), location))
    return reading.shouldBeInstanceOf<Reading.Read>()
}

internal suspend fun readWith(documents: Map<DocumentUri, String>, lines: List<String>): Reading.Read {
    val text = (head + lines).joinToString("\n", postfix = "\n")
    val reader = OpenApi.reader { resolver = MemoryResolver(documents) }
    return reader.read(Source.Text(text, uri)).shouldBeInstanceOf<Reading.Read>()
}

internal suspend fun partialOf(lines: List<String>): Document {
    val text = (head + lines).joinToString("\n", postfix = "\n")
    val reading = OpenApi.reader().read(Source.Text(text, uri))
    return reading.shouldBeInstanceOf<Reading.Invalid>().partial
}

internal suspend fun generationOf(lines: List<String>): Generation {
    val reading = read(lines)
    return generator().generate(reading.document, reading.sources)
}

internal suspend fun generatedOf(lines: List<String>): Generation.Generated =
    generationOf(lines).shouldBeInstanceOf<Generation.Generated>()

internal suspend fun refusedOf(lines: List<String>): Generation.Refused =
    generationOf(lines).shouldBeInstanceOf<Generation.Refused>()

internal fun pet(vararg properties: String, required: String = ""): List<String> {
    val body = properties.map { line -> "    $line" }
    return listOf("Pet:", "  type: object", "  required: [$required]", "  properties:") + body
}

internal suspend fun kotlinOf(lines: List<String>, name: String = "PetstorePet"): String {
    val file = generatedOf(schemas(lines)).files.first { file -> file.path.endsWith("/$name.kt") }
    return file.content
}

internal fun Generation.codes(): List<ViolationCode> = diagnostics.map(Diagnostic::code)
