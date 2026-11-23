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

package com.davils.kapi.spec.core.value

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private val here: Location = Location(
    DocumentUri.MEMORY,
    JsonPointer.of("x-kapi-name"),
)

private const val SECONDS: Long = 30L

private val name: ExtensionName = ExtensionName("x-kapi-name")

private val flag: ExtensionName = ExtensionName("x-kapi-ignore")

private val budget: ExtensionName = ExtensionName("x-kapi-timeout")

private val names: ExtensionName = ExtensionName("x-kapi-enum-names")

private val paging: ExtensionName = ExtensionName("x-kapi-pagination")

private val missing: ExtensionName = ExtensionName("x-kapi-group")

private val declared: Extensions = Extensions(
    mapOf(
        name to Value.Text("Pet", here),
        flag to Value.Flag(true),
        budget to Value.Number(NumberValue.of(SECONDS)),
        names to Value.Sequence(listOf(Value.Text("Cat"), Value.Text("Dog"))),
        paging to Value.Mapping(mapOf("style" to Value.Text("cursor"))),
    ),
)

class ExtensionsTest : FunSpec({
    context("extensions") {
        test("keep their entries in the order they were declared") {
            declared.names.toList() shouldBe listOf(name, flag, budget, names, paging)
            declared.isEmpty shouldBe false
            declared[name] shouldBe Value.Text("Pet")
            declared[missing] shouldBe null
        }

        test("are not changed through the map they were built from") {
            val entries = mutableMapOf<ExtensionName, Value>(flag to Value.Flag(true))
            val built = Extensions(entries)

            entries[name] = Value.Text("Pet")

            built.names shouldBe setOf(flag)
        }

        test("equal declared of the same entries") {
            val same = Extensions(declared.entries)

            declared shouldBe same
            declared.hashCode() shouldBe same.hashCode()
            declared shouldNotBe Extensions.EMPTY
            declared.equals(declared.entries) shouldBe false
            Extensions.EMPTY.isEmpty shouldBe true
            Extensions.EMPTY.toString() shouldBe "{}"
        }
    }

    context("a typed reading") {
        test("is present when the kind fits") {
            val style = mapOf("style" to Value.Text("cursor"))

            declared.text(name) shouldBe ExtensionReading.Present("Pet")
            declared[name]?.location shouldBe here
            declared.flag(flag) shouldBe ExtensionReading.Present(true)
            declared.number(budget) shouldBe ExtensionReading.Present(NumberValue.of(SECONDS))
            declared.texts(names) shouldBe ExtensionReading.Present(listOf("Cat", "Dog"))
            declared.mapping(paging) shouldBe ExtensionReading.Present(style)
        }

        test("is absent when the extension was not declared") {
            declared.text(missing) shouldBe ExtensionReading.Absent
            declared.flag(missing) shouldBe ExtensionReading.Absent
        }

        test("is mistyped with what was found when the kind does not fit") {
            val found = Value.Text("Pet")

            declared.flag(name) shouldBe ExtensionReading.Mistyped(found, ValueKind.Flag)
            declared.number(name) shouldBe ExtensionReading.Mistyped(found, ValueKind.Number)
            declared.texts(name) shouldBe ExtensionReading.Mistyped(found, ValueKind.Sequence)
            declared.mapping(name) shouldBe ExtensionReading.Mistyped(found, ValueKind.Mapping)
            declared.text(flag) shouldBe
                ExtensionReading.Mistyped(Value.Flag(true), ValueKind.Text)
        }

        test("is mistyped when a list of texts holds something else") {
            val mixed = Value.Sequence(listOf(Value.Text("Cat"), Value.Flag(true)))
            val built = Extensions(mapOf(names to mixed))

            built.texts(names) shouldBe ExtensionReading.Mistyped(mixed, ValueKind.Sequence)
        }
    }
})
