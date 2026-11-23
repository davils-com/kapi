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

package com.davils.kapi.spec.openapi.overlay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val oracle: Regex = Regex("""1\.0\.(0|[1-9][0-9]*)""")

private val versions: List<String> = listOf(
    "",
    "1.0.",
    "1.0.0",
    "1.0.1",
    "1.0.10",
    "1.0.00",
    "1.0.01",
    "1.0.1a",
    "1.0.-1",
    "1.0.0 ",
    " 1.0.0",
    "1.0",
    "1.1.0",
    "1x0.0",
    "1.0.١",
    "2.0.0",
    "1.0.0.0",
)

class OverlayKeysTest : FunSpec({
    context("the version of an overlay") {
        test("is what the regular expression of Overlay 1.0 accepts") {
            versions.forEach { version -> OverlayKeys.isVersion(version) shouldBe oracle.matches(version) }
        }
    }
})
