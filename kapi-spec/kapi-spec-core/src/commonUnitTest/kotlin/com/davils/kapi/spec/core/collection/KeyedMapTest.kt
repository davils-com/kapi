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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val COLLIDING: Int = 4096

private const val THREE: Int = 3

private const val TEN: Int = 10

class KeyedMapTest : FunSpec({
    context("a keyed map") {
        test("keeps its keys in the order they were first put") {
            val map = KeyedMap<String, Int>()
            map["b"] = 1
            map["a"] = 2
            map["b"] = THREE

            map.keys.toList() shouldBe listOf("b", "a")
            map["b"] shouldBe THREE
            map.containsKey("a") shouldBe true
            map.containsKey("c") shouldBe false
            map.size shouldBe 2
        }

        test("keeps the first place of a key that is removed and put again") {
            val map = KeyedMap<String, Int>()
            map["a"] = 1
            map["b"] = 2

            map.remove("a") shouldBe 1
            map.remove("a") shouldBe null
            map["a"] = THREE

            map.toList() shouldBe listOf("a" to THREE, "b" to 2)
        }

        test("removes entries through its iterator and changes them through its entries") {
            val map = KeyedMap<String, Int>()
            map.putAll(mapOf("a" to 1, "b" to 2, "c" to THREE))
            val iterator = map.entries.iterator()
            val first = iterator.next()
            first.setValue(TEN) shouldBe 1
            iterator.next()
            iterator.remove()

            map shouldBe mapOf("a" to TEN, "c" to THREE)
            first.toString() shouldBe "a=10"
            first shouldBe map.entries.first()
            first.hashCode() shouldBe map.entries.first().hashCode()
            first.equals("a=10") shouldBe false
            val renamed = mapOf("b" to TEN)
            val changed = mapOf("a" to 1)
            first.equals(renamed.entries.first()) shouldBe false
            first.equals(changed.entries.first()) shouldBe false
            map.entries.size shouldBe 2
        }

        test("adds entries through its entries and answers whether it holds one") {
            val map = KeyedMap<String, Int>()
            val other = KeyedMap<String, Int>()
            other["a"] = 1
            val entry = other.entries.first()

            map.entries.add(entry) shouldBe true
            map.entries.add(entry) shouldBe false
            map.entries.contains(entry) shouldBe true
            other["a"] = 2
            map.entries.contains(entry) shouldBe false
        }

        test("refuses to remove before an entry was returned and to read past the end") {
            val map = KeyedMap<String, Int>()
            val iterator = map.entries.iterator()

            shouldThrow<IllegalStateException> { iterator.remove() }
            shouldThrow<NoSuchElementException> { iterator.next() }
        }

        test("refuses to read an entry whose key was removed") {
            val map = KeyedMap<String, Int>()
            map["a"] = 1
            val entry = map.entries.first()
            map.clear()

            shouldThrow<IllegalStateException> { entry.value }
            map.isEmpty() shouldBe true
        }

        test("withstands keys whose hash codes collide") {
            val keys = collidingKeys(COLLIDING)
            val map = KeyedMap<String, Int>()
            keys.forEachIndexed { index, key -> map[key] = index }

            keys.map { key -> key.hashCode() }.distinct().size shouldBe 1
            map.size shouldBe COLLIDING
            keys.withIndex().all { (index, key) -> map[key] == index } shouldBe true
            map.keys.toList() shouldBe keys
        }
    }
})
