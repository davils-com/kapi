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

package com.davils.kapi.runtime.yaml

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.codec.codecs
import com.davils.kapi.runtime.yaml.fixture.FailingSerializer
import com.davils.kapi.runtime.yaml.fixture.Order
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private const val YAML: String = "application/yaml"

private const val ORDER: String = """id: 7
items:
  - book
  - pen
"""

private const val TWO_DOCUMENTS: String = """id: 7
---
id: 8
"""

private const val RESERVED: String = """%FUTURE directive
---
id: [7
"""

private const val DEPTH: Int = 64

private const val SMALL: Int = 100

private val order: Order = Order(id = 7, items = listOf("book", "pen"))

private val codec: YamlCodec = yamlCodec()

private fun decoded(text: String, codec: YamlCodec = com.davils.kapi.runtime.yaml.codec): Decoding<Order> =
    codec.decode(Order.serializer(), text.encodeToByteString())

private fun nested(levels: Int): String = "[".repeat(levels) + "]".repeat(levels)

private fun laughs(levels: Int): String {
    val lines = (1..levels).map { level ->
        val aliases = List(size = 9) { "*l${level - 1}" }
        "l$level: &l$level [${aliases.joinToString()}]"
    }
    return (listOf("l0: &l0 lol") + lines).joinToString(separator = "\n")
}

class YamlCodecTest : FunSpec({
    context("the media types") {
        test("are application/yaml, application/x-yaml and every +yaml type") {
            val codecs = codecs { yaml() }

            codecs.supports(YAML) shouldBe true
            codecs.supports("application/x-yaml") shouldBe true
            codecs.supports("application/openapi+yaml") shouldBe true
            codecs.supports("text/yaml") shouldBe false
            codecs.codecFor(YAML).shouldBeInstanceOf<YamlCodec>()
        }

        test("are only read once the codec is registered") {
            Codecs.Default.supports(YAML) shouldBe false
        }

        test("map values with the JSON of the codecs") {
            val lenient = Json { isLenient = true }
            val codecs = codecs {
                json = lenient
                yaml()
            }

            codecs.codecFor(YAML).shouldBeInstanceOf<YamlCodec>().json shouldBeSameInstanceAs lenient
        }
    }

    context("writing") {
        test("writes a value as the YAML of its JSON") {
            codec.encode(Order.serializer(), order).decodeToString() shouldContain "id: 7"
        }

        test("reads back what it wrote") {
            val bytes = codec.encode(Order.serializer(), order.copy(note = "gift: yes"))

            codec.decode(Order.serializer(), bytes) shouldBe Decoding.Decoded(order.copy(note = "gift: yes"))
        }

        test("throws for a value YAML cannot carry") {
            shouldThrow<IllegalArgumentException> { codec.encode(Double.serializer(), Double.NaN) }
            shouldThrow<IllegalStateException> { codec.encode(FailingSerializer, "value") }
        }
    }

    context("reading") {
        test("reads a mapping, a flow sequence and JSON itself") {
            decoded(ORDER) shouldBe Decoding.Decoded(order)
            decoded("{id: 7, items: [book, pen]}") shouldBe Decoding.Decoded(order)
            decoded("""{"id": 7, "items": ["book", "pen"]}""") shouldBe Decoding.Decoded(order)
        }

        test("reads what is free-form as JSON") {
            val tree = codec.decode(JsonElement.serializer(), "a: [1, true, null]".encodeToByteString())

            tree shouldBe Decoding.Decoded(Json.parseToJsonElement("""{"a": [1, true, null]}"""))
        }

        test("refuses content that does not fit the type") {
            decoded("id: seven").shouldBeInstanceOf<Decoding.Failed>()
            decoded("items: [book]").shouldBeInstanceOf<Decoding.Failed>()
            codec.decode(FailingSerializer, "x".encodeToByteString()) shouldBe
                Decoding.Failed("The content cannot be read as YAML of what was expected")
        }

        test("refuses malformed, empty and several documents") {
            decoded("id: [7").shouldBeInstanceOf<Decoding.Failed>()
            decoded("").shouldBeInstanceOf<Decoding.Failed>()
            decoded(TWO_DOCUMENTS) shouldBe Decoding.Failed("The content holds no document or more than one")
            codec.decode(Order.serializer(), ByteString(byteArrayOf(-1, 0, -1))).shouldBeInstanceOf<Decoding.Failed>()
        }

        test("names the first error, not a warning before it") {
            val failure = decoded(RESERVED).shouldBeInstanceOf<Decoding.Failed>()

            failure.reason shouldNotContain "reserve"
        }

        test("refuses what JSON cannot hold exactly, repeated keys and unknown tags") {
            decoded("? [1]\n: 7").shouldBeInstanceOf<Decoding.Failed>()
            decoded("id: 7\nid: 8").shouldBeInstanceOf<Decoding.Failed>()
            decoded("id: !point 7").shouldBeInstanceOf<Decoding.Failed>()
            decoded("id: 99999999999999999999999").shouldBeInstanceOf<Decoding.Failed>()
        }
    }

    context("hostile content") {
        test("refuses a billion laughs at the budget of aliases") {
            val lists = ListSerializer(JsonElement.serializer())

            val few = codec.decode(JsonElement.serializer(), laughs(levels = 2).encodeToByteString())
            val billion = codec.decode(JsonElement.serializer(), laughs(levels = 9).encodeToByteString())

            few.shouldBeInstanceOf<Decoding.Decoded<JsonElement>>()
            billion.shouldBeInstanceOf<Decoding.Failed>()
            codec.decode(lists, laughs(levels = 9).encodeToByteString()).shouldBeInstanceOf<Decoding.Failed>()
        }

        test("reads collections nested within the depth limit, the document counted, and refuses one level more") {
            val trees = JsonElement.serializer()

            val within = codec.decode(trees, nested(DEPTH - 1).encodeToByteString())

            within.shouldBeInstanceOf<Decoding.Decoded<JsonElement>>()
            codec.decode(trees, nested(DEPTH).encodeToByteString()) shouldBe
                Decoding.Failed("The document nests deeper than $DEPTH levels")
        }

        test("refuses a document longer than its limit") {
            val small = yamlCodec { limits { maximumInputLength = SMALL } }

            decoded(ORDER, small) shouldBe Decoding.Decoded(order)
            decoded(ORDER + "#".repeat(SMALL), small).shouldBeInstanceOf<Decoding.Failed>()
        }
    }

    context("the settings") {
        test("refuse limits above the ceilings of kapi") {
            shouldThrow<ValidationException> { yamlCodec { limits { maximumDepth = YamlCodec.HARD_DEPTH + 1 } } }
        }

        test("are kept and shown") {
            codec.limits.maximumDepth shouldBe DEPTH
            codec.json shouldBeSameInstanceAs Codecs.Default.json
            codec.toString() shouldContain "application/*+yaml"
        }

        test("cannot change once the codec is built") {
            val builder = YamlCodecBuilder()
            builder.build()

            shouldThrow<IllegalStateException> { builder.limits = builder.limits }
            shouldThrow<IllegalStateException> { builder.json = Json }
        }
    }
})
