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

package com.davils.kapi.spec.core.model

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.model.fixture.Node
import com.davils.kapi.spec.core.model.fixture.NodeBuilder
import com.davils.kapi.spec.core.model.fixture.node
import com.davils.kapi.spec.core.value.ExtensionName
import com.davils.kapi.spec.core.value.Extensions
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val PET: String = "pet"

private const val SECOND: String = "second"

private val vendor: ExtensionName = ExtensionName("x-vendor")

private val place: Location = Location(DocumentUri("memory:/api.yaml"), JsonPointer.of("paths"))

private val target: Target = Target(DocumentUri("memory:/api.yaml"), JsonPointer.of("components"))

private fun names(nodes: Sequence<Element>): List<String> = nodes.map { element -> (element as Node).name }.toList()

class ElementTest : FunSpec({
    context("the walk of an element") {
        test("visits the element and then every element it holds, depth first and in order") {
            val tree = node("root") {
                items += node("first") { items += node("nested") }
                named[SECOND] = node(SECOND)
                referable = Referable.Inline(node("inline"))
            }

            names(tree.walk()) shouldBe listOf("root", "first", "nested", SECOND, "inline")
        }

        test("does not follow a reference or anything that holds no element") {
            val tree = node("root") {
                referable = Referable.Referenced(Reference("#/components/schemas/Pet", target))
            }

            names(tree.walk()) shouldBe listOf("root")
            tree.children shouldBe emptyList()
        }
    }

    context("an element") {
        test("equals the element of the same class with the same content and extensions") {
            val first = node(PET) { extension(vendor, Value.Text("kide")) }
            val same = node(PET) { extension(vendor, Value.Text("kide")) }

            first shouldBe same
            first.hashCode() shouldBe same.hashCode()
        }

        test("differs by its content, its extensions and its class") {
            val pet = node(PET)
            val stranger = object : Element(NodeBuilder()) {
                override val identity: List<Any?> get() = pet.identityOf()
            }

            pet shouldNotBe node("toy")
            pet shouldNotBe node(PET) { extension(vendor, Value.Null()) }
            (pet == stranger) shouldBe false
            val text: Any = PET
            (pet == text) shouldBe false
        }

        test("names its class and where it was read") {
            val built = node(PET)
            val read = node(PET) { location = place }

            built.toString() shouldBe "Node(built)"
            read.toString() shouldBe "Node($place)"
            read.location shouldBe place
        }
    }

    context("the builder of an element") {
        test("takes over extensions read elsewhere and keeps the last value of a name") {
            val read = Extensions(mapOf(vendor to Value.Text("read")))
            val builder = NodeBuilder()
            builder.extension(vendor, Value.Text("written"))
            builder.adopt(read)

            builder.extensions shouldBe read
            builder.build().extensions shouldBe read
        }
    }
})

private fun Node.identityOf(): List<Any?> = listOf(name, items, named, referable)
