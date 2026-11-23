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

package com.davils.kapi.spec.testing

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf

private const val UNKNOWN: String = "no-such-document"

private fun rootOf(text: String): YamlNode {
    val result = Yaml.Default.parse(text)
    val parsed = result.shouldBeInstanceOf<YamlParseResult.Parsed>()
    result.diagnostics.shouldBeEmpty()
    val document = parsed.stream.single.shouldNotBeNull()
    return document.root.shouldNotBeNull()
}

class CorpusTest : FunSpec({
    context("the names of the corpus") {
        test("are distinct and sorted by the names of their files") {
            val files = Corpus.openApiNames.map { name -> "$name.yaml" }

            files shouldBe files.sorted().distinct()
        }

        test("name every document of the corpus and nothing else") {
            Corpus.openApiNames.toSet() shouldBe openApiTexts.keys
        }
    }

    context("every document of the corpus") {
        test("is one YAML document of OpenAPI 3") {
            Corpus.openApiNames.forEach { name ->
                val root = rootOf(Corpus.openApiText(name))

                val version = root["openapi"].string()
                version.shouldNotBeNull() shouldStartWith "3."
            }
        }

        test("is a source at its own address in the corpus") {
            Corpus.openApiNames.forEach { name ->
                val source = Corpus.openApi(name)

                source.uri shouldBe DocumentUri("corpus:/$name.yaml")
                source.text shouldBe Corpus.openApiText(name)
            }
        }
    }

    context("a name the corpus does not know") {
        test("is refused with the name") {
            val text = shouldThrow<IllegalArgumentException> { Corpus.openApiText(UNKNOWN) }
            val source = shouldThrow<IllegalArgumentException> { Corpus.openApi(UNKNOWN) }

            text.message.shouldNotBeNull() shouldContain UNKNOWN
            source.message.shouldNotBeNull() shouldContain UNKNOWN
        }
    }
})
