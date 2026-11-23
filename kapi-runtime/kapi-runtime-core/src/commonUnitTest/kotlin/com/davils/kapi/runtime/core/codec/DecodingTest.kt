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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.builtins.serializer

private const val REASON: String = "broken"

private val nothing: Decoding<String?> = Decoding.Decoded(null)

class DecodingTest : FunSpec({
    context("a decoded value") {
        test("may be null") {
            nothing.hashCode() shouldBe 0
            nothing.toString() shouldBe "Decoded(null)"
            nothing shouldBe Decoding.Decoded(null)
        }

        test("differs from a failure and from another value") {
            nothing.equals(Decoding.Failed(REASON)) shouldBe false
            Decoding.Decoded(1) shouldNotBe Decoding.Decoded(2)
            Decoding.Decoded(REASON).hashCode() shouldBe REASON.hashCode()
        }
    }

    context("a failure") {
        test("is hashed by its reason and differs from a value") {
            Decoding.Failed(REASON).hashCode() shouldBe REASON.hashCode()
            Decoding.Failed(REASON).equals(nothing) shouldBe false
        }
    }

    context("the codecs") {
        test("are built with their defaults") {
            codecs().maxContentBytes shouldBe Codecs.Default.maxContentBytes
        }

        test("refuse a media type that is malformed") {
            Codecs.Default
                .decode(Int.serializer(), ByteString(), "not a type")
                .shouldBeInstanceOf<Decoding.Failed>()
                .reason shouldContain "no media type"
        }
    }
})
