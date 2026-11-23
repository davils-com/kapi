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

package com.davils.kapi.spec.core.load

import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.load.fixture.Loading
import com.davils.kapi.spec.core.load.fixture.TestLoadCodes
import com.davils.kapi.spec.core.load.fixture.yamlOf
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.core.policy.ReadLimitsBuilder
import com.davils.kapi.spec.core.policy.ReferencePolicyBuilder
import com.davils.kapi.spec.core.source.Refusal
import com.davils.kapi.spec.core.source.Resolution
import com.davils.kapi.spec.core.source.Source
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.TextPosition
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val INVALID_BYTE: Int = 0xFF

private const val SHA_256_LENGTH: Int = 32

private val petstoreText: String = yamlOf("openapi: 3.1.0", "info:", "  title: Petstore")

private val modelsText: String = yamlOf("Pet:", "  type: object")

private val petstore: DocumentUri = DocumentUri("memory:/api/petstore.yaml")

private val models: DocumentUri = DocumentUri("memory:/api/models.yaml")

private val errors: DocumentUri = DocumentUri("memory:/api/errors.yaml")

private val from: Location = Location(petstore, JsonPointer.of("components"))

private val inline: Source = Source.Text(petstoreText, petstore)

private fun found(text: String): Resolution = Resolution.Found(text.encodeToByteString())

private fun limits(configure: ReadLimitsBuilder.() -> Unit): ReadLimits {
    val builder = ReadLimitsBuilder()
    builder.configure()
    return builder.build()
}

