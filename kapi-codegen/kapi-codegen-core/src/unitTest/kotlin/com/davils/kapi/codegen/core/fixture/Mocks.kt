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

internal const val SAMPLES: String = "PetstoreSamples"
internal const val DEFAULT_MOCK: String = "PetstoreDefaultMockService"
internal const val UNMATCHED: String = "{type: string, pattern: '^[0-9]{4}\$'}"

internal suspend fun mockGenerationOf(lines: List<String>, configure: GeneratorBuilder.() -> Unit = {}): Generation {
    val reading = read(lines)
    val generator = generator {
        artifacts = Artifact.entries.toSet()
        configure()
    }
    return generator.generate(reading.document, reading.sources)
}

internal suspend fun mockFileOf(lines: List<String>, name: String = DEFAULT_MOCK): String {
    val generated = mockGenerationOf(lines).shouldBeInstanceOf<Generation.Generated>()
    return generated.files.first { file -> file.path.endsWith("/$name.kt") }.content
}

internal suspend fun samplesOf(lines: List<String>): String = mockFileOf(schemas(lines), SAMPLES)

internal suspend fun sampleOf(vararg properties: String, required: String = ""): String =
    samplesOf(pet(*properties, required = required))
