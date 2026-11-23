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

package com.davils.kapi.spec.core.diagnostic

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.text.TextPosition
import com.davils.kore.core.text.TextRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private val petstore: DocumentUri = DocumentUri("file:///api/petstore.yaml")

private val pets: JsonPointer = JsonPointer.of("paths", "/pets")

private fun located(offset: Int, line: Int, column: Int): Location = Location(
    document = petstore,
    pointer = pets,
    range = TextRange.at(offset),
    position = TextPosition(line, column),
)

class LocationTest : FunSpec({
    context("a location") {
        test("renders the document and the pointer when it has no position") {
            Location(petstore, pets).toString() shouldBe "file:///api/petstore.yaml#/paths/~1pets"
        }

        test("renders the line and the column counted from one") {
            val location = located(offset = 40, line = 11, column = 4)

            location.toString() shouldBe "file:///api/petstore.yaml:12:5#/paths/~1pets"
        }

        test("equals a location of the same place") {
            val location = located(offset = 3, line = 0, column = 3)
            val same = located(offset = 3, line = 0, column = 3)

            location shouldBe same
            location.hashCode() shouldBe same.hashCode()
            location shouldNotBe Location(petstore, JsonPointer.ROOT)
            location.equals("file:///api/petstore.yaml") shouldBe false
        }
    }
})
