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

import com.davils.kapi.codegen.core.Codegen
import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.fixture.KotlinCompilation
import com.davils.kapi.codegen.core.fixture.KotlinStyle
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import com.davils.kapi.spec.testing.Corpus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private val unreadable: Set<String> = setOf("hostile", "response-links")

private val refused: Map<String, List<String>> = mapOf(
    "feature-matrix" to listOf(
        "a schema that accepts nothing",
        "a union with a member that is no object in the components",
        "additionalProperties with a schema next to properties",
        "more than one type",
        "patternProperties",
        "prefixItems",
        "the parameter raw yet, which is no text, list or flat object",
    ),
    "openapi-32" to listOf(
        "a discriminator on a schema that nothing extends or chooses from",
        "a discriminator that maps lamp to no member",
    ),
)

private suspend fun generationOf(name: String): Generation {
    val reading = OpenApi.reader().read(Corpus.openApi(name))
    val read = reading.shouldBeInstanceOf<Reading.Read>()
    val generator = Codegen.openApi {
        packageName = "com.example.corpus"
        apiName = "Corpus"
        artifacts = Artifact.entries.toSet()
    }
    return generator.generate(read.document, read.sources)
}

private fun messageOf(what: String): String {
    val prefix = "The generator has no Kotlin for "
    if (what.endsWith("flat object")) return prefix + what
    return "$prefix$what yet"
}

class CorpusTest : FunSpec({
    context("the documents of the corpus") {
        val readable = Corpus.openApiNames - unreadable

        (readable - refused.keys).forEach { name ->
            test("$name becomes models, clients, servers, mocks and webhooks that compile and keep the rules") {
                val generated = generationOf(name).shouldBeInstanceOf<Generation.Generated>()

                KotlinCompilation.problems(generated.files).shouldBeEmpty()
                KotlinStyle.problems(generated.files).shouldBeEmpty()
            }
        }

        refused.forEach { (name, unsupported) ->
            test("$name is refused for exactly what the generator has no Kotlin for") {
                val refusal = generationOf(name).shouldBeInstanceOf<Generation.Refused>()
                val errors = refusal.diagnostics.filter { diagnostic -> diagnostic.isError }
                val codes = errors.map { diagnostic -> diagnostic.code }.distinct()

                codes.all { code ->
                    code in setOf(
                        CodegenCodes.SCHEMA_UNSUPPORTED,
                        CodegenCodes.OPERATION_UNSUPPORTED,
                    )
                } shouldBe
                    true
                val messages = errors.map { diagnostic -> diagnostic.message }.toSet()
                messages.sorted() shouldBe unsupported.map(::messageOf).sorted()
            }
        }
    }
})
