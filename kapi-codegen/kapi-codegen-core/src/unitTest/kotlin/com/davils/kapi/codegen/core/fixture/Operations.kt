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

import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.codegen.core.options.GeneratorBuilder
import io.kotest.matchers.types.shouldBeInstanceOf

internal const val OK: String = "responses: {'200': {description: ok}}"

internal const val DEFAULT_CLIENT: String = "PetstoreDefaultClient"

internal const val LIST_PETS: String = "operationId: listPets"

internal const val LIST_PETS_RESULT: String = "PetstoreListPetsResult"

internal fun paths(vararg lines: String): List<String> {
    val body = lines.map { line -> "  $line" }
    return listOf("paths:") + body
}

internal fun get(vararg fields: String): List<String> {
    val all = fields.toList() + OK
    return paths("/pets: {get: {${all.joinToString()}}}")
}

internal fun parameters(vararg parameters: String): List<String> {
    val all = parameters.joinToString { fields -> "{$fields}" }
    return get(LIST_PETS, "parameters: [$all]")
}

internal fun responses(fields: String): List<String> = paths("/pets: {get: {$LIST_PETS, responses: {$fields}}}")

internal suspend fun clientGenerationOf(lines: List<String>, configure: GeneratorBuilder.() -> Unit = {}): Generation {
    val reading = read(lines)
    val generator = generator {
        artifacts = setOf(Artifact.Models, Artifact.Client)
        configure()
    }
    return generator.generate(reading.document, reading.sources)
}

internal suspend fun clientFilesOf(lines: List<String>): List<String> {
    val generated = clientGenerationOf(lines).shouldBeInstanceOf<Generation.Generated>()
    return generated.files.map { file -> file.path.substringAfter("petstore/") }
}

internal suspend fun clientOf(lines: List<String>, name: String = DEFAULT_CLIENT): String {
    val generated = clientGenerationOf(lines).shouldBeInstanceOf<Generation.Generated>()
    return generated.files.first { file -> file.path.endsWith("/$name.kt") }.content
}

internal suspend fun serverGenerationOf(lines: List<String>, configure: GeneratorBuilder.() -> Unit = {}): Generation {
    val reading = read(lines)
    val generator = generator {
        artifacts = setOf(Artifact.Models, Artifact.Server)
        configure()
    }
    return generator.generate(reading.document, reading.sources)
}

internal suspend fun serverFilesOf(lines: List<String>): List<String> {
    val generated = serverGenerationOf(lines).shouldBeInstanceOf<Generation.Generated>()
    return generated.files.map { file -> file.path.substringAfter("petstore/") }
}

internal suspend fun serverOf(lines: List<String>, name: String = "PetstoreDefaultRoutes"): String {
    val generated = serverGenerationOf(lines).shouldBeInstanceOf<Generation.Generated>()
    return generated.files.first { file -> file.path.endsWith("/$name.kt") }.content
}
