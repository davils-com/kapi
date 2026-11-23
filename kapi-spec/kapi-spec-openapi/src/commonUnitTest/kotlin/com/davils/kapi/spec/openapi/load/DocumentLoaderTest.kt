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

package com.davils.kapi.spec.openapi.load

import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.source.Refusal
import com.davils.kapi.spec.core.source.Resolution
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.fixture.yamlOf
import com.davils.kapi.spec.openapi.load.fixture.Loading
import com.davils.kore.core.text.TextPosition
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.io.bytestring.ByteString
import com.davils.kore.core.annotation.DavilsInternal

private const val INVALID_BYTE: Int = 0xFF

private val petstoreText: String = yamlOf("openapi: 3.1.0", "info:", "  title: Petstore")

private val modelsText: String = yamlOf("Pet:", "  type: object")

private val petstore: DocumentUri = DocumentUri("memory:/api/petstore.yaml")

private val models: DocumentUri = DocumentUri("memory:/api/models.yaml")

private val errors: DocumentUri = DocumentUri("memory:/api/errors.yaml")

private val from: Location = Location(petstore, JsonPointer.of("components"))

private val inline: Source = Source.Text(petstoreText, petstore)

private const val SHA_256_LENGTH: Int = 32

class DocumentLoaderTest : FunSpec({
    context("the root document") {
        test("is parsed from a text without asking the resolver") {
            val loading = Loading(inline)

            val document = loading.loader.root().shouldNotBeNull()

            document.uri shouldBe petstore
            document.root["openapi"].string() shouldBe "3.1.0"
            document.source.length shouldBe petstoreText.length
            document.source.digest.size shouldBe SHA_256_LENGTH
            loading.resolver.requested.shouldBeEmpty()
            loading.diagnostics.shouldBeEmpty()
            loading.loader.documents shouldBe listOf(document)
        }

        test("is fetched through the resolver when it is located") {
            val loading = Loading(Source.Located(petstore), mapOf(petstore to petstoreText))

            val document = loading.loader.root().shouldNotBeNull()

            document.text shouldBe petstoreText
            loading.resolver.requested shouldBe listOf(petstore)
        }

        test("is refused when it is remote") {
            val remote = DocumentUri("https://example.com/petstore.yaml")
            val loading = Loading(Source.Located(remote))

            loading.loader.root() shouldBe null
            loading.codes shouldBe listOf(DiagnosticCodes.LOAD_REMOTE)
            loading.resolver.requested.shouldBeEmpty()
        }

        test("is refused when it is larger than a document may be") {
            val loading = Loading(inline) {
                limits { maximumDocumentLength = 1 }
            }

            loading.loader.root() shouldBe null
            loading.codes shouldBe listOf(DiagnosticCodes.LOAD_SIZE)
        }

        test("reports what the parser found with its place and a snippet") {
            val twice = yamlOf("openapi: 3.1.0", "openapi: 3.0.3")
            val loading = Loading(Source.Text(twice, petstore))

            loading.loader.root() shouldBe null
            val diagnostic = loading.diagnostics.single()

            diagnostic.code shouldBe ViolationCode("kaml.mapping.duplicate-key")
            diagnostic.location?.position shouldBe TextPosition(1, 0)
            diagnostic.snippet?.line shouldBe "openapi: 3.0.3"
        }

        test("holds exactly one document") {
            val several = Loading(Source.Text(yamlOf("a: 1", "---", "b: 2"), petstore))
            val none = Loading(Source.Text("", petstore))

            several.loader.root() shouldBe null
            none.loader.root() shouldBe null

            several.codes shouldBe listOf(DiagnosticCodes.SOURCE_MULTIPLE_DOCUMENTS)
            none.codes shouldBe listOf(DiagnosticCodes.DOCUMENT_NOT_MAPPING)
        }

        test("accepts merge keys only when the reader was told to") {
            val text = yamlOf("base: &base", "  a: 1", "derived:", "  <<: *base")
            val plain = Loading(Source.Text(text, petstore))
            val merging = Loading(Source.Text(text, petstore)) { hasMergeKeys = true }

            val unmerged = plain.loader.root().shouldNotBeNull()
            val merged = merging.loader.root().shouldNotBeNull()

            unmerged.root["derived"]["a"] shouldBe null
            merged.root["derived"]["a"].string() shouldBe "1"
        }
    }

    context("another document") {
        test("is loaded once and remembered") {
            val loading = Loading(inline, mapOf(models to modelsText))
            loading.loader.root()

            val first = loading.loader.load(models, from).shouldNotBeNull()
            val second = loading.loader.load(models, from)

            second shouldBe first
            loading.resolver.requested shouldBe listOf(models)
            loading.loader.order shouldBe mapOf(petstore to 0, models to 1)
        }

        test("is reported at the reference when it does not exist, and not asked for again") {
            val loading = Loading(inline)
            loading.loader.root()

            loading.loader.load(models, from) shouldBe null
            loading.loader.load(models, from) shouldBe null

            loading.codes shouldBe listOf(DiagnosticCodes.LOAD_MISSING)
            loading.diagnostics.single().location shouldBe from
            loading.diagnostics.single().message shouldContain "memory:/api/models.yaml does not exist"
            loading.resolver.requested shouldBe listOf(models)
        }

        test("is refused outside the folder, with another scheme or over the network") {
            val loading = Loading(inline)
            loading.loader.root()

            loading.loader.load(DocumentUri("memory:/secrets.yaml"), from) shouldBe null
            loading.loader.load(DocumentUri("file:///api/models.yaml"), from) shouldBe null
            loading.loader.load(DocumentUri("HTTP://example.com/models.yaml"), from) shouldBe null

            loading.codes.toSet() shouldBe setOf(
                DiagnosticCodes.REFERENCE_OUTSIDE,
                DiagnosticCodes.LOAD_SCHEME,
                DiagnosticCodes.LOAD_REMOTE,
            )
            loading.resolver.requested.shouldBeEmpty()
        }

        test("is refused once a reading has loaded as many documents as it may") {
            val documents = mapOf(models to modelsText, errors to modelsText)
            val loading = Loading(inline, documents) {
                limits { maximumDocuments = 2 }
            }
            loading.loader.root()

            loading.loader.load(models, from).shouldNotBeNull()
            loading.loader.load(errors, from) shouldBe null

            loading.codes shouldBe listOf(DiagnosticCodes.LOAD_DOCUMENTS)
        }

        test("maps every answer of the resolver to its code") {
            val unresolvable = DocumentUri("memory:/api/unresolvable.yaml")
            val answers = mapOf(
                models to Resolution.Refused(Refusal.Confinement),
                errors to Resolution.Refused(Refusal.Size),
                petstore to Resolution.Failed("the disk is gone"),
                unresolvable to Resolution.Refused(Refusal.Unresolvable),
            )
            val loading = Loading(Source.Located(petstore), answers = answers)

            loading.loader.root() shouldBe null
            loading.loader.load(models, from) shouldBe null
            loading.loader.load(errors, from) shouldBe null
            loading.loader.load(unresolvable, from) shouldBe null

            loading.codes.toSet() shouldBe setOf(
                DiagnosticCodes.LOAD_FAILED,
                DiagnosticCodes.REFERENCE_OUTSIDE,
                DiagnosticCodes.LOAD_SIZE,
            )
            loading.diagnostics.first().message shouldContain "the disk is gone"
        }

        test("is refused when it is no UTF-8") {
            val broken = Resolution.Found(ByteString(INVALID_BYTE.toByte()))
            val loading = Loading(inline, answers = mapOf(models to broken))
            loading.loader.root()

            loading.loader.load(models, from) shouldBe null

            loading.codes shouldBe listOf(DiagnosticCodes.SOURCE_ENCODING)
        }
    }
})
