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

package com.davils.kapi.codegen.core.asyncapi.fixture

import com.davils.kapi.codegen.core.Codegen
import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.asyncapi.GeneratorBuilder
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.spec.asyncapi.AsyncApi
import com.davils.kapi.spec.asyncapi.Reading
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import io.kotest.matchers.types.shouldBeInstanceOf

internal const val GOLDEN: String = "com/davils/kapi/codegen/core/asyncapi/golden/"

internal fun resource(name: String): String {
    val stream = ClassLoader.getSystemResourceAsStream(GOLDEN + name)
    return stream?.use { found -> found.readBytes().decodeToString() }.orEmpty()
}

internal suspend fun generationOf(
    name: String,
    text: String,
    configure: GeneratorBuilder.() -> Unit = {},
): Generation {
    val reading = AsyncApi.reader().read(Source.Text(text, DocumentUri("file:///specs/$name")))
    val read = reading.shouldBeInstanceOf<Reading.Read>()
    val generator = Codegen.asyncApi {
        packageName = "com.example." + name.substringBefore('.')
        apiName = name.substringBefore('.').replaceFirstChar(Char::uppercaseChar)
        artifacts = setOf(Artifact.Models, Artifact.Client, Artifact.Server)
        configure()
    }
    return generator.generate(read.document, read.sources)
}

internal suspend fun filesOf(name: String, text: String = resource(name)): List<GeneratedFile> =
    generationOf(name, text).shouldBeInstanceOf<Generation.Generated>().files

internal fun rendered(files: List<GeneratedFile>): String =
    files.joinToString("") { file -> "===== ${file.path}\n${file.content}" }
