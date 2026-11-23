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

import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.CompatibilityCodes
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.fixture.readable
import com.davils.kapi.spec.testing.HostileDocuments
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith

private const val DEPTH: Int = 40

private const val SHALLOW: Int = 10

private const val VALUES: Int = 20_000

private const val COLLIDING: Int = 2048

private const val LIMIT: String = "LIMIT"

private const val INCOMPLETE: String = "incomplete"

private fun body(schema: String): String = """
    |paths:
    |  /pets:
    |    post:
    |      requestBody:
    |        content:
    |          application/json: {schema: $schema}
    |      responses:
    |        '200': {description: OK}
""".trimMargin()

private fun nested(depth: Int, leaf: String): String = "{type: array, items: ".repeat(depth) + leaf + "}".repeat(depth)

private val cyclic: String = body("{${'$'}ref: '#/components/schemas/Node'}") + """
    |
    |components:
    |  schemas:
    |    Node:
    |      type: object
    |      properties:
    |        next: {${'$'}ref: '#/components/schemas/Node'}
    |        tag: {type: string, maxLength: $LIMIT}
""".trimMargin()

private fun enumOf(count: Int, skip: Int): String {
    val values = (skip until count).joinToString(prefix = "[", postfix = "]") { index -> "v$index" }
    return "{type: string, enum: $values}"
}

class HostileComparisonTest : FunSpec({
    context("a hostile pair of documents") {
        test("of deep schemas is compared within the depth of the limits") {
            val before = body(nested(DEPTH, "{type: string}"))
            val after = body(nested(DEPTH, "{type: integer}"))

            (before to after).rules() shouldBe listOf("type-changed")
            (before to after).rules { limits { maximumDepth = SHALLOW } } shouldBe listOf(INCOMPLETE)
        }

        test("of cyclic schemas ends") {
            val before = cyclic.replace(oldValue = LIMIT, newValue = "10")
            val after = cyclic.replace(oldValue = LIMIT, newValue = "5")

            (before to after).rules() shouldBe listOf("max-length")
            (before to before).rules().shouldBeEmpty()
        }

        test("of huge enums is compared within the steps of the limits") {
            val before = body(enumOf(count = VALUES, skip = 0))
            val after = body(enumOf(count = VALUES, skip = 1))

            (before to after).rules() shouldBe listOf("enum-value-removed")
            (before to after).rules { limits { maximumNodes = VALUES.toLong() } } shouldBe listOf(INCOMPLETE)
        }

        test("with keys whose hash codes collide is compared like any other") {
            val text = HostileDocuments.collidingKeysDocument(COLLIDING)
            val reader = OpenApi.reader()
            val document = reader.read(Source.Text(text)).readable()
            val edited = text.replace(oldValue = "      type: object", newValue = "      type: string")
            val candidate = reader.read(Source.Text(edited)).readable()

            OpenApi.compare(baseline = document, candidate = document).changes.shouldBeEmpty()
            OpenApi.compare(baseline = document, candidate = candidate).changes.shouldBeEmpty()
        }

        test("reports at most the changes of the limits and says how many it dropped") {
            val before = "paths:\n" + (1..SHALLOW).joinToString(separator = "\n") { index -> "  /p$index: {}" }
            val changes = (before to "paths: {}").changes { limits { maximumDiagnostics = 2 } }

            changes.map(::ruleOf) shouldBe listOf("path-removed", "path-removed", INCOMPLETE)
            changes.last().message shouldContain "8 further changes were dropped"
        }
    }

    context("the configuration of a comparison") {
        test("refuses limits above their ceilings") {
            val document = documentOf("paths: {}")

            shouldThrow<ValidationException> {
                OpenApi.compare(baseline = document, candidate = document) { limits { maximumDepth = Int.MAX_VALUE } }
            }
        }
    }

    context("the codes of the comparison") {
        test("are named once each under openapi.compatibility") {
            CompatibilityCodes.all.shouldNotContainDuplicates()
            CompatibilityCodes.all.forEach { code -> code.value shouldStartWith "openapi.compatibility." }
            CompatibilityCodes.PATH_REMOVED.value shouldBe "openapi.compatibility.path-removed"
        }
    }
})
