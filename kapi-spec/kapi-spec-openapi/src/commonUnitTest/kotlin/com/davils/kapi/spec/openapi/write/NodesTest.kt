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

package com.davils.kapi.spec.openapi.write

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val oracle: Regex = Regex("-?[0-9]+")

private val literals: List<String> = listOf(
    "",
    "-",
    "0",
    "-0",
    "12",
    "-12",
    "--1",
    "+1",
    "1.0",
    "1e3",
    "1-",
    " 1",
    "١",
    "007",
)

class NodesTest : FunSpec({
    context("a number literal") {
        test("is an integer exactly when the regular expression of an integer accepts it") {
            literals.forEach { literal -> Nodes.isInteger(literal) shouldBe oracle.matches(literal) }
        }
    }
})
