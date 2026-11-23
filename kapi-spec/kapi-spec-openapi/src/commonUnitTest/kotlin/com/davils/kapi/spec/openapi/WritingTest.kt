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

package com.davils.kapi.spec.openapi

import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.fixture.Resources
import com.davils.kapi.spec.openapi.fixture.head31
import com.davils.kapi.spec.openapi.fixture.petstoreUri
import com.davils.kapi.spec.openapi.fixture.yamlOf
import com.davils.kapi.spec.openapi.testing.Corpus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith

private val sparse: List<String> = listOf(
    "components:",
    "  schemas:",
    "    A: {type: [string, 'null'], default: null}",
    "  headers:",
    "    Plain: {}",
    "  responses:",
    "    Gone: {description: Gone}",
    "    Same: {\$ref: '#/components/responses/Gone', summary: Short, description: Long}",
    "  securitySchemes:",
    "    oauth: {type: oauth2, flows: {implicit: {authorizationUrl: https://example.com, scopes: {}}}}",
    "webhooks:",
    "  changed: {post: {responses: {x-note: none}}}",
)

private suspend fun documentOf(source: Source): Document {
    val reading = OpenApi.reader().read(source)
    val read = (reading as? Reading.Read)?.document
    val partial = (reading as? Reading.Invalid)?.partial
    return checkNotNull(read ?: partial)
}

private suspend fun rewritten(source: Source.Text): Pair<Document, Document> {
    val first = documentOf(source)
    val second = documentOf(Source.Text(OpenApi.write(first), source.uri))
    return first to second
}

class WritingTest : FunSpec({
    context("a written document") {
        test("starts with its version") {
            val document = documentOf(Corpus.source("petstore"))

            OpenApi.write(document) shouldStartWith "openapi: 3.1.0"
        }

        test("reads back as the document it was written from, for every document of the corpus") {
            Corpus.names.forEach { name ->
                val (first, second) = rewritten(Corpus.source(name))

                val differing = name.takeIf { second != first }

                differing shouldBe null
            }
        }

        test("keeps what a document leaves out and what it writes in its shortest form") {
            val (first, second) = rewritten(Source.Text(yamlOf(head31 + sparse), petstoreUri))
            val written = OpenApi.write(first)

            second shouldBe first
            written shouldContain "summary: Short"
            written shouldContain "default: null"
            written shouldNotContain "paths:"
        }

        test("reads back as the document it was written from, for the document that uses every object") {
            val source = Source.Text(Resources.text("everything.yaml"), petstoreUri)
            val (first, second) = rewritten(source)

            second shouldBe first
        }
    }
})
