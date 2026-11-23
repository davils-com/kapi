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

package com.davils.kapi.spec.openapi.read.info

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.readable
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.reading32
import com.davils.kapi.spec.openapi.fixture.rewritten
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val TAGS: String = "tags:"

private const val PARENT: String = "  - {name: pets, summary: Pets, kind: nav}"

private const val CHILD: String = "  - {name: cats, summary: Cats, parent: pets, kind: badge}"

class TagReadingTest : FunSpec({
    context("a tag of OpenAPI 3.2") {
        test("reads its summary, parent and kind") {
            val reading = reading32(TAGS, PARENT, CHILD)
            val child = reading.document().tags.last()

            reading.diagnostics.shouldBeEmpty()
            child.summary shouldBe "Cats"
            child.parent shouldBe "pets"
            child.kind shouldBe "badge"
        }

        test("is written with them and reads back") {
            val document = reading32(TAGS, PARENT, CHILD).document()
            val written = OpenApi.write(document)

            written shouldContain "parent: pets"
            written shouldContain "kind: badge"
            document.rewritten() shouldBe document
        }
    }

    context("a tag before OpenAPI 3.2") {
        test("has no summary, parent or kind") {
            val reading = reading31(TAGS, CHILD)
            val tag = reading.readable().tags.single()

            reading.codes() shouldBe List(3) { DiagnosticCodes.FIELD_VERSION }
            listOf(tag.summary, tag.parent, tag.kind) shouldBe listOf(null, null, null)
        }
    }
})
