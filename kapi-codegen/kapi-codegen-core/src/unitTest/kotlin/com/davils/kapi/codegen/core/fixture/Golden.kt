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
import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.GeneratedFileKind
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

private const val FOLDER: String = "/com/davils/kapi/codegen/core/golden/"

private const val RULE: String = "===== "

private val chosen: Map<String, Set<Artifact>> = mapOf(
    "client" to setOf(Artifact.Models, Artifact.Client, Artifact.Server),
    "server" to setOf(Artifact.Models, Artifact.Client, Artifact.Server),
    "xml" to setOf(Artifact.Models, Artifact.Client, Artifact.Server),
    "mocks" to setOf(Artifact.Models, Artifact.Client, Artifact.Server, Artifact.Mocks),
    "hooks" to setOf(Artifact.Models, Artifact.Senders, Artifact.Receivers),
    "hostile" to Artifact.entries.toSet(),
)

internal object Golden {
    fun text(name: String): String {
        val stream = Golden::class.java.getResourceAsStream(FOLDER + name).shouldNotBeNull()
        return stream.use { input -> input.readBytes().decodeToString() }
    }

    suspend fun generated(name: String): Generation.Generated {
        val uri = DocumentUri("file:/work/specs/$name.yaml")
        val reading = OpenApi.reader().read(Source.Text(text("$name.yaml"), uri))
        val read = reading.shouldBeInstanceOf<Reading.Read>()
        val generator = Codegen.openApi {
            packageName = "com.example.petstore"
            apiName = "Petstore"
            artifacts = chosen[name] ?: setOf(Artifact.Models, Artifact.Client)
        }
        return generator.generate(read.document, read.sources).shouldBeInstanceOf<Generation.Generated>()
    }

    fun rendered(files: List<GeneratedFile>): String = kotlin(files).joinToString("") { file ->
        RULE + file.path + "\n" + file.content
    }

    fun kotlin(files: List<GeneratedFile>): List<GeneratedFile> =
        files.filter { file -> file.kind == GeneratedFileKind.Kotlin }
}
