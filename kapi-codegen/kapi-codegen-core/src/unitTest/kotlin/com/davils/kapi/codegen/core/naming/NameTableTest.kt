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

package com.davils.kapi.codegen.core.naming

import com.davils.kapi.codegen.core.pipeline.Findings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val NAME: String = "Pet"

private const val OWNER: String = "The schema Pet"

class NameTableTest : FunSpec({
    context("claim") {
        test("names the first owner without a place when it has none") {
            val findings = Findings()
            val table = NameTable(findings)
            table.claim(NameClaim(name = NAME, origin = OWNER, location = null))
            table.claim(NameClaim(name = NAME, origin = "The schema pet", location = null))

            findings.sorted().single().message shouldBe
                "The schema pet becomes Pet, which The schema Pet already is as Pet; " +
                "name one of them with x-kapi-name"
        }

        test("gives every distinct name to its first claim") {
            val findings = Findings()
            val table = NameTable(findings)
            table.claim(NameClaim(name = NAME, origin = OWNER, location = null))
            table.claim(NameClaim(name = "Order", origin = "The schema Order", location = null))

            findings.hasErrors shouldBe false
        }
    }
})
