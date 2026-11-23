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

package com.davils.kapi.spec.openapi.upgrade

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kapi.spec.openapi.upgrade.fixture.nodeOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val KEY: String = "a"

private val value: YamlNode = yamlScalar("b")

private val entries: List<YamlEntry> = Trees.entries(KEY to value)

private val sequenceKey: YamlEntry = YamlEntry(yamlSequence(emptyList()), value)

class TreesTest : FunSpec({
    context("a mapping built at the place of another node") {
        test("keeps where the mapping it replaces was read") {
            val origin = nodeOf("x: y")

            Trees.mappingAt(origin, entries).range shouldBe origin.range
            Trees.mappingAt(origin, entries).range.shouldNotBeNull()
        }

        test("is new when there was no mapping to replace") {
            Trees.mappingAt(null, entries).entries shouldBe entries
            Trees.mappingAt(value, entries).range.shouldBeNull()
        }
    }

    context("an entry whose key is no text") {
        test("is no extension") {
            Trees.isExtension(sequenceKey) shouldBe false
        }

        test("is kept as it is when entries are replaced or mapped") {
            val mapping = yamlMapping(listOf(sequenceKey))

            Trees.replaced(listOf(sequenceKey), mapOf(KEY to value)).single() shouldBeSameInstanceAs sequenceKey
            Trees.mapped(mapping) { _, node -> node }.entries.single() shouldBeSameInstanceAs sequenceKey
        }
    }

    context("the entries of a node") {
        test("are none when the node is no mapping") {
            Trees.entriesOf(value).shouldBeEmpty()
        }
    }
})
