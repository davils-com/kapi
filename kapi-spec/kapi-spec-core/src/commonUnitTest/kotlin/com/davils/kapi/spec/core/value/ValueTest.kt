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

package com.davils.kapi.spec.core.value

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.collection.collidingKeys
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private val here: Location = Location(
    DocumentUri.MEMORY,
    JsonPointer.of("default"),
)

private const val SEVEN: Long = 7L

private const val COLLIDING: Int = 4096

private val seven: NumberValue = NumberValue.of(SEVEN)

class ValueTest : FunSpec({
    context("a value") {
        test("names its kind") {
            Value.Null().kind shouldBe ValueKind.Null
            Value.Flag(true).kind shouldBe ValueKind.Flag
            Value.Number(seven).kind shouldBe ValueKind.Number
            Value.Text("a").kind shouldBe ValueKind.Text
            Value.Sequence(emptyList()).kind shouldBe ValueKind.Sequence
            Value.Mapping(emptyMap()).kind shouldBe ValueKind.Mapping
        }

        test("equals a value of the same content wherever it was found") {
            Value.Null(here) shouldBe Value.Null()
            Value.Flag(true, here) shouldBe Value.Flag(true)
            Value.Number(seven, here) shouldBe Value.Number(NumberValue.of(SEVEN))
            Value.Text("a", here) shouldBe Value.Text("a")
            Value.Text("a", here).location shouldBe here
        }

        test("differs from a value of another content or kind") {
            Value.Flag(true) shouldNotBe Value.Flag(false)
            Value.Text("1") shouldNotBe Value.Number(NumberValue.of(1L))
            Value.Number(seven) shouldNotBe Value.Number(NumberValue.of(SEVEN + 1))
            Value.Null().equals("null") shouldBe false
            Value.Flag(true).equals("true") shouldBe false
            Value.Number(seven).equals("7") shouldBe false
            Value.Text("a").equals("a") shouldBe false
            Value.Sequence(emptyList()).equals(emptyList<String>()) shouldBe false
            Value.Mapping(emptyMap()).equals(emptyMap<String, String>()) shouldBe false
            Value.Text("a") shouldNotBe Value.Text("b")
        }

        test("has the hash of its content") {
            Value.Null(here).hashCode() shouldBe Value.Null().hashCode()
            Value.Flag(true).hashCode() shouldBe true.hashCode()
            Value.Number(seven).hashCode() shouldBe seven.hashCode()
            Value.Text("a").hashCode() shouldBe "a".hashCode()
        }

        test("renders like the JSON it stands for") {
            Value.Null().toString() shouldBe "null"
            Value.Flag(false).toString() shouldBe "false"
            Value.Number(seven).toString() shouldBe "7"
            Value.Text("a").toString() shouldBe "\"a\""
        }
    }

    context("a sequence") {
        test("keeps its items in order and is not changed through the list it was built from") {
            val items = mutableListOf<Value>(Value.Text("a"))
            val sequence = Value.Sequence(items, here)

            items += Value.Text("b")

            sequence.items shouldBe listOf(Value.Text("a"))
            sequence shouldBe Value.Sequence(listOf(Value.Text("a")))
            sequence.hashCode() shouldBe sequence.items.hashCode()
            sequence shouldNotBe Value.Sequence(emptyList())
            sequence.toString() shouldBe "[\"a\"]"
        }
    }

    context("a mapping") {
        test("keeps its entries in order and finds one by its key") {
            val entries = mutableMapOf<String, Value>("b" to Value.Flag(true))
            val mapping = Value.Mapping(entries, here)

            entries["a"] = Value.Null()

            mapping.entries.keys.toList() shouldBe listOf("b")
            mapping["b"] shouldBe Value.Flag(true)
            mapping["a"] shouldBe null
            mapping shouldBe Value.Mapping(mapOf("b" to Value.Flag(true)))
            mapping.hashCode() shouldBe mapping.entries.hashCode()
            mapping shouldNotBe Value.Mapping(emptyMap())
            mapping.toString() shouldBe "{\"b\"=true}"
        }

        test("cannot be changed through a cast of its entries") {
            val mapping = Value.Mapping(mapOf("a" to Value.Null()))

            shouldThrowAny { (mapping.entries as MutableMap<String, Value>)["b"] = Value.Null() }
            mapping.entries.keys shouldBe setOf("a")
        }

        test("withstands keys whose hash codes collide") {
            val keys = collidingKeys(COLLIDING)
            val entries = KeyedMap<String, Value>()
            keys.forEach { key -> entries[key] = Value.Text(key) }
            val mapping = Value.Mapping(entries)

            mapping.entries.size shouldBe COLLIDING
            keys.all { key -> mapping[key] == Value.Text(key) } shouldBe true
        }
    }

    context("the items of a sequence") {
        test("cannot be changed through a cast of its items") {
            val sequence = Value.Sequence(listOf(Value.Null()))

            shouldThrowAny { (sequence.items as MutableList<Value>).add(Value.Null()) }
            sequence.items.size shouldBe 1
        }
    }
})
