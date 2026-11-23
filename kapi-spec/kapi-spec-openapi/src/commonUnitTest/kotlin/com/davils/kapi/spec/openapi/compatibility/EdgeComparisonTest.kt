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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val MISSING: String = "'#/components/schemas/Missing'"

private fun get(text: String): String = """
    |paths:
    |  /pets:
    |    get:
    |$text
""".trimMargin()

private val dangling: String = get(
    """
        |      parameters:
        |        - {name: q, in: query}
        |        - {${'$'}ref: '#/components/parameters/Missing'}
        |      responses:
        |        '200': {${'$'}ref: '#/components/responses/Missing'}
        |        '201':
        |          description: OK
        |          headers:
        |            X-Total: {${'$'}ref: '#/components/headers/Missing'}
        |          content:
        |            application/json: {schema: {${'$'}ref: $MISSING}}
    """.trimMargin(),
)

class EdgeComparisonTest : FunSpec({
    context("a pair of documents") {
        test("without paths has no operations to compare") {
            val components = "components:\n  schemas:\n    Pet: {type: object}"

            (components to components).rules().shouldBeEmpty()
            (components to get("      responses: {'200': {description: OK}}")).rules() shouldBe listOf("path-added")
        }

        test("that refer to what they do not hold leave it out, and report a schema they cannot follow") {
            (dangling to dangling).rules() shouldBe listOf("incomplete")
        }

        test("with a path item they cannot follow compares nothing behind it") {
            val reference = "paths:\n  /pets: {${'$'}ref: '#/components/pathItems/Missing'}"

            (reference to reference).rules().shouldBeEmpty()
        }

        test("leave out a header whose schema becomes a content and an operation id that is dropped") {
            val before = get(
                """
                    |      operationId: listPets
                    |      responses:
                    |        '200':
                    |          description: OK
                    |          headers:
                    |            X-Total: {schema: {type: integer}}
                """.trimMargin(),
            )
            val after = get(
                """
                    |      responses:
                    |        '200':
                    |          description: OK
                    |          headers:
                    |            X-Total: {content: {text/plain: {}}}
                """.trimMargin(),
            )

            (before to after).rules().shouldBeEmpty()
        }

        test("leave out a deprecation that was there before and a security scheme that is removed") {
            val before = get(
                """
                    |      parameters:
                    |        - {name: q, in: query, deprecated: true, schema: {type: string}}
                    |      responses: {'200': {description: OK}}
                    |components:
                    |  securitySchemes:
                    |    key: {type: apiKey, name: X-Key, in: header}
                    |    gone: {${'$'}ref: '#/components/securitySchemes/Missing'}
                """.trimMargin(),
            )
            val after = before.replace(oldValue = "    key: {type: apiKey, name: X-Key, in: header}\n", newValue = "")

            (before to after).rules().shouldBeEmpty()
        }
    }
})
