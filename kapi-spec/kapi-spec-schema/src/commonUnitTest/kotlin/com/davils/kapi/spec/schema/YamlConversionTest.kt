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

package com.davils.kapi.spec.schema

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private val SCALARS: List<String> = listOf("a: ~", "b: true", "c: 12", "d: 1.5e3", "e: text", "f: [1, '2']")

class YamlConversionTest : FunSpec({
    test("a boolean that is none and a key that is no scalar are refused") {
        JsonSchema.json(yamlOf("? [a]\n: 1\n")).shouldBeInstanceOf<YamlConversion.Refused>()
        val uri = DocumentUri("https://example.com/s.yaml")
        val compilation = JsonSchema.compiler().compile(yamlOf("? [a]\n: 1\n"), uri)
        val diagnostic = compilation.shouldBeInstanceOf<Compilation.Invalid>().diagnostics.single()
        diagnostic.location?.document shouldBe uri
    }

    test("scalars convert by their tag") {
        val converted = JsonSchema.json(yamlOf(SCALARS.joinToString(separator = "\n", postfix = "\n")))
        converted.shouldBeInstanceOf<YamlConversion.Converted>().json shouldBe
            json("""{"a": null, "b": true, "c": 12, "d": 1.5e3, "e": "text", "f": [1, "2"]}""")
        converted.toString() shouldBe "Converted"
    }

    test("what JSON cannot express is refused with its place") {
        listOf("a: .inf\n", "a: 0x1F\n", "? [1]\n: 2\n", "a: !!binary aGk=\n").forEach { text ->
            JsonSchema.json(yamlOf(text)).shouldBeInstanceOf<YamlConversion.Refused>()
        }
        val refused = JsonSchema.json(yamlOf("a:\n  b: .nan\n")).shouldBeInstanceOf<YamlConversion.Refused>()
        refused.pointer.text shouldBe "/a/b"
        refused.toString() shouldContain "/a/b"
    }

    test("keys with the same text are refused") {
        val refused = JsonSchema.json(yamlOf("1: a\n'1': b\n")).shouldBeInstanceOf<YamlConversion.Refused>()
        refused.reason shouldContain "twice"
    }

    test("depth and nodes are bounded") {
        val deep = yamlOf(nested(open = "[", center = "1", close = "]", depth = 10))
        JsonSchema.json(deep, maximumDepth = 10).shouldBeInstanceOf<YamlConversion.Converted>()
        JsonSchema.json(deep, maximumDepth = 9).shouldBeInstanceOf<YamlConversion.Refused>()
        val wide = yamlOf("[1, 2, 3]")
        JsonSchema.json(wide, maximumNodes = 4).shouldBeInstanceOf<YamlConversion.Converted>()
        JsonSchema.json(wide, maximumNodes = 3).shouldBeInstanceOf<YamlConversion.Refused>()
    }
})
