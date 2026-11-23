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
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val COLLIDING: Int = 8192

private const val THREE: Int = 3

class FrozenTest : FunSpec({
    context("a frozen map") {
        test("keeps the order and the content of its source") {
            val source = linkedMapOf("b" to 1, "a" to 2)
            val frozen = source.frozen()
            source["c"] = THREE

            frozen.keys.toList() shouldBe listOf("b", "a")
            frozen shouldBe mapOf("b" to 1, "a" to 2)
            mapOf("a" to 2, "b" to 1) shouldBe frozen
            frozen.hashCode() shouldBe mapOf("a" to 2, "b" to 1).hashCode()
            frozen["c"] shouldBe null
            frozen.containsKey("a") shouldBe true
            frozen.containsKey("c") shouldBe false
            frozen.toString() shouldBe "{b=1, a=2}"
        }

        test("answers whether it holds an entry") {
            val frozen = mapOf("a" to 1).frozen()
            val entry = frozen.entries.first()

            frozen.entries.contains(entry) shouldBe true
            val changed = mapOf("a" to 2)
            frozen.entries.contains(changed.entries.first()) shouldBe false
            val same = mapOf("a" to 1).entries.first()
            entry shouldBe same
            entry.hashCode() shouldBe same.hashCode()
            entry.equals("a=1") shouldBe false
            entry.toString() shouldBe "a=1"
            val renamed = mapOf("b" to 1)
            entry.equals(renamed.entries.first()) shouldBe false
            entry.equals(changed.entries.first()) shouldBe false
        }

        test("keeps the last value of a key its source holds twice") {
            val source = object : AbstractMap<String, Int>() {
                override val entries: Set<Map.Entry<String, Int>> = linkedSetOf(
                    mapOf("a" to 1).entries.first(),
                    mapOf("a" to 2).entries.first(),
                )
            }

            source.frozen() shouldBe mapOf("a" to 2)
        }

        test("is returned as it is when frozen again") {
            val frozen = mapOf("a" to 1).frozen()

            frozen.frozen() shouldBeSameInstanceAs frozen
        }

        test("cannot be changed through a cast") {
            val frozen = mapOf("a" to 1).frozen()

            shouldThrowAny { (frozen as MutableMap<String, Int>)["b"] = 2 }
            frozen shouldBe mapOf("a" to 1)
        }

        test("withstands keys whose hash codes collide") {
            val keys = collidingKeys(COLLIDING)
            val source = KeyedMap<String, Int>()
            keys.forEachIndexed { index, key -> source[key] = index }
            val frozen = source.frozen()

            frozen.size shouldBe COLLIDING
            keys.withIndex().all { (index, key) -> frozen[key] == index } shouldBe true
            frozen shouldBe source
        }
    }

    context("a frozen list") {
        test("keeps the order and the content of its source") {
            val source = mutableListOf("a", "b")
            val frozen = source.frozen()
            source += "c"

            frozen shouldBe listOf("a", "b")
            frozen.frozen() shouldBeSameInstanceAs frozen
        }

        test("cannot be changed through a cast") {
            val frozen = listOf("a").frozen()

            shouldThrowAny { (frozen as MutableList<String>).add("b") }
            frozen shouldBe listOf("a")
        }
    }

    context("a frozen set") {
        test("keeps the distinct items of its source in their order") {
            val frozen = listOf("b", "a", "b").frozenSet()

            frozen.toList() shouldBe listOf("b", "a")
            ("a" in frozen) shouldBe true
            ("c" in frozen) shouldBe false
            frozen shouldBe setOf("a", "b")
            frozen.frozenSet() shouldBeSameInstanceAs frozen
        }

        test("cannot be changed through a cast or its iterator") {
            val frozen = listOf("a").frozenSet()

            shouldThrowAny { (frozen as MutableSet<String>).add("b") }
            shouldThrowAny { (frozen.iterator() as MutableIterator<String>).remove() }
            frozen shouldBe setOf("a")
        }

        test("withstands items whose hash codes collide") {
            val keys = collidingKeys(COLLIDING)
            val frozen = (keys + keys).frozenSet()

            frozen.size shouldBe COLLIDING
            keys.all { key -> key in frozen } shouldBe true
        }
    }
})
