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


package com.davils.kapi.codegen.core.sample

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val LONG: Int = 20_000
private const val THREE: Int = 3

private fun count(edges: Map<String, List<String>>): Int {
    val components = SampleComponents(edges).of()
    return components.values.toSet().size
}

class SampleComponentsTest : FunSpec({
    context("of") {
        test("puts models that reach each other into one component") {
            val edges = mapOf("a" to listOf("b"), "b" to listOf("a", "c"), "c" to emptyList())
            val components = SampleComponents(edges).of()

            components["a"] shouldBe components["b"]
            components["c"] shouldNotBe components["a"]
        }

        test("keeps the models of a chain apart, also one that is only referred to") {
            count(mapOf("a" to listOf("b"), "b" to listOf("c", "c"))) shouldBe THREE
        }

        test("joins two circles that share a model") {
            val edges = mapOf("a" to listOf("b"), "b" to listOf("a", "c"), "c" to listOf("d"), "d" to listOf("b"))

            count(edges) shouldBe 1
        }

        test("walks a chain of any length without recursion") {
            val chain = (0 until LONG).associate { index -> "n$index" to listOf("n" + (index + 1)) }

            count(chain + ("n$LONG" to listOf("n0"))) shouldBe 1
        }
    }
})
