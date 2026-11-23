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
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

private const val WIDE: Int = 20_000

class HostileTest : FunSpec({
    test("a schema nested far too deep is refused, not overflowed") {
        val schema = nested("""{"items": """, "{}", "}", 300)
        codesOf(schema) shouldContain DiagnosticCodes.LIMIT_EXCEEDED
    }

    test("a reference to itself that reads nothing is refused") {
        listOf(
            """{"${'$'}ref": "#"}""",
            """{"${'$'}defs": {"a": {"${'$'}ref": "#/${'$'}defs/b"},
                "b": {"allOf": [{"${'$'}ref": "#/${'$'}defs/a"}]}}, "${'$'}ref": "#/${'$'}defs/a"}""",
            """{"${'$'}dynamicAnchor": "x", "anyOf": [{"${'$'}dynamicRef": "#x"}]}""",
        ).forEach { schema ->
            schemaOf(schema).refusalOf(json("1")).reason shouldBe RefusalReason.ReferenceCycle
        }
        val legacy =
            schemaOf(
                """{"definitions": {"a": {"${'$'}ref": "#/definitions/a"}},
            "${'$'}ref": "#/definitions/a"}""",
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
            """"d$index": {"${'$'}id": "https://example.com/d$index", "${'$'}dynamicAnchor": "n",
                "${'$'}ref": "https://example.com/d${index + 1}"}"""
        }
        val schema = """{"${'$'}defs": {$definitions, "d200": {"${'$'}id": "https://example.com/d200",
            "${'$'}dynamicAnchor": "n", "items": {"${'$'}dynamicRef": "#n"}}},
            "${'$'}ref": "https://example.com/d0"}"""
        val compiled = schemaOf(schema) { limits { maximumEvaluationDepth = SchemaLimits.CEILING_EVALUATION_DEPTH } }
        compiled.accepts("[1]") shouldBe true
        schemaOf(schema) { limits { maximumEvaluationDepth = 100 } }.refusalOf(json("[1]")).reason shouldBe
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
        val schema = nested("""{"anyOf": [{"type": "string"}, """, """{"type": "string"}""", "]}", 40)
            .replace(
                """{"anyOf": [{"type": "string"}, {"type": "string"}]}""",
                """{"anyOf": [{"type": "string"},
                {"type": "boolean"}]}""",
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
        val value = json(nested("[", "1", "]", 7))
        schemaOf(schema) { limits { maximumEvaluationSteps = 1_000_000L } }.refusalOf(value).reason shouldBe
            RefusalReason.EvaluationSteps
    }

    test("multipleOf with extreme exponents is exact and bounded") {
        verify("""{"multipleOf": 1e-4000}""", listOf("1e4000", "3"), emptyList())
        verify("""{"multipleOf": 3e4000}""", listOf("6e4000", "0"), listOf("1e4000", "1"))
        val big = "9".repeat(400)
        verify("""{"multipleOf": $big}""", listOf(big, "${big}0"), listOf("1${big.drop(1)}"))
    }

    test("keys an attacker chose do not slow lookups") {
        val keys = (0 until WIDE).joinToString(", ") { """"k${"0".repeat(20)}$it": $it""" }
        val compiled =
            schemaOf(
                """{"required": ["k${"0".repeat(20)}19999", "missing"],
            "propertyNames": {"maxLength": 40}}""",
            )
        compiled
            .errorsOf("{$keys}")
            .single()
            .error
            .contains("missing") shouldBe true
    }
})
