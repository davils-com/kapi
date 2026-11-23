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

package com.davils.kapi.spec.core.value

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val KIDE: String = "kide"

private val secret: Value = Value.Text("secret")

class ExtensionReadingTest : FunSpec({
    context("a present value") {
        test("equals the reading of the same value") {
            val present = ExtensionReading.Present(KIDE)

            present shouldBe ExtensionReading.Present(KIDE)
            present.hashCode() shouldBe KIDE.hashCode()
            present shouldNotBe ExtensionReading.Present("kapi")
            (present as ExtensionReading<*> == ExtensionReading.Absent) shouldBe false
            present.toString() shouldBe "Present(value=kide)"
        }
    }

    context("a mistyped value") {
        test("equals the reading of the same value and expected kind, and shows only the kinds") {
            val mistyped = ExtensionReading.Mistyped(secret, ValueKind.Flag)

            mistyped shouldBe ExtensionReading.Mistyped(secret, ValueKind.Flag)
            mistyped.hashCode() shouldBe ExtensionReading.Mistyped(secret, ValueKind.Flag).hashCode()
            mistyped shouldNotBe ExtensionReading.Mistyped(Value.Text("other"), ValueKind.Flag)
            mistyped shouldNotBe ExtensionReading.Mistyped(secret, ValueKind.Number)
            (mistyped as ExtensionReading<*> == ExtensionReading.Absent) shouldBe false
            mistyped.toString() shouldBe "Mistyped(found=${ValueKind.Text}, expected=${ValueKind.Flag})"
        }
    }
})
