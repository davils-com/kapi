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

package com.davils.kapi.runtime.core.codec.internal

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.codec.codecs
import com.davils.kapi.runtime.core.codec.fixture.Pet
import com.davils.kapi.runtime.core.codec.internal.fixture.floatVectors
import com.davils.kapi.runtime.core.codec.internal.fixture.readJson
import com.davils.kapi.runtime.core.codec.internal.fixture.readVectors
import com.davils.kapi.runtime.core.codec.internal.fixture.reasonOf
import com.davils.kapi.runtime.core.codec.internal.fixture.refusedVectors
import com.davils.kapi.runtime.core.codec.internal.fixture.writeVectors
import com.davils.kapi.runtime.core.codec.internal.fixture.written
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.double

private const val CBOR: String = "application/cbor"

private const val DEEP: Int = 300

private val rex: Pet = Pet("Rex")

class CborTest : FunSpec({
    context("the reader") {
        readVectors.forEach { (hex, json) ->
            test("reads $hex as $json") {
                readJson(hex) shouldBe Json.parseToJsonElement(json)
            }
        }

        floatVectors.forEach { (hex, number) ->
            test("reads $hex as the number $number") {
                readJson(hex).shouldBeInstanceOf<JsonPrimitive>().double shouldBe number
            }
        }

        refusedVectors.forEach { (hex, reason) ->
            test("refuses '$hex' because it $reason") {
                reasonOf(hex).shouldNotBeNull() shouldContain reason
            }
        }

        test("refuses what nests deeper than it follows") {
            reasonOf("81".repeat(DEEP) + "00").shouldNotBeNull() shouldContain "nests deeper"
            reasonOf("c0".repeat(DEEP) + "00").shouldNotBeNull() shouldContain "nests deeper"
        }

        test("keeps the first reason when more than one thing is wrong") {
            reasonOf("821c1bffffffffffffffff").shouldNotBeNull() shouldContain "argument 28"
        }
    }

    context("the writer") {
        writeVectors.forEach { (json, hex) ->
            test("writes $json as $hex") {
                written(json) shouldBe hex
            }
        }

        test("writes a list of 25 items with a length of one byte") {
            val items = List(CborFormat.DIRECT + 1) { 0 }

            written(items.toString()) shouldBe "9819" + "00".repeat(items.size)
        }

        @OptIn(ExperimentalSerializationApi::class)
        test("writes a literal that is no number it can keep as text") {
            val huge = CborWriter().write(JsonUnquotedLiteral("1e999"))
            val word = CborWriter().write(JsonUnquotedLiteral("abc"))

            huge.toByteArray().toHexString() shouldBe "653165393939"
            word.toByteArray().toHexString() shouldBe "63616263"
        }
    }

    context("the codec") {
        test("reads and writes CBOR and what is structured as CBOR") {
            val codecs = Codecs.Default

            codecs.supports(CBOR) shouldBe true
            codecs.supports("application/senml+cbor") shouldBe true
        }

        test("writes a value as the CBOR of its JSON and reads it back") {
            val codec = Codecs.Default.codecFor(CBOR).shouldNotBeNull()
            val content = codec.encode(Pet.serializer(), rex)

            content.toByteArray().toHexString() shouldBe "a1646e616d6563526578"
            codec.decode(Pet.serializer(), content) shouldBe Decoding.Decoded(rex)
        }

        test("fails with the reason of the reader when the content is no CBOR") {
            val codec = Codecs.Default.codecFor(CBOR).shouldNotBeNull()
            val decoding = codec.decode(Pet.serializer(), ByteString("18".hexToByteArray()))

            decoding.shouldBeInstanceOf<Decoding.Failed>().reason shouldContain "ends early"
        }

        test("fails when the content has another shape than expected") {
            val codec = Codecs.Default.codecFor(CBOR).shouldNotBeNull()
            val decoding = codec.decode(Pet.serializer(), ByteString("83010203".hexToByteArray()))

            decoding.shouldBeInstanceOf<Decoding.Failed>()
        }

        test("keeps the depth the codecs are configured with") {
            val codec = codecs { maxDepth = 2 }.codecFor(CBOR).shouldNotBeNull()
            val decoding = codec.decode(Pet.serializer(), ByteString("81818100".hexToByteArray()))

            decoding.shouldBeInstanceOf<Decoding.Failed>().reason shouldContain "deeper than 2"
        }
    }
})
