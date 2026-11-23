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

import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.codec.fixture.Order
import com.davils.kapi.runtime.core.codec.form.FormLimits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val NAME: String = "a"

private val codec: FormCodec = FormCodec(Json, FormLimits.Default)

private fun objectOf(member: JsonElement): JsonObject = JsonObject(mapOf(NAME to member))

private fun encoded(element: JsonElement): String = codec.encode(JsonElement.serializer(), element).decodeToString()

class FormCodecTest : FunSpec({
    context("writing") {
        test("leaves out a member without a value") {
            encoded(JsonObject(mapOf(NAME to JsonNull, "b" to JsonPrimitive(1)))) shouldBe "b=1"
        }

        test("refuses what is no object") {
            shouldThrow<IllegalArgumentException> { encoded(JsonPrimitive(1)) }
            shouldThrow<IllegalArgumentException> { encoded(JsonArray(emptyList())) }
        }

        test("refuses a list that holds no value or a structure") {
            shouldThrow<IllegalArgumentException> { encoded(objectOf(JsonArray(listOf(JsonNull)))) }
            shouldThrow<IllegalArgumentException> { encoded(objectOf(JsonArray(listOf(JsonObject(emptyMap()))))) }
            shouldThrow<IllegalArgumentException> { encoded(objectOf(JsonArray(listOf(JsonArray(emptyList()))))) }
        }
    }

    context("reading") {
        test("refuses a repeated field that is no list") {
            codec.decode(Order.serializer(), "item=a&item=b".encodeToByteString()).shouldBeInstanceOf<Decoding.Failed>()
        }

        test("reads a repeated field of a tree as a list") {
            codec.decode(JsonElement.serializer(), "a=1&a=2".encodeToByteString()) shouldBe
                Decoding.Decoded(objectOf(JsonArray(listOf(JsonPrimitive("1"), JsonPrimitive("2")))))
        }
    }
})
