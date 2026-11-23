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
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.load.fixture.Loading
import com.davils.kapi.spec.core.load.fixture.yamlOf
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.core.policy.ReferencePolicy
import com.davils.kapi.spec.core.source.Resolution
import com.davils.kapi.spec.core.source.Resolver
import com.davils.kapi.spec.core.source.Source
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.TextPosition
import com.davils.kore.core.text.TextRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private val text: String = yamlOf("openapi: 3.1.0", "info:", "  title: Petstore")

private val petstore: DocumentUri = DocumentUri("memory:/api/petstore.yaml")

private val title: JsonPointer = JsonPointer.of("info", "title")

private suspend fun loaded(): LoadedDocument {
    val loading = Loading(Source.Text(text, petstore))
    return loading.loader.root().shouldNotBeNull()
}

private fun settings(resolver: Resolver = Resolver.None, hasMergeKeys: Boolean = false): LoadSettings = LoadSettings(
    resolver = resolver,
    limits = ReadLimits.Default,
    references = ReferencePolicy.Default,
    hasMergeKeys = hasMergeKeys,
)

class LoadedDocumentTest : FunSpec({
    context("a loaded document") {
        test("locates a node by its pointer, line and column") {
            val document = loaded()
            val node = document.root["info"]["title"]

            val location = document.locate(title, node)

            location.document shouldBe petstore
            location.pointer shouldBe title
            location.position shouldBe TextPosition(line = 2, column = text.lines()[2].indexOf("Petstore"))
            document.snippet(location)?.line shouldBe "  title: Petstore"
        }

        test("locates by pointer alone what has no place in the text") {
            val document = loaded()

            val missing = document.locate(title, node = null)
            val unranged = document.locate(title, range = null)

            missing shouldBe Location(petstore, title)
            unranged shouldBe missing
            document.snippet(missing) shouldBe null
        }

        test("locates a range") {
            val document = loaded()

            val location = document.locate(JsonPointer.ROOT, TextRange.at(0))

            location.position shouldBe TextPosition(line = 0, column = 0)
        }
    }

    context("the settings of a loader") {
        test("equal the settings of the same resolver, limits, policies and merge keys") {
            val settings = settings()

            settings shouldBe settings()
            settings.hashCode() shouldBe settings().hashCode()
            settings shouldNotBe settings(hasMergeKeys = true)
            settings shouldNotBe settings(resolver = Resolver { Resolution.Missing })
            val text: Any = "$settings"
            (settings == text) shouldBe false
        }

        test("show the limits and policies but never the resolver") {
            val text = settings().toString()

            text shouldContain "limits=ReadLimits("
            text shouldContain "references=ReferencePolicy("
            text shouldContain "hasMergeKeys=false"
            text shouldNotContain "resolver"
        }
    }
})
