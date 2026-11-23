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

package com.davils.kapi.spec.openapi.compatibility

import com.davils.kapi.spec.core.compatibility.Impact
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private fun responses(text: String): String = """
    |paths:
    |  /pets:
    |    get:
    |      responses:
    |$text
    |components:
    |  schemas:
    |    Pet:
    |      type: object
    |      required: [name]
    |      properties:
    |        name: {type: string}
    |        age: {type: integer}
    |    Named:
    |      type: object
    |      required: [name]
    |      properties:
    |        name: {type: string}
""".trimMargin()

private fun status(code: String, media: String = "application/json", schema: String = "Pet"): String = """
    |        '$code':
    |          description: OK
    |          content:
    |            $media: {schema: {${'$'}ref: '#/components/schemas/$schema'}}
""".trimMargin()

private val withHeader: String = status("200") + """
    |
    |          headers:
    |            X-Total: {schema: {type: integer}}
""".trimMargin()

class ResponseRulesTest : FunSpec({
    context("the responses of an operation") {
        test("report removed and added statuses") {
            (responses(status("200")) to responses(status("201"))).rules() shouldBe
                listOf("response-status-removed", "response-status-added")
        }

        test("report removed and added media types") {
            val json = responses(status("200"))
            val xml = responses(status(code = "200", media = "application/xml"))

            (json to xml).rules() shouldBe listOf("response-media-type-removed", "response-media-type-added")
            (json to xml).changes().last().provider shouldBe Impact.Breaking
        }

        test("report a removed header and compare the schemas of the headers") {
            val changed = withHeader.replace(oldValue = "type: integer", newValue = "type: string")

            (responses(withHeader) to responses(status("200"))).rules() shouldBe listOf("response-header-removed")
            (responses(withHeader) to responses(changed)).rules() shouldBe listOf("type-changed")
        }

        test("compare the schemas of their content as responses, through references") {
            val before = responses(status("200"))
            val after = responses(status(code = "200", schema = "Named"))
            val changes = (before to after).changes()

            changes.map(::ruleOf) shouldBe listOf("property-removed")
            changes.single().client shouldBe Impact.Breaking
            (before to before).rules().shouldBeEmpty()
        }
    }
})
