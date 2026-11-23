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

package com.davils.kapi.spec.core.load

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val INVALID_BYTE: Int = 0xFF

private const val BYTE_ORDER_MARK: Int = 0xFEFF

private const val UMLAUTS: String = "title: Tierhandlung für Möpse"

class SourceTextTest : FunSpec({
    context("decode") {
        test("reads UTF-8") {
            SourceText.decode(UMLAUTS.encodeToByteString()) shouldBe UMLAUTS
        }

        test("drops a byte order mark") {
            val marked = Char(BYTE_ORDER_MARK) + UMLAUTS

            SourceText.decode(marked.encodeToByteString()) shouldBe UMLAUTS
        }

        test("refuses bytes that are no UTF-8 instead of replacing them") {
            SourceText.decode(ByteString(INVALID_BYTE.toByte(), 'a'.code.toByte())) shouldBe null
        }
    }
})