class DocumentLoaderTest : FunSpec({
    context("the root document") {
        test("is parsed from a text without asking the resolver") {
            val loading = Loading(inline)

            val document = loading.loader.root().shouldNotBeNull()

            document.uri shouldBe petstore
            document.root["openapi"].string() shouldBe "3.1.0"
            document.source.length shouldBe petstoreText.length
            document.source.digest.size shouldBe SHA_256_LENGTH
            loading.requested.shouldBeEmpty()
            loading.diagnostics.shouldBeEmpty()
            loading.loader.documents shouldBe listOf(document)
        }

        test("is fetched through the resolver when it is located") {
            val loading = Loading(Source.Located(petstore), answers = mapOf(petstore to found(petstoreText)))

            val document = loading.loader.root().shouldNotBeNull()

            document.text shouldBe petstoreText
            loading.requested shouldBe listOf(petstore)
        }

        test("is refused when it is remote, whatever the case of its scheme") {
            listOf("https://example.com/petstore.yaml", "HTTP://example.com/petstore.yaml").forEach { text ->
                val loading = Loading(Source.Located(DocumentUri(text)))

                loading.loader.root() shouldBe null
                loading.codes shouldBe listOf(TestLoadCodes.remote)
                loading.requested.shouldBeEmpty()
            }
        }

        test("is refused when it is one byte larger than a document may be, and read at the limit") {
            val exact = Loading(inline, limits = limits { maximumDocumentLength = petstoreText.length })
            val larger = Loading(inline, limits = limits { maximumDocumentLength = petstoreText.length - 1 })

            exact.loader.root().shouldNotBeNull()
            larger.loader.root() shouldBe null
            larger.codes shouldBe listOf(TestLoadCodes.size)
            larger.diagnostics.single().message shouldContain "is larger than a document may be"
        }

        test("reports what the parser found with its place and a snippet") {
            val twice = Source.Text(yamlOf("openapi: 3.1.0", "openapi: 3.0.3"), petstore)
            val loading = Loading(twice)

            loading.loader.root() shouldBe null
            val diagnostic = loading.diagnostics.single()

            diagnostic.code shouldBe ViolationCode("kaml.mapping.duplicate-key")
            diagnostic.location?.position shouldBe TextPosition(line = 1, column = 0)
            diagnostic.location?.pointer shouldBe JsonPointer.ROOT
            diagnostic.snippet?.line shouldBe "openapi: 3.0.3"
        }

        test("holds exactly one document") {
            val several = Loading(Source.Text(yamlOf("a: 1", "---", "b: 2"), petstore))
            val none = Loading(Source.Text("", petstore))

            several.loader.root() shouldBe null
            none.loader.root() shouldBe null

            several.codes shouldBe listOf(TestLoadCodes.multipleDocuments)
            none.codes shouldBe listOf(TestLoadCodes.documentEmpty)
            none.diagnostics.single().location shouldBe Location(petstore, JsonPointer.ROOT)
        }

        test("accepts merge keys only when the reader was told to") {
            val text = Source.Text(yamlOf("base: &base", "  a: 1", "derived:", "  <<: *base"), petstore)
            val plain = Loading(text)
            val merging = Loading(text, hasMergeKeys = true)

            val unmerged = plain.loader.root().shouldNotBeNull()
            val merged = merging.loader.root().shouldNotBeNull()

            unmerged.root["derived"]["a"] shouldBe null
            merged.root["derived"]["a"].string() shouldBe "1"
        }

        test("is dropped when the preparation drops it, without a finding") {
            val loading = Loading(inline, prepare = { null })

            loading.loader.root() shouldBe null
            loading.diagnostics.shouldBeEmpty()
            loading.loader.documents.shouldBeEmpty()
        }
    }

    context("hostile input") {
        test("is refused when it nests deeper than the limit") {
            val deep = Source.Text(yamlOf("a: " + "[".repeat(8) + "]".repeat(8)), petstore)
            val loading = Loading(deep, limits = limits { maximumDepth = 4 })

            loading.loader.root() shouldBe null
            loading.codes.shouldNotBeEmpty()
            loading.codes.first().value shouldStartWith "kaml."
        }

        test("is refused when its aliases expand beyond the limit") {
            val bomb = yamlOf("a: &a [x, x, x]", "b: &b [*a, *a, *a]", "c: &c [*b, *b, *b]", "d: [*c, *c, *c]")
            val loading = Loading(Source.Text(bomb, petstore), limits = limits { maximumAliasExpansion = 10 })

            loading.loader.root() shouldBe null
            loading.codes.shouldNotBeEmpty()
        }

        test("is refused when a scalar is longer than the limit") {
            val long = Source.Text(yamlOf("a: " + "b".repeat(32)), petstore)
            val loading = Loading(long, limits = limits { maximumScalarLength = 8 })

            loading.loader.root() shouldBe null
            loading.codes.shouldNotBeEmpty()
        }

        test("is no UTF-8 and refused before it is parsed") {
            val broken = Resolution.Found(ByteString(INVALID_BYTE.toByte()))
            val loading = Loading(Source.Located(petstore), answers = mapOf(petstore to broken))

            loading.loader.root() shouldBe null
            loading.codes shouldBe listOf(TestLoadCodes.encoding)
            loading.diagnostics.single().message shouldContain "is not valid UTF-8"
        }
    }

    context("another document") {
        test("is loaded once and remembered") {
            val loading = Loading(inline, answers = mapOf(models to found(modelsText)))
            loading.loader.root()

            val first = loading.loader.load(models, from).shouldNotBeNull()
            val second = loading.loader.load(models, from)

            second shouldBe first
            loading.requested shouldBe listOf(models)
            loading.loader.order shouldBe mapOf(petstore to 0, models to 1)
            loading.loader.documents.size shouldBe 2
        }

        test("is reported at the reference when it does not exist, and not asked for again") {
            val loading = Loading(inline)
            loading.loader.root()

            loading.loader.load(models, from) shouldBe null
            loading.loader.load(models, from) shouldBe null

            loading.codes shouldBe listOf(TestLoadCodes.missing)
            loading.diagnostics.single().location shouldBe from
            loading.diagnostics.single().message shouldContain "memory:/api/models.yaml does not exist"
            loading.requested shouldBe listOf(models)
        }

        test("is refused outside the folder, with another scheme or over the network") {
            val loading = Loading(inline)
            loading.loader.root()

            loading.loader.load(DocumentUri("memory:/secrets.yaml"), from) shouldBe null
            loading.loader.load(DocumentUri("file:///api/models.yaml"), from) shouldBe null
            loading.loader.load(DocumentUri("https://example.com/models.yaml"), from) shouldBe null

            loading.codes.toSet() shouldBe setOf(TestLoadCodes.outside, TestLoadCodes.scheme, TestLoadCodes.remote)
            loading.requested.shouldBeEmpty()
        }

        test("is refused when the reader allows no other document") {
            val builder = ReferencePolicyBuilder().apply { isExternalAllowed = false }
            val loading = Loading(inline, references = builder.build())
            loading.loader.root()

            loading.loader.load(models, from) shouldBe null

            loading.codes shouldBe listOf(TestLoadCodes.outside)
        }

        test("is refused once a reading has loaded as many documents as it may") {
            val answers = mapOf(models to found(modelsText), errors to found(modelsText))
            val loading = Loading(inline, answers = answers, limits = limits { maximumDocuments = 2 })
            loading.loader.root()

            loading.loader.load(models, from).shouldNotBeNull()
            loading.loader.load(errors, from) shouldBe null

            loading.codes shouldBe listOf(TestLoadCodes.tooManyDocuments)
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

            loading.codes.toSet() shouldBe setOf(TestLoadCodes.failed, TestLoadCodes.outside, TestLoadCodes.size)
            val messages = loading.diagnostics.map(Diagnostic::message)
            messages.toSet().size shouldBe answers.size
            loading.diagnostics.first().message shouldContain "the disk is gone"
        }

        test("is refused when it is larger than a document may be") {
            val answers = mapOf(models to found(petstoreText + yamlOf("x-larger: true")))
            val loading = Loading(
                inline,
                answers = answers,
                limits = limits { maximumDocumentLength = petstoreText.length },
            )
            loading.loader.root()

            loading.loader.load(models, from) shouldBe null

            loading.codes shouldBe listOf(TestLoadCodes.size)
        }
    }

    context("an overlay") {
        test("is loaded in order and placed after the documents") {
            val overlay = DocumentUri("memory:/api/overlay.yaml")
            val loading = Loading(inline, answers = mapOf(overlay to found(yamlOf("overlay: 1.0.0"))))
            loading.loader.root()

            val loaded = loading.loader.overlay(Source.Located(overlay)).shouldNotBeNull()
            val missing = loading.loader.overlay(Source.Located(models))

            missing shouldBe null
            loading.loader.overlays shouldBe listOf(loaded)
            loading.loader.order shouldBe mapOf(petstore to 0, overlay to 1)
            loading.codes shouldBe listOf(TestLoadCodes.missing)
        }

        test("is applied by replacing the document of the same address") {
            val loading = Loading(inline)
            val root = loading.loader.root().shouldNotBeNull()
            val changed = LoadedDocument(yamlOf("openapi: 3.2.0"), root.root, root.source)

            loading.loader.replace(changed)

            loading.loader.documents shouldBe listOf(changed)
        }
    }
})
