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

package com.davils.kapi.codegen.core.emit.mock

import com.davils.kapi.codegen.core.fixture.ServedScenario
import com.davils.kapi.codegen.core.fixture.ServedScenarios
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val DOCUMENT: String = "mocks"

private val answers: List<String> = listOf(
    "listed 0 null: true",
    "created true",
    "unexpected 400",
    "found Rex of []",
    "deleted",
    "picture of 0 bytes",
    "animals 200: [PetstoreCat(kind=cat, name=string, rival=null)]",
    "tree 200: PetstoreLeaf(value=0)",
    "stamped 3s",
    "down",
)

private val samples: List<String> = listOf(
    "animal",
    "branch",
    "cat",
    "dog",
    "leaf",
    "owner",
    "ownerWrite",
    "pet",
    "petWrite",
    "tree",
)

private suspend fun played(): List<Any?> {
    val result = ServedScenarios.run(ServedScenario(DOCUMENT)) as? List<*>
    return result.orEmpty()
}

class GeneratedMocksTest : FunSpec({
    context("a generated client calling the generated mocks") {
        val played = played()

        test("gets the documented success of every operation, and a refusal for a request that is invalid") {
            played.take(answers.size) shouldBe answers
        }

        test("finds every sample valid by the rules of its model and reads it back from its JSON as itself") {
            played.drop(answers.size) shouldBe samples.map { name -> "$name: 0 errors, read back true" }
        }
    }
})
