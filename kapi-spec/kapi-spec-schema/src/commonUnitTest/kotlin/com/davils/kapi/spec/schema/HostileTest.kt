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

package com.davils.kapi.spec.schema

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private const val WIDE: Int = 20_000

private const val SHALLOW_DEPTH: Int = 100

private const val STEP_BUDGET: Long = 1_000_000L

private const val PATTERNS: Int = 30

private const val LONG_TEXT: Int = 65_000

class HostileTest : FunSpec({
    test("a schema nested far too deep is refused, not overflowed") {
        val schema = nested(open = """{"items": """, center = "{}", close = "}", depth = 300)
        codesOf(schema) shouldContain DiagnosticCodes.LIMIT_EXCEEDED
    }

    test("a reference to itself that reads nothing is refused") {
        listOf(
            """{"${'$'}ref": "#"}""",
            """{"${'$'}defs": {"a": {"${'$'}ref": "#/${'$'}defs/b"}, """ +
                """"b": {"allOf": [{"${'$'}ref": "#/${'$'}defs/a"}]}}, "${'$'}ref": "#/${'$'}defs/a"}""",
            """{"${'$'}dynamicAnchor": "x", "anyOf": [{"${'$'}dynamicRef": "#x"}]}""",
        ).forEach { schema ->
            schemaOf(schema).refusalOf(json("1")).reason shouldBe RefusalReason.ReferenceCycle
        }
        val legacy =
            schemaOf(
                """{"definitions": {"a": {"${'$'}ref": "#/definitions/a"}}, "${'$'}ref": "#/definitions/a"}""",
            ) {
                defaultDialect = Dialect.Draft07
            }
        legacy.refusalOf(json("1")).reason shouldBe RefusalReason.ReferenceCycle
    }

    test("a recursive schema over a deep value is bounded by depth, not by the stack") {
        val value = (1..100_000).fold(JsonPrimitive(1) as JsonElement) { inner, _ -> JsonArray(listOf(inner)) }
        schemaOf("""{"items": {"${'$'}ref": "#"}}""").refusalOf(value).reason shouldBe RefusalReason.InstanceDepth
    }

    test("a long chain of dynamic references is bounded") {
        val definitions = (0 until 200).joinToString(", ") { index ->
            """"d$index": {"${'$'}id": "https://example.com/d$index", "${'$'}dynamicAnchor": "n", """ +
                """"${'$'}ref": "https://example.com/d${index + 1}"}"""
        }
        val schema = """{"${'$'}defs": {$definitions, "d200": {"${'$'}id": "https://example.com/d200", """ +
            """"${'$'}dynamicAnchor": "n", "items": {"${'$'}dynamicRef": "#n"}}}, """ +
            """"${'$'}ref": "https://example.com/d0"}"""
        val compiled = schemaOf(schema) { limits { maximumEvaluationDepth = SchemaLimits.CEILING_EVALUATION_DEPTH } }
        compiled.accepts("[1]") shouldBe true
        schemaOf(schema) { limits { maximumEvaluationDepth = SHALLOW_DEPTH } }.refusalOf(json("[1]")).reason shouldBe
            RefusalReason.EvaluationDepth
    }

    test("uniqueItems over many items stays fast and exact") {
        val distinct = JsonArray((0 until WIDE).map { JsonPrimitive(it) })
        val compiled = schemaOf("""{"uniqueItems": true}""")
        compiled.validate(distinct).isValid shouldBe true
        val duplicated = JsonArray(distinct + JsonPrimitive(WIDE / 2))
        compiled.validate(duplicated).isValid shouldBe false
        val objects = JsonArray((0 until WIDE).map { json("""{"a": $it, "b": [$it, "x"]}""") })
        compiled.validate(objects).isValid shouldBe true
    }

    test("nested alternatives that all fail run out of steps instead of time") {
        val schema = nested(
            open = """{"anyOf": [{"type": "string"}, """,
            center = """{"type": "string"}""",
            close = "]}",
            depth = 40,
        ).replace(
            oldValue = """{"anyOf": [{"type": "string"}, {"type": "string"}]}""",
            newValue = """{"anyOf": [{"type": "string"}, {"type": "boolean"}]}""",
        )
        val compiled = schemaOf(schema) { limits { maximumEvaluationSteps = 100_000L } }
        compiled.accepts("\"a\"") shouldBe true
        compiled.validate(json("1")).isValid shouldBe false
    }

    test("a combinatorial schema is refused by the step budget") {
        val branches = (0 until 10).joinToString(", ") { """{"${'$'}ref": "#/${'$'}defs/l1"}""" }
        val levels = (1..6).joinToString(", ") { level ->
            val next = (0 until 10).joinToString(", ") { """{"${'$'}ref": "#/${'$'}defs/l${level + 1}"}""" }
            """"l$level": {"items": {"anyOf": [$next]}}"""
        }
        val schema = """{"${'$'}defs": {$levels, "l7": {"type": "string"}}, "anyOf": [$branches]}"""
        val value = json(nested(open = "[", center = "1", close = "]", depth = 7))
        schemaOf(schema) { limits { maximumEvaluationSteps = STEP_BUDGET } }.refusalOf(value).reason shouldBe
            RefusalReason.EvaluationSteps
    }

    test("a pattern of many states over a long text pays for every state it runs") {
        val schema = """{"allOf": [""" + List(PATTERNS) { """{"pattern": ".{0,4998}b"}""" }.joinToString() + "]}"
        val text = JsonPrimitive("a".repeat(LONG_TEXT) + "b")
        val start = TimeSource.Monotonic.markNow()

        schemaOf(schema).refusalOf(text).reason shouldBe RefusalReason.EvaluationSteps
        start.elapsedNow() shouldBeLessThan 5.seconds
    }

    test("multipleOf with extreme exponents is exact and bounded") {
        verify(schema = """{"multipleOf": 1e-4000}""", valid = listOf("1e4000", "3"), invalid = emptyList())
        verify(
            schema = """{"multipleOf": 3e4000}""",
            valid = listOf("6e4000", "0"),
            invalid = listOf("1e4000", "1"),
        )
        val big = "9".repeat(400)
        verify(
            schema = """{"multipleOf": $big}""",
            valid = listOf(big, "${big}0"),
            invalid = listOf("1${big.drop(1)}"),
        )
    }

    test("keys an attacker chose do not slow lookups") {
        val keys = (0 until WIDE).joinToString(", ") { """"k${"0".repeat(20)}$it": $it""" }
        val compiled =
            schemaOf(
                """{"required": ["k${"0".repeat(20)}19999", "missing"], "propertyNames": {"maxLength": 40}}""",
            )
        val error = compiled.errorsOf("{$keys}").single()
        error.error shouldContain "missing"
    }
})
