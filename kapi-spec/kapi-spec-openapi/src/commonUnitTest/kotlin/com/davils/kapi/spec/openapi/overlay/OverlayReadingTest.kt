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

package com.davils.kapi.spec.openapi.overlay

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import com.davils.kapi.spec.openapi.fixture.EMPTY_PATHS
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.head31
import com.davils.kapi.spec.openapi.fixture.petstoreUri
import com.davils.kapi.spec.openapi.fixture.yamlOf
import com.davils.kapi.spec.openapi.overlay.fixture.overlayHead
import com.davils.kapi.spec.openapi.overlay.fixture.overlayUri
import com.davils.kapi.spec.openapi.testing.MemoryResolver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private val petstore: String = yamlOf(head31 + EMPTY_PATHS)

private val retitled: String = yamlOf(overlayHead + listOf("  - target: $.info", "    update: {title: Pets}"))

private fun uriOf(name: String): DocumentUri = DocumentUri("memory:/api/$name")

private suspend fun readingWith(overlays: Map<String, String>): Reading {
    val documents = overlays.mapKeys { (name, _) -> uriOf(name) }
    val reader = OpenApi.reader { resolver = MemoryResolver(documents) }
    val sources = overlays.keys.map { name -> Source.Located(uriOf(name)) }
    return reader.read(Source.Text(petstore, petstoreUri), sources)
}

class OverlayReadingTest : FunSpec({
    context("a reading with overlays") {
        test("reads the document the overlays changed and lists the overlays among its sources") {
            val reading = readingWith(mapOf("petstore.overlay.yaml" to retitled))

            reading.document().info.title shouldBe "Pets"
            reading.sources.map(SourceDocument::uri) shouldBe listOf(petstoreUri, overlayUri)
        }

        test("applies the overlays in the order they are given") {
            val renamed = yamlOf(overlayHead + listOf("  - target: $.info", "    update: {title: Animals}"))

            val reading = readingWith(mapOf("petstore.overlay.yaml" to retitled, "second.yaml" to renamed))

            reading.document().info.title shouldBe "Animals"
        }

        test("is invalid when an overlay cannot be loaded or is malformed") {
            val documents = mapOf(overlayUri to "overlay: 1.0.0")
            val reader = OpenApi.reader { resolver = MemoryResolver(documents) }
            val overlays = listOf(
                Source.Located(uriOf("missing.yaml")),
                Source.Located(overlayUri),
            )

            val reading = reader.read(Source.Text(petstore, petstoreUri), overlays)

            reading.shouldBeInstanceOf<Reading.Invalid>()
            reading.codes() shouldBe listOf(
                DiagnosticCodes.LOAD_MISSING,
                DiagnosticCodes.OVERLAY_SHAPE,
                DiagnosticCodes.OVERLAY_SHAPE,
            )
        }

        test("reads a document as it is without overlays") {
            val reading = OpenApi.reader().read(Source.Text(petstore, petstoreUri), emptyList())

            reading.document().info.title shouldBe "Petstore"
        }
    }
})
