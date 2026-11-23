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

package com.davils.kapi.spec.openapi.model.content

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val OK: Int = 200

class ResponseStatusTest : FunSpec({
    context("a response status") {
        test("is a code, a range or the default") {
            val ok = ResponseStatus("200")
            val client = ResponseStatus("4XX")

            ok.code shouldBe OK
            ok.isRange shouldBe false
            ok.isDefault shouldBe false
            client.code shouldBe null
            client.isRange shouldBe true
            ResponseStatus.Default.isDefault shouldBe true
            ResponseStatus.Default.toString() shouldBe "default"
        }

        test("refuses everything else") {
            listOf("", "20", "600", "2xx", "099", "Default", "2XXX").forEach { text ->
                val failure = shouldThrow<IllegalArgumentException> { ResponseStatus(text) }

                failure.message shouldContain "is no response status"
            }
        }
    }

    context("parseOrNull") {
        test("reads a status and returns null for what is none") {
            ResponseStatus.parseOrNull("404") shouldBe ResponseStatus("404")
            ResponseStatus.parseOrNull("teapot") shouldBe null
        }
    }
})
