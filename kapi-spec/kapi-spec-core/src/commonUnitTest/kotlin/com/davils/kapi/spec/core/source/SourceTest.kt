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

private val petstore: DocumentUri = DocumentUri("file:///api/petstore.yaml")

class SourceTest : FunSpec({
    context("a text") {
        test("lives in memory unless it names where it came from") {
            Source.Text("openapi: 3.1.0").uri shouldBe DocumentUri.MEMORY
            Source.Text("openapi: 3.1.0", petstore).uri shouldBe petstore
        }

        test("renders its length and never its content") {
            Source.Text("openapi").toString() shouldBe "Text(memory:/document, 7 characters)"
        }
    }

    context("a located source") {
        test("names the document a resolver has to find") {
            Source.Located(petstore).uri shouldBe petstore
            Source.Located(petstore).toString() shouldBe "Located(file:///api/petstore.yaml)"
        }
    }
})
