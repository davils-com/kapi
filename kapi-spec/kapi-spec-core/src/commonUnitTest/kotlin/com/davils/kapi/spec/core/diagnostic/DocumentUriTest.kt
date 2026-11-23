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

package com.davils.kapi.spec.core.diagnostic

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val PETSTORE: String = "file:///api/petstore.yaml"

class DocumentUriTest : FunSpec({
    context("a document uri") {
        test("keeps its text and names its scheme") {
            val uri = DocumentUri(PETSTORE)

            uri.value shouldBe PETSTORE
            uri.scheme shouldBe "file"
            uri.toString() shouldBe PETSTORE
        }

        test("refuses what has no scheme, a fragment or white space") {
            listOf(
                "",
                "petstore.yaml",
                "/api/petstore.yaml",
                "$PETSTORE#/info",
                "file:///a b.yaml",
                "1a:x",
            ).forEach { text ->
                val failure = shouldThrow<IllegalArgumentException> { DocumentUri(text) }

                failure.message shouldContain "is no document uri"
            }
        }

        test("names the document that only exists in memory") {
            DocumentUri.MEMORY.scheme shouldBe "memory"
        }
    }

    context("parseOrNull") {
        test("reads a uri") {
            DocumentUri.parseOrNull(PETSTORE) shouldBe DocumentUri(PETSTORE)
        }

        test("returns null for what is no uri") {
            DocumentUri.parseOrNull("petstore.yaml") shouldBe null
        }
    }
})
