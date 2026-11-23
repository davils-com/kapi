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

package com.davils.kapi.spec.core.collection

import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val COLLIDING: Int = 4096

private const val THREE: Int = 3

class KeyingTest : FunSpec({
    context("keyed collections") {
        test("turn pairs into a map in their order, the last value of a key winning") {
            val map = listOf("b" to 1, "a" to 2, "b" to THREE).toKeyedMap()

            map.toList() shouldBe listOf("b" to THREE, "a" to 2)
            shouldThrowAny { (map as MutableMap<String, Int>)["c"] = 1 }
        }

        test("associate items by what a function makes of them") {
            listOf("a", "bb").associateKeyed { text -> text to text.length } shouldBe mapOf("a" to 1, "bb" to 2)
            listOf("a", "bb", "cc").associateByKeyed(String::length) shouldBe mapOf(1 to "a", 2 to "cc")
        }

        test("group items in the order of the first item of each group") {
            val groups = listOf("bb", "a", "cc", "d").groupByKeyed(String::length)

            groups shouldBe mapOf(2 to listOf("bb", "cc"), 1 to listOf("a", "d"))
            groups.keys.toList() shouldBe listOf(2, 1)
            shouldThrowAny { (groups.getValue(2) as MutableList<String>).add("ee") }
        }

        test("keep the first item of every key") {
            listOf("bb", "a", "cc", "d").distinctByKeyed(String::length) shouldBe listOf("bb", "a")
            val kept = mapOf("a" to 1, "b" to 2).filterKeyed { key, value -> key == "b" || value == THREE }
            kept shouldBe mapOf("b" to 2)
        }

        test("withstand keys whose hash codes collide") {
            val keys = collidingKeys(COLLIDING)
            val groups = (keys + keys).groupByKeyed { key -> key }

            groups.size shouldBe COLLIDING
            groups.values.all { group -> group.size == 2 } shouldBe true
            (keys + keys).distinctByKeyed { key -> key } shouldBe keys
        }
    }
})
