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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.openapi.read.reference

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.model.Target
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val MODELS: String = "models.yaml"

private const val PET_IN_MODELS: String = "models.yaml#/Pet"

private const val SELF: String = "v2/openapi.yaml"

private val models: DocumentUri = DocumentUri("memory:/api/models.yaml")

private val base: DocumentUri = DocumentUri("memory:/api/petstore.yaml")

private val remote: DocumentUri = DocumentUri("https://example.com/api/petstore.yaml")

private val opaque: DocumentUri = DocumentUri("urn:example:petstore")

private fun documentOf(from: DocumentUri, text: String): DocumentUri? {
    val reference = UriResolution.parse(text).shouldNotBeNull()
    return UriResolution.documentOf(from, reference)
}

private fun pointerOf(text: String): JsonPointer? {
    val reference = UriResolution.parse(text).shouldNotBeNull()
    return UriResolution.pointerOf(reference)
}

class UriResolutionTest : FunSpec({
    context("the document of a reference") {
        test("is the base when the reference has a fragment alone") {
            documentOf(base, "#/components") shouldBe base
            documentOf(base, "") shouldBe base
        }

        test("is resolved against the base for every other part a reference may have") {
            documentOf(base, MODELS) shouldBe models
            documentOf(base, "../shared/errors.yaml#/E") shouldBe DocumentUri("memory:/shared/errors.yaml")
            documentOf(base, "?version=2") shouldBe DocumentUri("memory:/api/petstore.yaml?version=2")
            documentOf(base, "https://example.com/x.yaml") shouldBe DocumentUri("https://example.com/x.yaml")
            documentOf(remote, "//other.example.com/y.yaml") shouldBe DocumentUri("https://other.example.com/y.yaml")
        }

        test("is unknown when the base is no hierarchical URI") {
            documentOf(opaque, MODELS) shouldBe null
        }
    }

    context("the pointer of a reference") {
        test("is the decoded fragment") {
            pointerOf("#/components/schemas/P%C3%A9t") shouldBe JsonPointer.of("components", "schemas", "Pét")
            pointerOf("#/a%20b") shouldBe JsonPointer.of("a b")
            pointerOf(MODELS) shouldBe JsonPointer.ROOT
        }

        test("is refused when the fragment decodes to no UTF-8 or no pointer") {
            pointerOf("#/a%FF") shouldBe null
            pointerOf("#a") shouldBe null
        }

        test("is never read from a broken escape, which no reference may hold") {
            listOf("#/a%", "#/a%4", "#/a%zz", "#/€").forEach { text ->
                UriResolution.parse(text) shouldBe null
            }
        }
    }

    context("the target of a reference") {
        test("is the document and the pointer") {
            val reference = UriResolution.parse(PET_IN_MODELS).shouldNotBeNull()

            val target = UriResolution.target(base, reference)

            target shouldBe Target(models, JsonPointer.of("Pet"))
        }

        test("is unknown when the document or the pointer is") {
            val noDocument = UriResolution.parse(PET_IN_MODELS).shouldNotBeNull()
            val noPointer = UriResolution.parse("#Pet").shouldNotBeNull()

            UriResolution.target(opaque, noDocument) shouldBe null
            UriResolution.target(base, noPointer) shouldBe null
        }
    }

    context("the self of a document") {
        test("resolves a relative self against the address the document was read from") {
            val self = UriResolution.selfOf(base, SELF)

            self shouldBe DocumentUri("memory:/api/v2/openapi.yaml")
        }

        test("keeps an absolute self, normalized") {
            UriResolution.selfOf(
                base,
                "https://example.com/a/../b.yaml",
            ) shouldBe DocumentUri("https://example.com/b.yaml")
        }

        test("is refused with a fragment, as no URI reference, or relative to an opaque address") {
            UriResolution.selfOf(base, "https://example.com/b.yaml#top") shouldBe null
            UriResolution.selfOf(base, "http://[broken") shouldBe null
            UriResolution.selfOf(opaque, SELF) shouldBe null
        }
    }
})
