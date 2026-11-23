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

private fun operation(parameters: String, body: String = ""): String = """
    |paths:
    |  /pets/{petId}:
    |    parameters:
    |      - {name: petId, in: path, required: true, schema: {type: string}}
    |    post:
    |      parameters:
    |$parameters
    |$body
    |      responses:
    |        '200': {description: OK}
""".trimMargin()

private fun parameter(text: String): String = "        - $text"

private fun body(required: Boolean, vararg media: String): String {
    val content = media.joinToString(separator = "\n") { type ->
        "          $type: {schema: {type: string, maxLength: 10}}"
    }
    return """
        |      requestBody:
        |        required: $required
        |        content:
        |$content
    """.trimMargin()
}

private const val QUERY: String = "{name: q, in: query, schema: {type: string}}"

private const val JSON: String = "application/json"

private const val NONE: String = "        []"

private const val STYLED: String = "{name: q, in: query, style: form, explode: false, schema: {type: string}}"

class RequestRulesTest : FunSpec({
    context("the parameters of an operation") {
        test("that are removed break clients") {
            (operation(parameter(QUERY)) to operation(NONE)).rules() shouldBe listOf("parameter-removed")
        }

        test("that are added break clients only when they are required") {
            val none = operation(NONE)
            val required = operation(parameter("{name: q, in: query, required: true, schema: {type: string}}"))

            (none to operation(parameter(QUERY))).rules() shouldBe listOf("parameter-added")
            (none to required).rules() shouldBe listOf("parameter-required-added")
            (none to required).changes().single().client shouldBe Impact.Breaking
        }

        test("report becoming required or optional, a new serialization and a deprecation") {
            val plain = operation(parameter("{name: q, in: query, schema: {type: string}}"))
            val required = operation(parameter("{name: q, in: query, required: true, schema: {type: string}}"))
            val styled = operation(parameter(STYLED))
            val deprecated = operation(parameter("{name: q, in: query, deprecated: true, schema: {type: string}}"))

            (plain to required).rules() shouldBe listOf("parameter-required")
            (required to plain).rules() shouldBe listOf("parameter-optional")
            (plain to styled).rules() shouldBe listOf("parameter-serialization")
            (plain to deprecated).rules() shouldBe listOf("parameter-deprecated")
        }

        test("compare their schemas as requests, also through their content") {
            val before = operation(parameter("{name: q, in: query, schema: {type: string, maxLength: 10}}"))
            val after = operation(parameter("{name: q, in: query, schema: {type: string, maxLength: 5}}"))
            val content = operation(parameter("{name: q, in: query, content: {$JSON: {schema: {type: integer}}}}"))

            (before to after).rules() shouldBe listOf("max-length")
            (before to after).changes().single().client shouldBe Impact.Breaking
            (before to content).rules() shouldBe listOf("type-changed", "max-length")
        }

        test("match headers without case and an operation parameter over a path parameter") {
            val before = operation(parameter("{name: X-Trace, in: header, schema: {type: string}}"))
            val after = operation(parameter("{name: x-trace, in: header, schema: {type: string}}"))
            val overridden = operation(parameter("{name: petId, in: path, required: true, schema: {type: integer}}"))

            (before to after).rules().shouldBeEmpty()
            (operation(NONE) to overridden).rules() shouldBe listOf("type-changed")
        }
    }

    context("the request body of an operation") {
        test("that is added breaks clients when it is required, and its removal breaks clients") {
            val none = operation(NONE)
            val optional = operation(parameters = NONE, body = body(required = false, JSON))
            val required = operation(parameters = NONE, body = body(required = true, JSON))

            (none to optional).rules() shouldBe listOf("request-body-added")
            (none to optional).changes().single().client shouldBe Impact.NonBreaking
            (none to required).changes().single().client shouldBe Impact.Breaking
            (required to none).rules() shouldBe listOf("request-body-removed")
        }

        test("reports becoming required or optional and its media types") {
            val optional = operation(parameters = NONE, body = body(required = false, JSON))
            val required = operation(parameters = NONE, body = body(required = true, JSON))
            val other = operation(parameters = NONE, body = body(required = false, "application/xml"))

            (optional to required).rules() shouldBe listOf("request-body-required")
            (required to optional).rules() shouldBe listOf("request-body-optional")
            (optional to other).rules() shouldBe listOf("request-media-type-removed", "request-media-type-added")
        }
    }
})
