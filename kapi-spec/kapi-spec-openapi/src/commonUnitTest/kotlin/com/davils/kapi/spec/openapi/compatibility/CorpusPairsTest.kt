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

package com.davils.kapi.spec.openapi.compatibility

import com.davils.kapi.spec.core.compatibility.Impact
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.fixture.readable
import com.davils.kapi.spec.testing.Corpus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val PETSTORE: String = "petstore"

private suspend fun corpus(name: String): Document {
    val reading = OpenApi.reader().read(Corpus.openApi(name))
    return reading.readable()
}

private suspend fun petstoreWith(original: String, replacement: String): Document {
    val text = Corpus.openApiText(PETSTORE)
    val edited = text.replaceFirst(oldValue = original, newValue = replacement)
    edited shouldNotBe text
    val uri = Corpus.openApi(PETSTORE).uri
    val reading = OpenApi.reader().read(Source.Text(edited, uri))
    return reading.readable()
}

private suspend fun petstoreRules(original: String, replacement: String): List<String> {
    val comparison = OpenApi.compare(baseline = corpus(PETSTORE), candidate = petstoreWith(original, replacement))
    return comparison.changes.map(::ruleOf)
}

class CorpusPairsTest : FunSpec({
    context("every document of the corpus") {
        test("is compatible with itself") {
            Corpus.openApiNames.forEach { name ->
                val document = corpus(name)

                OpenApi.compare(baseline = document, candidate = document).changes.shouldBeEmpty()
                OpenApi.compare(baseline = document, candidate = corpus(name)).changes.shouldBeEmpty()
            }
        }
    }

    context("a pair of the corpus") {
        test("tells a narrower limit of a query as breaking for clients") {
            val comparison = OpenApi.compare(
                baseline = corpus(PETSTORE),
                candidate = petstoreWith(original = "maximum: 100", replacement = "maximum: 50"),
            )
            val change = comparison.changes.single()

            ruleOf(change) shouldBe "maximum"
            change.client shouldBe Impact.Breaking
            change.provider shouldBe Impact.NonBreaking
            change.candidate.shouldNotBeNull()
            comparison.isBreakingForClients shouldBe true
        }

        test("tells a moved path from a renamed parameter") {
            petstoreRules(original = "/pets/{petId}/photo:", replacement = "/pets/{petId}/image:") shouldBe
                listOf("path-removed", "path-added")
            petstoreRules(
                original = "  /pets/{petId}:\n    parameters:\n      - { name: petId",
                replacement = "  /pets/{id}:\n    parameters:\n      - { name: id",
            ) shouldBe listOf("path-parameter-renamed")
        }

        test("tells security that an operation now needs") {
            val rules = petstoreRules(
                original = "      security: []\n      parameters:\n        - { name: status",
                replacement = "      parameters:\n        - { name: status",
            )

            rules shouldContain "security-added"
        }

        test("of different documents finds changes in both directions") {
            val forward = OpenApi.compare(baseline = corpus(PETSTORE), candidate = corpus("kitchen-sink"))
            val backward = OpenApi.compare(baseline = corpus("kitchen-sink"), candidate = corpus(PETSTORE))

            (forward.changes.isNotEmpty() && backward.changes.isNotEmpty()) shouldBe true
            val first = OpenApi.compare(baseline = corpus("security"), candidate = corpus("security-and-or"))
            val second = OpenApi.compare(baseline = corpus("security"), candidate = corpus("security-and-or"))
            first shouldBe second
        }
    }
})
