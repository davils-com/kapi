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

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

private val DRAFT_04: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.Draft04 }

private val DRAFT_07: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.Draft07 }

private val DRAFT_2019: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.Draft201909 }

private val OPENAPI_30: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.OpenApi30 }

private const val STRING_A: String = "\"a\""

private const val EMPTY_ARRAY: String = "[]"

private const val PAIR: String = "[1, 2]"

private const val SCHEMA_DEPTH: Int = 3

private val SAMPLES: List<String> = listOf("null", "true", "1", "1.5", STRING_A, EMPTY_ARRAY, "{}")

class KeywordShapeTest : FunSpec({
    test("a keyword of the wrong shape is a keyword error") {
        val keywords = listOf(
            """"required": "a"""", """"dependentRequired": {"a": "b"}""", """"type": ["string", 1]""",
            """"type": []""", """"maximum": []""", """"maximum": true""", """"exclusiveMinimum": "1"""",
            """"maxLength": "1"""", """"maxLength": []""", """"maxLength": true""", """"pattern": {}""",
            """"${'$'}dynamicRef": 1""",
        )
        keywords.forEach { keyword ->
            withClue(keyword) { codesOf("{$keyword}").toSet() shouldBe setOf(DiagnosticCodes.KEYWORD_INVALID) }
        }
        codesOf("""{"${'$'}recursiveRef": 1}""", DRAFT_2019) shouldBe listOf(DiagnosticCodes.KEYWORD_INVALID)
        codesOf("""{"dependencies": 1}""", DRAFT_07) shouldBe listOf(DiagnosticCodes.KEYWORD_INVALID)
        codesOf("""{"dependencies": {"a": [1]}}""", DRAFT_07) shouldBe listOf(DiagnosticCodes.KEYWORD_INVALID)
        codesOf("""{"type": ["string"]}""", OPENAPI_30) shouldBe listOf(DiagnosticCodes.KEYWORD_INVALID)
        codesOf("""{"not": "x"}""") shouldBe listOf(DiagnosticCodes.NOT_A_SCHEMA)
        codesOf("""{"patternProperties": {"(": {}}}""") shouldBe listOf(DiagnosticCodes.PATTERN_REFUSED)
        codesOf("""{"${'$'}ref": "a b"}""") shouldBe listOf(DiagnosticCodes.REFERENCE_UNRESOLVED)
    }

    test("counts are exact, saturate when huge and accept zero") {
        verify(schema = """{"maxLength": 0}""", valid = listOf("\"\""), invalid = listOf(STRING_A))
        verify(schema = """{"maxLength": 1e400}""", valid = listOf("\"abc\""), invalid = emptyList())
        verify(schema = """{"maxItems": 2.0}""", valid = listOf(PAIR), invalid = listOf("[1, 2, 3]"))
    }

    test("a literal deeper than the schema depth is refused") {
        val diagnostics = diagnosticsOf("""{"const": [[[[[1]]]]]}""") { limits { maximumSchemaDepth = SCHEMA_DEPTH } }
        diagnostics.map { it.code } shouldContain DiagnosticCodes.KEYWORD_INVALID
    }

    test("every keyword ignores the values it does not apply to") {
        val numbers = listOf("1", "1.5")
        val strings = listOf(STRING_A)
        val arrays = listOf(EMPTY_ARRAY)
        val objects = listOf("{}")
        val schemas = listOf(
            """{"maximum": 0, "exclusiveMaximum": 0, "minimum": 9, "exclusiveMinimum": 9, "multipleOf": 7}""" to
                numbers,
            """{"maxLength": 0, "minLength": 9, "pattern": "^x", "format": "date"}""" to strings,
            """{"items": false, "prefixItems": [false], "contains": false, "uniqueItems": true}""" to arrays,
            """{"minItems": 9, "maxItems": 0, "unevaluatedItems": false}""" to arrays,
            """{"properties": {"a": false}, "required": ["a"], "minProperties": 9, "propertyNames": false}""" to
                objects,
        )
        schemas.forEach { (schema, applicable) ->
            val compiled = schemaOf(schema) { isFormatAsserted = true }
            val ignored = SAMPLES - applicable.toSet()
            withClue(schema) { ignored.filter { sample -> !compiled.accepts(sample) } shouldBe emptyList() }
        }
        verify(
            schema = """{"items": [true], "additionalItems": false}""",
            valid = listOf("1", "[1]"),
            invalid = listOf(PAIR),
            block = DRAFT_07,
        )
    }

    test("keywords of other drafts and vocabularies are left alone") {
        verify(
            schema = """{"patternProperties": {"^a": {"type": "string"}}, "additionalProperties": false}""",
            valid = listOf("{}"),
            invalid = listOf("""{"ab": "x"}"""),
            block = OPENAPI_30,
        )
        verify(
            schema = """{"prefixItems": [{"type": "integer"}], "items": {"type": "string"}}""",
            valid = listOf("""["a"]"""),
            invalid = listOf("[1]"),
            block = OPENAPI_30,
        )
        verify(
            schema = """{"contains": {"type": "integer"}, "minContains": 2, "maxContains": 0}""",
            valid = listOf("[1]"),
            invalid = listOf("""["a"]"""),
            block = DRAFT_07,
        )
        verify(
            schema = """{"additionalProperties": true, "additionalItems": true, "items": [{}]}""",
            valid = listOf("""{"a": 1}""", PAIR),
            invalid = emptyList(),
            block = DRAFT_04,
        )
    }
})
