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

package com.davils.kapi.spec.openapi.load

import com.davils.kaml.core.node.get
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.fixture.yamlOf
import com.davils.kapi.spec.openapi.load.fixture.Loading
import com.davils.kore.core.text.TextPosition
import com.davils.kore.core.text.TextRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val INFO: String = "info"

private const val TITLE: String = "title"

private const val TITLE_LINE: String = "  title: Petstore"

private val text: String = yamlOf("openapi: 3.1.0", "info:", TITLE_LINE)

private val petstore: DocumentUri = DocumentUri("memory:/api/petstore.yaml")

private val title: JsonPointer = JsonPointer.of(INFO, TITLE)

private suspend fun loaded(): LoadedDocument {
    val loading = Loading(Source.Text(text, petstore))
    return loading.loader.root().shouldNotBeNull()
}

class LoadedDocumentTest : FunSpec({
    context("a loaded document") {
        test("locates a node by its range, its line and its column") {
            val document = loaded()
            val node = document.root[INFO][TITLE]

            val location = document.locate(title, node)

            location.document shouldBe petstore
            location.pointer shouldBe title
            location.range shouldBe node.shouldNotBeNull().range
            location.position shouldBe TextPosition(2, TITLE_LINE.indexOf("Petstore"))
        }

        test("locates what has no node by its pointer alone") {
            val document = loaded()

            document.locate(title, document.root["missing"]) shouldBe Location(petstore, title)
        }

        test("shows the line of a location that has a range") {
            val document = loaded()
            val nowhere: TextRange? = null
            val located = document.locate(title, document.root[INFO][TITLE])

            document.snippet(located)?.line shouldBe TITLE_LINE
            document.snippet(document.locate(title, nowhere)) shouldBe null
        }
    }
})
