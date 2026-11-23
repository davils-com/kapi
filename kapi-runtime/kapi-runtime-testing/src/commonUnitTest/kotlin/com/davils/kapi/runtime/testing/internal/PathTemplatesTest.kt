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

package com.davils.kapi.runtime.testing.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val PET: String = "/pets/{id}"

class PathTemplatesTest : FunSpec({
    context("matching") {
        test("binds every placeholder, also within a segment") {
            val match = PathTemplates.match(template = "/files/{name}.{kind}/v{n}", path = "/files/a%20b.tar.gz/v2")

            match?.parameters shouldBe listOf("name" to "a b", "kind" to "tar.gz", "n" to "2")
            match?.literals shouldBe "files.v".length
        }

        test("refuses other lengths, other literals and empty values") {
            PathTemplates.match(template = PET, path = "/pets") shouldBe null
            PathTemplates.match(template = "/pets", path = "/petsx") shouldBe null
            PathTemplates.match(template = PET, path = "/dogs/1") shouldBe null
            PathTemplates.match(template = PET, path = "/pets/") shouldBe null
            PathTemplates.match(template = "/pets/{a}{b}", path = "/pets/xy") shouldBe null
            PathTemplates.match(template = "/a-{x}", path = "/b-1") shouldBe null
            PathTemplates.match(template = "/{x}-end", path = "/1-other") shouldBe null
        }

        test("keeps a value it cannot decode as it is") {
            PathTemplates.match(template = "/{x}", path = "/%zz")?.parameters shouldBe listOf("x" to "%zz")
        }
    }

    context("templates") {
        test("start with a slash, balance their braces and name every placeholder") {
            PathTemplates.isTemplate("/pets/{id}") shouldBe true
            PathTemplates.isTemplate("/a/b{") shouldBe false
            PathTemplates.isTemplate("pets") shouldBe false
            PathTemplates.isTemplate("/{}") shouldBe false
        }
    }
})
