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

package com.davils.kapi.runtime.core.codec

import com.davils.kapi.runtime.core.codec.fixture.Blob
import com.davils.kapi.runtime.core.codec.fixture.Nested
import com.davils.kapi.runtime.core.codec.fixture.Order
import com.davils.kapi.runtime.core.codec.fixture.Pet
import com.davils.kapi.runtime.core.codec.fixture.SilentSerializer
import com.davils.kapi.runtime.core.codec.fixture.UpperCodec
import com.davils.kapi.runtime.core.codec.form.formLimits
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private const val JSON: String = "application/json"

private const val FORM: String = "application/x-www-form-urlencoded"

private const val TEXT: String = "text/plain"

private const val OCTETS: String = "application/octet-stream"

private const val INVALID_BYTE: Int = 0xFF

private const val DEEP: Int = 100

private const val ANSWER: Int = 42

private val rex: Pet = Pet("Rex")

private fun decoded(text: String): Decoding<Pet> {
    val codec = Codecs.Default.codecFor(JSON).shouldNotBeNull()
    return codec.decode(Pet.serializer(), text.encodeToByteString())
}

class CodecsTest : FunSpec({
    context("the default codecs") {
        test("read and write JSON and what is structured as JSON") {
            val codecs = Codecs.Default

            codecs.supports(JSON) shouldBe true
            codecs.supports("application/problem+json; charset=utf-8") shouldBe true
            codecs.supports("text/json") shouldBe true
            codecs.supports("application/xml") shouldBe false
            codecs.supports("not a type") shouldBe false
            codecs.supports(null) shouldBe false
            codecs.toString() shouldContain "application/*+json"
        }

        test("write what has no value by leaving it out and ignore what they do not know") {
            val codec = Codecs.Default.codecFor(JSON).shouldNotBeNull()

            codec.encode(Pet.serializer(), rex).decodeToString() shouldBe """{"name":"Rex"}"""
            decoded("""{"name":"Rex","age":3}""") shouldBe Decoding.Decoded(rex)
        }
    }

    context("decoding JSON") {
        test("fails with a reason when the content is no JSON or has another shape") {
            decoded("{").shouldBeInstanceOf<Decoding.Failed>()
            decoded("""{"tag":"a"}""").shouldBeInstanceOf<Decoding.Failed>().reason shouldContain "name"
            decoded("""{"name":7.5,"tag":{}}""").shouldBeInstanceOf<Decoding.Failed>()
        }

        test("contains what the serializer of the application throws") {
            val codec = Codecs.Default.codecFor(JSON).shouldNotBeNull()
            val decoding = codec.decode(SilentSerializer, "1".encodeToByteString())

            decoding shouldBe Decoding.Failed("The content cannot be read as what was expected")
        }

        test("fails when the content is no UTF-8") {
            val codec = Codecs.Default.codecFor(JSON).shouldNotBeNull()
            val decoding = codec.decode(Pet.serializer(), ByteString(INVALID_BYTE.toByte()))

            decoding shouldBe Decoding.Failed("The content is no UTF-8")
        }

        test("refuses content that nests deeper than the limit before it is parsed") {
            val codecs = codecs { maxDepth = 2 }
            val codec = codecs.codecFor(JSON).shouldNotBeNull()

            val shallow = codec.decode(JsonElement.serializer(), "[[1]]".encodeToByteString())

            shallow.shouldBeInstanceOf<Decoding.Decoded<*>>()
            codec.decode(JsonElement.serializer(), "[[[1]]]".encodeToByteString()) shouldBe
                Decoding.Failed("The content nests deeper than 2 levels")
            val inString = codec.decode(JsonElement.serializer(), """["[[[["]""".encodeToByteString())

            inString.shouldBeInstanceOf<Decoding.Decoded<*>>()
            codec.decode(String.serializer(), """"\"[[["""".encodeToByteString()) shouldBe Decoding.Decoded(""""[[[""")
            val defaults = Codecs.Default.codecFor(JSON).shouldNotBeNull()

            defaults
                .decode(JsonElement.serializer(), "[".repeat(DEEP).encodeToByteString())
                .shouldBeInstanceOf<Decoding.Failed>()
        }

        test("shows the reason of a failure escaped") {
            Decoding.Failed("a\nb").toString() shouldBe "Failed(a\\nb)"
            Decoding.Decoded("x\u0000").toString() shouldNotContain "\u0000"
        }
    }

    context("decode") {
        test("reads content with the codec of its media type") {
            Codecs.Default.decode(Pet.serializer(), """{"name":"Rex"}""".encodeToByteString(), JSON) shouldBe
                Decoding.Decoded(rex)
        }

        test("refuses content above the limit, without a type and of a type no codec reads") {
            val codecs = codecs { maxContentBytes = 4 }
            val content = "12345".encodeToByteString()

            codecs.decode(
                Int.serializer(),
                content,
                JSON,
            ) shouldBe Decoding.Failed("The content is larger than 4 bytes")
            codecs
                .decode(Int.serializer(), "1".encodeToByteString(), null)
                .shouldBeInstanceOf<Decoding.Failed>()
                .reason shouldContain "no media type"
            codecs
                .decode(Int.serializer(), "1".encodeToByteString(), "image/png")
                .shouldBeInstanceOf<Decoding.Failed>()
                .reason shouldContain "No codec"
        }

        test("keep the limits at the hard ceiling and refuse one past it") {
            codecs { maxContentBytes = Codecs.HARD_CONTENT_BYTES }.maxContentBytes shouldBe Codecs.HARD_CONTENT_BYTES
            shouldThrow<ValidationException> { codecs { maxContentBytes = Codecs.HARD_CONTENT_BYTES + 1 } }
            shouldThrow<ValidationException> { codecs { maxDepth = 0 } }
        }
    }

    context("text/plain") {
        test("writes a text, a number or a flag as UTF-8 and refuses a structure") {
            val codec = Codecs.Default.codecFor(TEXT).shouldNotBeNull()

            codec.encode(String.serializer(), "grüß").decodeToString() shouldBe "grüß"
            codec.encode(Int.serializer(), ANSWER).decodeToString() shouldBe "42"
            shouldThrow<IllegalArgumentException> { codec.encode(Pet.serializer(), rex) }
        }

        test("reads the charsets it knows and refuses every other") {
            val latin = ByteString(byteArrayOf(0x67, 0xFC.toByte()))
            val codecs = Codecs.Default

            codecs.decode(String.serializer(), latin, "text/plain; charset=ISO-8859-1") shouldBe Decoding.Decoded("gü")
            codecs.decode(String.serializer(), "abc".encodeToByteString(), "text/plain; charset=us-ascii") shouldBe
                Decoding.Decoded("abc")
            codecs.decode(String.serializer(), latin, "text/plain; charset=us-ascii") shouldBe
                Decoding.Failed("The content is no us-ascii")
            codecs.decode(String.serializer(), latin, TEXT) shouldBe Decoding.Failed("The content is no utf-8")
            codecs.decode(String.serializer(), latin, "text/plain; charset=utf-16") shouldBe
                Decoding.Failed("The charset utf-16 is not supported")
            val text = codecs.codecFor(TEXT).shouldNotBeNull()

            text.decode(String.serializer(), "x".encodeToByteString()) shouldBe Decoding.Decoded("x")
        }
    }

    context("application/x-www-form-urlencoded") {
        val codec = Codecs.Default.codecFor(FORM).shouldNotBeNull()

        test("writes a flat object as a form, repeating the name of every item of a list") {
            val order = Order("tea cup", count = 2, tags = listOf("a&b", "ü"))

            codec.encode(Order.serializer(), order).decodeToString() shouldBe
                "item=tea+cup&count=2&tags=a%26b&tags=%C3%BC"
        }

        test("reads a form back as the object, a single item as a list") {
            val content = "item=tea+cup&count=2&tags=x".encodeToByteString()

            codec.decode(Order.serializer(), content) shouldBe Decoding.Decoded(Order("tea cup", 2, listOf("x")))
        }

        test("refuses a nested object and a malformed or oversized form") {
            val limited = codecs { formLimits = formLimits { maxFields = 1 } }
            val small = limited.codecFor(FORM).shouldNotBeNull()

            shouldThrow<IllegalArgumentException> { codec.encode(Nested.serializer(), Nested(Order("x"))) }
            codec.decode(Order.serializer(), "item=%zz".encodeToByteString()).shouldBeInstanceOf<Decoding.Failed>()
            small
                .decode(Order.serializer(), "item=a&count=1".encodeToByteString())
                .shouldBeInstanceOf<Decoding.Failed>()
                .reason shouldContain "TooManyFields"
        }
    }

    context("application/octet-stream") {
        test("writes and reads the bytes of a value written as Base64") {
            val codec = Codecs.Default.codecFor(OCTETS).shouldNotBeNull()
            val bytes = ByteString(byteArrayOf(0, 1, INVALID_BYTE.toByte()))
            val serializer = Blob.serializer()

            codec.decode(String.serializer(), bytes) shouldBe Decoding.Decoded("AAH/")
            codec.encode(String.serializer(), "AAH/") shouldBe bytes
            shouldThrow<IllegalArgumentException> { codec.encode(serializer, Blob(bytes)) }
        }
    }

    context("configured codecs") {
        test("ask a codec of the application before the built-in one") {
            val upper = UpperCodec(listOf(MediaRange.Json, MediaRange.of("text/upper")))
            val codecs = codecs { codec(upper) }

            codecs.codecFor(JSON) shouldBe upper
            codecs.codecFor(MediaType.parse("text/upper")) shouldBe upper
            codecs.supports("application/problem+json") shouldBe true
        }

        test("use the JSON they were given") {
            val strict = Json { ignoreUnknownKeys = false }
            val codecs = codecs { json = strict }
            val codec = codecs.codecFor(JSON).shouldNotBeNull()
            val decoding = codec.decode(Pet.serializer(), """{"name":"Rex","age":3}""".encodeToByteString())

            codecs.json shouldBe strict
            decoding.shouldBeInstanceOf<Decoding.Failed>()
        }
    }
})
