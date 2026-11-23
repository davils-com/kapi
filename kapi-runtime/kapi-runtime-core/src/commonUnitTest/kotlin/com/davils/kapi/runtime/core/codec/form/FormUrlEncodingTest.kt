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

package com.davils.kapi.runtime.core.codec.form

import com.davils.kore.core.outcome.Outcome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val SHORT: Int = 3

private const val INVALID_BYTE: Int = 0xFF

private val small: FormLimits = formLimits {
    maxLength = SHORT
    maxFieldLength = 1
}

private fun failed(reason: FormFailure): Outcome<List<Pair<String, String>>, FormFailure> = Outcome.Failure(reason)

class FormUrlEncodingTest : FunSpec({
    context("decoding text") {
        test("reads fields with the default limits and skips empty sequences") {
            FormUrlEncoding.decode("a=1&&b&c=x+y&") shouldBe
                Outcome.Success(listOf("a" to "1", "b" to "", "c" to "x y"))
        }

        test("refuses text above the limit") {
            FormUrlEncoding.decode("a=12", small) shouldBe failed(FormFailure.TooLong)
        }

        test("refuses a name or a value above the limit of a field") {
            FormUrlEncoding.decode("ab=", small) shouldBe failed(FormFailure.FieldTooLong)
            FormUrlEncoding.decode("a=bc", formLimits { maxFieldLength = 1 }) shouldBe failed(FormFailure.FieldTooLong)
        }

        test("refuses a malformed escape and a sequence that is no UTF-8 in the name and in the value") {
            FormUrlEncoding.decode("%zz=1") shouldBe failed(FormFailure.MalformedEscape)
            FormUrlEncoding.decode("a=%z") shouldBe failed(FormFailure.MalformedEscape)
            FormUrlEncoding.decode("%FF=1") shouldBe failed(FormFailure.NotUtf8)
            FormUrlEncoding.decode("a=%C3") shouldBe failed(FormFailure.NotUtf8)
            FormUrlEncoding.decode("%zz=%FF") shouldBe failed(FormFailure.MalformedEscape)
        }
    }

    context("decoding bytes") {
        test("reads UTF-8 with the default limits") {
            FormUrlEncoding.decode("a=%C3%BC".encodeToByteArray()) shouldBe Outcome.Success(listOf("a" to "ü"))
        }

        test("refuses bytes above the limit before they are decoded") {
            FormUrlEncoding.decode("a=12".encodeToByteArray(), small) shouldBe failed(FormFailure.TooLong)
            FormUrlEncoding.decode(byteArrayOf(INVALID_BYTE.toByte()).plus(ByteArray(SHORT)), small) shouldBe
                failed(FormFailure.TooLong)
        }

        test("refuses bytes that are no UTF-8") {
            FormUrlEncoding.decode(byteArrayOf(INVALID_BYTE.toByte())) shouldBe failed(FormFailure.NotUtf8)
        }
    }

    context("the limits") {
        test("have defaults and show them") {
            formLimits().toString() shouldBe "FormLimits(length=1048576, fields=1000, fieldLength=65536)"
        }
    }
})
