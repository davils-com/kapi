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

package com.davils.kapi.codegen.core.operation

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.fixture.LIST_PETS
import com.davils.kapi.codegen.core.fixture.OK
import com.davils.kapi.codegen.core.fixture.clientFilesOf
import com.davils.kapi.codegen.core.fixture.clientGenerationOf
import com.davils.kapi.codegen.core.fixture.clientOf
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.generatedOf
import com.davils.kapi.codegen.core.fixture.get
import com.davils.kapi.codegen.core.fixture.paths
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val PETS_CLIENT: String = "client/PetstorePetsClient.kt"

private const val PAGED: String = "x-kapi-pagination: {style: link}"

private val pending: List<String> = listOf(
    PAGED,
)

class OperationPlannerTest : FunSpec({
    context("the name of an operation") {
        test("is its operationId") {
            clientOf(get(LIST_PETS)) shouldContain "public suspend fun listPets(): PetstoreListPetsResult {"
        }

        test("is its method and its path when it has no operationId") {
            clientOf(get()) shouldContain "public suspend fun getPets(): PetstoreGetPetsResult {"
        }

        test("is what x-kapi-name says") {
            val kotlin = clientOf(get(LIST_PETS, "x-kapi-name: pets"))

            kotlin shouldContain "public suspend fun pets(): PetstorePetsResult {"
            kotlin shouldContain """apiCall("pets", "GET")"""
        }

        test("is taken once in a client") {
            val lines = paths("/pets: {get: {operationId: pets, $OK}}", "/cats: {get: {operationId: Pets, $OK}}")

            clientGenerationOf(lines).codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
        }

        test("is taken once in a document, because its result is a type of its own") {
            val lines = paths(
                "/pets: {get: {operationId: pets, tags: [a], $OK}}",
                "/cats: {get: {operationId: Pets, tags: [b], $OK}}",
            )

            clientGenerationOf(lines).codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
        }
    }

    context("the client of an operation") {
        test("is named after its first tag") {
            clientFilesOf(get(LIST_PETS, "tags: [pets, cats]")) shouldContain PETS_CLIENT
        }

        test("is the default client without a tag") {
            clientFilesOf(get(LIST_PETS)) shouldContain "client/PetstoreDefaultClient.kt"
        }

        test("is what x-kapi-group says on its path") {
            val lines = paths("/cats: {x-kapi-group: pets, get: {tags: [cats], $OK}}")

            clientFilesOf(lines) shouldContain PETS_CLIENT
        }

        test("is what x-kapi-group says on the operation, before its path") {
            val lines = paths("/cats: {x-kapi-group: cats, get: {x-kapi-group: pets, $OK}}")

            clientFilesOf(lines) shouldContain PETS_CLIENT
        }
    }

    context("the operations of a document") {
        test("are written in the order of their paths and methods, whatever order the document has") {
            val first = "/cats: {post: {operationId: b, $OK}, get: {operationId: a, $OK}}"
            val second = "/pets: {get: {operationId: c, $OK}}"

            clientOf(paths(second, first)) shouldBe clientOf(paths(first, second))
        }

        test("leave out the one that x-kapi-ignore names") {
            val lines = paths("/pets: {get: {$LIST_PETS, $OK}, post: {operationId: hidden, x-kapi-ignore: true, $OK}}")
            val files = clientFilesOf(lines)

            files shouldContain "operation/PetstoreListPetsResult.kt"
            files shouldNotContain "operation/PetstoreHiddenResult.kt"
        }

        test("keep the one whose x-kapi-ignore is false") {
            val files = clientFilesOf(get(LIST_PETS, "x-kapi-ignore: false"))

            files shouldContain "operation/PetstoreListPetsResult.kt"
        }

        test("are not written without a client among the artifacts") {
            generatedOf(get(LIST_PETS)).files.size shouldBe 1
        }
    }

    context("a keyword the client does not serve yet") {
        pending.forEach { keyword ->
            test("is a warning on an operation: $keyword") {
                val generation = clientGenerationOf(get(LIST_PETS, keyword))

                generation.shouldBeInstanceOf<Generation.Generated>()
                generation.codes() shouldBe listOf(CodegenCodes.EXTENSION_PENDING)
            }
        }

        test("is one warning on a path, however many operations it has") {
            val lines = paths("/pets: {x-kapi-group: a, get: {$LIST_PETS, $PAGED, $OK}, post: {operationId: add, $OK}}")

            clientGenerationOf(lines).codes() shouldBe listOf(CodegenCodes.EXTENSION_PENDING)
        }
    }
})
