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

package com.davils.kapi.spec.openapi.model.path

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val PET: String = "/pets/{petId}/toys/{toyId}"

class PathTemplateTest : FunSpec({
    context("a path template") {
        test("starts at the root and keeps its text") {
            PathTemplate(PET).value shouldBe PET
            PathTemplate(PET).toString() shouldBe PET
        }

        test("names its parameters in order") {
            PathTemplate(PET).parameters shouldBe listOf("petId", "toyId")
            PathTemplate("/pets").parameters shouldBe emptyList()
        }

        test("refuses a path that is not rooted") {
            val failure = shouldThrow<IllegalArgumentException> { PathTemplate("pets") }

            failure.message shouldContain "is no path template"
        }
    }

    context("parseOrNull") {
        test("reads a template and returns null for what is none") {
            PathTemplate.parseOrNull(PET) shouldBe PathTemplate(PET)
            PathTemplate.parseOrNull("pets") shouldBe null
        }
    }
})
