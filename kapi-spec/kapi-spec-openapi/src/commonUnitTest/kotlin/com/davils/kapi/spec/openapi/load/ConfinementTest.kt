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

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.openapi.fixture.configuration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val petstore: DocumentUri = DocumentUri("file:///work/api/petstore.yaml")

private fun uri(text: String): DocumentUri = DocumentUri(text)

class ConfinementTest : FunSpec({
    context("the default confinement") {
        val confinement = Confinement.of(configuration().references, petstore)

        test("contains the document itself and what lies in its folder or below") {
            confinement.contains(petstore) shouldBe true
            confinement.contains(uri("file:///work/api/models.yaml")) shouldBe true
            confinement.contains(uri("file:///work/api/shared/errors.yaml")) shouldBe true
        }

        test("excludes what lies beside or above the folder") {
            confinement.contains(uri("file:///work/secrets.yaml")) shouldBe false
            confinement.contains(uri("file:///work/api-internal/models.yaml")) shouldBe false
            confinement.contains(uri("file:///etc/passwd")) shouldBe false
        }

        test("excludes every way of climbing out of the folder") {
            val escapes = listOf(
                "file:///work/api/../secrets.yaml",
                "file:///work/api/shared/..",
                "file:///work/api/%2e%2e/secrets.yaml",
                "file:///work/api/..%2Fsecrets.yaml",
                "file:///work/api/..%5csecrets.yaml",
                "file:///work/api/models.yaml%00.png",
            )

            escapes.forEach { escape -> confinement.contains(uri(escape)) shouldBe false }
        }

        test("never meets a backslash, which no document uri holds") {
            DocumentUri.parseOrNull("file:///work/api/..\\secrets.yaml") shouldBe null
        }
    }

    context("a configured root") {
        test("replaces the folder of the document") {
            val wide = configuration { references { root = uri("file:///work/") } }
            val confinement = Confinement.of(wide.references, petstore)

            confinement.contains(uri("file:///work/shared/errors.yaml")) shouldBe true
            confinement.contains(uri("file:///etc/passwd")) shouldBe false
        }
    }

    context("a reader that allows no other document") {
        test("contains the document alone") {
            val alone = configuration { references { isExternalAllowed = false } }
            val confinement = Confinement.of(alone.references, petstore)

            confinement.contains(petstore) shouldBe true
            confinement.contains(uri("file:///work/api/models.yaml")) shouldBe false
        }
    }

    context("a document without a folder") {
        test("contains nothing but itself") {
            val confinement = Confinement.of(configuration().references, uri("urn:petstore"))

            confinement.contains(uri("urn:petstore")) shouldBe true
            confinement.contains(uri("urn:models")) shouldBe false
        }
    }
})
