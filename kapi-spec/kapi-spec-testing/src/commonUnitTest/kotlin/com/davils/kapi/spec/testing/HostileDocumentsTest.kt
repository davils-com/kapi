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

package com.davils.kapi.spec.testing

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf

private val head: String = listOf("openapi: 3.1.0", "info:", "  title: Hostile", "  version: 1.0.0", "paths: {}")
    .joinToString(separator = "\n", postfix = "\n")

private const val WIDTH: Int = 9

private const val COUNT: Int = 3

private fun rootOf(text: String): YamlNode.Mapping {
    val result = Yaml.Default.parse(text)
    val parsed = result.shouldBeInstanceOf<YamlParseResult.Parsed>()
    result.diagnostics.shouldBeEmpty()
    val document = parsed.stream.single.shouldNotBeNull()
    return document.root.shouldBeInstanceOf<YamlNode.Mapping>()
}

private fun keysOf(text: String): List<String?> = rootOf(text).entries.map { entry -> entry.key.string() }

private fun YamlNode?.sequence(): YamlNode.Sequence = shouldBeInstanceOf<YamlNode.Sequence>()

class HostileDocumentsTest : FunSpec({
    context("every hostile document") {
        test("is a document of OpenAPI 3.1 that ends with a line break") {
            val documents = listOf(
                HostileDocuments.aliasBomb(1),
                HostileDocuments.deepNesting(1),
                HostileDocuments.unknownFields(1),
                HostileDocuments.referenceChain(1),
            )

            documents.forEach { document ->
                document shouldStartWith head
                document shouldEndWith "\n"
            }
        }
    }

    context("an alias bomb") {
        test("multiplies the nodes by nine on every level") {
            val bomb = HostileDocuments.aliasBomb(2)
            val root = rootOf(bomb)

            val top = root["x-level2"].sequence()
            val middle = top.items.first().sequence()
            val bottom = middle.items.first().sequence()

            top.items.size shouldBe WIDTH
            middle.items.size shouldBe WIDTH
            bottom.items.first().string() shouldBe "boom"
        }

        test("holds the first level alone at level zero") {
            val bomb = HostileDocuments.aliasBomb(0)

            val keys = keysOf(bomb).filterNotNull()

            keys.filter { key -> key.startsWith("x-level") } shouldBe listOf("x-level0")
        }
    }

    context("a deep nesting") {
        test("nests empty sequences as deep as asked") {
            HostileDocuments.deepNesting(COUNT) shouldContain "x-deep: [[[]]]\n"
            HostileDocuments.deepNesting(0) shouldContain "x-deep: \n"
        }
    }

    context("unknown fields") {
        test("are numbered from one") {
            val keys = keysOf(HostileDocuments.unknownFields(COUNT))

            keys.takeLast(COUNT) shouldBe listOf("unknown1", "unknown2", "unknown3")
        }

        test("are none when none are asked for") {
            HostileDocuments.unknownFields(0) shouldBe head
        }
    }

    context("a reference chain") {
        test("leads each response to the next and ends with a description") {
            val chain = HostileDocuments.referenceChain(2)
            val responses = rootOf(chain)["components"]["responses"]

            responses["R1"]["\$ref"].string() shouldBe "#/components/responses/R2"
            responses["R2"]["\$ref"].string() shouldBe "#/components/responses/R3"
            responses["R3"]["description"].string() shouldBe "The end"
        }

        test("is a single response when its length is zero") {
            val chain = HostileDocuments.referenceChain(0)

            chain shouldEndWith "  responses:\n    R1: {description: The end}\n"
        }
    }
})
