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

package com.davils.kapi.spec.core.diagnostic

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val PATHS: String = "paths"

private const val PETS: String = "/pets"

private val reported: JsonPointer = JsonPointer.of(PATHS, PETS, "get")

class PointerTreeTest : FunSpec({
    context("a pointer tree") {
        test("relates a place to a reported place, its ancestors, its siblings and its descendants") {
            val tree = PointerTree(listOf(reported))

            tree.isRelated(reported) shouldBe true
            tree.isRelated(JsonPointer.of(PATHS, PETS)) shouldBe true
            tree.isRelated(JsonPointer.of(PATHS, PETS, "put")) shouldBe true
            tree.isRelated(JsonPointer.of(PATHS, PETS, "get", "responses")) shouldBe true
            tree.isRelated(JsonPointer.of(PATHS, "/users", "get")) shouldBe false
            tree.isRelated(JsonPointer.of("info")) shouldBe false
        }

        test("knows whether it holds a place below another") {
            val tree = PointerTree(listOf(reported))

            tree.hasBelow(JsonPointer.of(PATHS)) shouldBe true
            tree.hasBelow(reported) shouldBe false
            tree.hasBelow(JsonPointer.of("info")) shouldBe false
        }

        test("relates nothing when nothing was reported") {
            val tree = PointerTree(emptyList())

            tree.isRelated(JsonPointer.ROOT) shouldBe false
            tree.hasBelow(JsonPointer.ROOT) shouldBe false
        }
    }
})
