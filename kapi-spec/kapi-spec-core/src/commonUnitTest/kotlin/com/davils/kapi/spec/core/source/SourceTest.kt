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

package com.davils.kapi.spec.core.source

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val OPENAPI: String = "openapi"

private val petstore: DocumentUri = DocumentUri("file:///api/petstore.yaml")

class SourceTest : FunSpec({
    context("a text") {
        test("lives in memory unless it names where it came from") {
            Source.Text("openapi: 3.1.0").uri shouldBe DocumentUri.MEMORY
            Source.Text("openapi: 3.1.0", petstore).uri shouldBe petstore
        }

        test("renders its length and never its content") {
            Source.Text(OPENAPI).toString() shouldBe "Text(memory:/document, 7 characters)"
        }

        test("equals the text of the same content and address") {
            val text = Source.Text(OPENAPI, petstore)

            text shouldBe Source.Text(OPENAPI, petstore)
            text.hashCode() shouldBe Source.Text(OPENAPI, petstore).hashCode()
            text shouldNotBe Source.Text(OPENAPI)
            text shouldNotBe Source.Text("asyncapi", petstore)
            (text as Source == Source.Located(petstore)) shouldBe false
        }
    }

    context("a located source") {
        test("names the document a resolver has to find") {
            Source.Located(petstore).uri shouldBe petstore
            Source.Located(petstore).toString() shouldBe "Located(file:///api/petstore.yaml)"
        }

        test("equals the located source of the same address") {
            Source.Located(petstore) shouldBe Source.Located(petstore)
            Source.Located(petstore).hashCode() shouldBe petstore.hashCode()
            Source.Located(petstore) shouldNotBe Source.Located(DocumentUri.MEMORY)
            (Source.Located(petstore) as Source == Source.Text("", petstore)) shouldBe false
        }
    }
})
