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

package com.davils.kapi.spec.openapi.testing

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Refusal
import com.davils.kapi.spec.core.source.Resolution
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.bytestring.encodeToByteString

private const val TEXT: String = "openapi: 3.1.0"

private val petstore: DocumentUri = DocumentUri("memory:/petstore.yaml")

private val models: DocumentUri = DocumentUri("memory:/models.yaml")

private val outside: DocumentUri = DocumentUri("memory:/outside.yaml")

class MemoryResolverTest : FunSpec({
    context("the memory resolver") {
        test("finds the documents it was given") {
            val resolver = MemoryResolver(mapOf(petstore to TEXT))

            resolver.resolve(petstore) shouldBe Resolution.Found(TEXT.encodeToByteString())
        }

        test("misses every other document") {
            val resolver = MemoryResolver(mapOf(petstore to TEXT))

            resolver.resolve(models) shouldBe Resolution.Missing
        }

        test("answers a document the way a test asked for") {
            val refused = Resolution.Refused(Refusal.Confinement)
            val resolver = MemoryResolver(mapOf(petstore to TEXT), mapOf(outside to refused))

            resolver.resolve(outside) shouldBe refused
        }

        test("records what was asked for in order") {
            val resolver = MemoryResolver(mapOf(petstore to TEXT))

            resolver.resolve(models)
            resolver.resolve(petstore)

            resolver.requested shouldBe listOf(models, petstore)
        }
    }
})
