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

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import io.kotest.core.spec.style.FunSpec

private const val OBJECT_A: String = """{"a": 1}"""

private const val OBJECT_B: String = """{"b": 1}"""

private const val PAIR: String = "[1, 2]"

private const val ARRAY_A: String = """["a"]"""

private const val STRING_A: String = "\"a\""

private const val FALSE: String = "false"

class KeywordTest : FunSpec({
    context("validation") {
        test("type") {
            verify(
                schema = """{"type": "integer"}""",
                valid = listOf("1", "1.0", "1e2", "-0"),
                invalid = listOf("1.5", "\"1\"", "null"),
            )
            verify(
                schema = """{"type": ["string", "null"]}""",
                valid = listOf(STRING_A, "null"),
                invalid = listOf("1", "[]", "{}", "true"),
            )
            verify(schema = """{"type": "number"}""", valid = listOf("1", "1.5"), invalid = listOf("true"))
            verify(schema = """{"type": "boolean"}""", valid = listOf(FALSE), invalid = listOf("0"))
            verify(schema = """{"type": "array"}""", valid = listOf("[]"), invalid = listOf("{}"))
            verify(schema = """{"type": "object"}""", valid = listOf("{}"), invalid = listOf("[]"))
        }

        test("const compares by JSON equality") {
            verify(
                schema = """{"const": {"a": [1, 2.0], "b": null}}""",
                valid = listOf("""{"b": null, "a": [1.0, 2]}"""),
                invalid = listOf(
                    """{"a": [2, 1], "b": null}""",
                    """{"a": [1, 2]}""",
                    """{"a": [1, 2], "b": null, "c": 1}""",
                ),
            )
            verify(schema = """{"const": false}""", valid = listOf(FALSE), invalid = listOf("0", "null"))
        }

        test("enum") {
            verify(
                schema = """{"enum": [1, "a", [true], {"x": 1}]}""",
                valid = listOf("1.0", STRING_A, "[true]", """{"x": 1}"""),
                invalid = listOf("2", "[1]"),
            )
        }

        test("multipleOf is exact") {
            verify(
                schema = """{"multipleOf": 0.01}""",
                valid = listOf("0.07", "19.99", "0", "-0.5", "\"x\""),
                invalid = listOf("0.075", "1e-3"),
            )
            verify(
                schema = """{"multipleOf": 0.5}""",
                valid = listOf("1e308", "1e4000", "2.5"),
                invalid = listOf("1.25"),
            )
            verify(
                schema = """{"multipleOf": 0.123456789}""",
                valid = listOf("0.246913578"),
                invalid = listOf("1e308"),
            )
            verify(
                schema = """{"multipleOf": 123456789012345678901234567890}""",
                valid = listOf("246913578024691357802469135780"),
                invalid = listOf("1e29"),
            )
            verify(
                schema = """{"multipleOf": 7e-400}""",
                valid = listOf("1.4e-399", "7"),
                invalid = listOf("1e-400"),
            )
            verify(schema = """{"multipleOf": 20}""", valid = listOf("1e10", "40"), invalid = listOf("10", "5e-1"))
        }

        test("bounds compare exact decimals") {
            verify(
                schema = """{"maximum": 18446744073709551615}""",
                valid = listOf("18446744073709551600", "18446744073709551615"),
                invalid = listOf("18446744073709551616"),
            )
            verify(
                schema = """{"exclusiveMaximum": 1.5}""",
                valid = listOf("1.4999999999999999999999"),
                invalid = listOf("1.5", "2"),
            )
            verify(
                schema = """{"minimum": -1e-5}""",
                valid = listOf("-0.00001", "0"),
                invalid = listOf("-0.000011"),
            )
            verify(
                schema = """{"exclusiveMinimum": 0}""",
                valid = listOf("1e-400"),
                invalid = listOf("0", "-1", "-0.0"),
            )
        }

        test("lengths count code points") {
            verify(
                schema = """{"maxLength": 2}""",
                valid = listOf("\"💩💩\"", "\"ab\"", "1"),
                invalid = listOf("\"abc\""),
            )
            verify(schema = """{"minLength": 2}""", valid = listOf("\"ab\""), invalid = listOf("\"💩\""))
        }

        test("pattern is not anchored") {
            verify(schema = """{"pattern": "b+"}""", valid = listOf("\"abbc\"", "1"), invalid = listOf("\"ac\""))
        }

        test("item counts and uniqueness") {
            verify(schema = """{"maxItems": 1}""", valid = listOf("[1]", "{}"), invalid = listOf(PAIR))
            verify(schema = """{"minItems": 1}""", valid = listOf("[1]"), invalid = listOf("[]"))
            verify(
                schema = """{"uniqueItems": true}""",
                valid = listOf(PAIR, "[[1], [true]]", """[{"a": 1}, {"a": 2}]""", "[0, false]", "[\"1\", 1]"),
                invalid = listOf(
                    "[1, 1.0]",
                    """[{"a": 1, "b": 2}, {"b": 2, "a": 1}]""",
                    "[[1, 2], [1, 2.0]]",
                    "[null, null]",
                ),
            )
            verify(schema = """{"uniqueItems": false}""", valid = listOf("[1, 1]"), invalid = emptyList())
        }

        test("contains with minContains and maxContains") {
            verify(
                schema = """{"contains": {"type": "string"}}""",
                valid = listOf("[1, \"a\"]", "{}"),
                invalid = listOf("[1]", "[]"),
            )
            verify(
                schema = """{"contains": {"type": "string"}, "minContains": 2, "maxContains": 3}""",
                valid = listOf("""["a", "b"]""", """["a", 1, "b", "c"]"""),
                invalid = listOf(ARRAY_A, """["a", "b", "c", "d"]"""),
            )
            verify(
                schema = """{"contains": {"type": "string"}, "minContains": 0}""",
                valid = listOf("[]", "[1]"),
                invalid = emptyList(),
            )
        }

        test("property counts, required and dependentRequired") {
            verify(
                schema = """{"maxProperties": 1}""",
                valid = listOf("{}", PAIR),
                invalid = listOf("""{"a": 1, "b": 2}"""),
            )
            verify(schema = """{"minProperties": 1}""", valid = listOf(OBJECT_A), invalid = listOf("{}"))
            verify(
                schema = """{"required": ["a"]}""",
                valid = listOf("""{"a": null}""", "[]"),
                invalid = listOf(OBJECT_B),
            )
            verify(
                schema = """{"dependentRequired": {"a": ["b"]}}""",
                valid = listOf("{}", """{"a": 1, "b": 2}""", OBJECT_B),
                invalid = listOf(OBJECT_A),
            )
        }

        test("format is an annotation unless it asserts") {
            verify(schema = """{"format": "date"}""", valid = listOf("\"not a date\"", "1"), invalid = emptyList())
            val asserting: CompilerBuilder.() -> Unit = { isFormatAsserted = true }
            verify(
                schema = """{"format": "date"}""",
                valid = listOf("\"2024-02-29\"", "1"),
                invalid = listOf("\"2023-02-29\"", "\"2024-13-01\""),
                block = asserting,
            )
            verify(
                schema = """{"format": "date-time"}""",
                valid = listOf("\"2024-01-01T23:59:60Z\"", "\"2024-01-01T10:00:00.5+01:00\""),
                invalid = listOf("\"2024-01-01T10:00:60Z\"", "\"2024-01-01 10:00:00Z\"", "\"2024-01-01T10:00:00.Z\""),
                block = asserting,
            )
            verify(
                schema = """{"format": "ipv4"}""",
                valid = listOf("\"192.168.0.1\""),
                invalid = listOf("\"256.1.1.1\"", "\"01.1.1.1\"", "\"1.1.1\""),
                block = asserting,
            )
            verify(
                schema = """{"format": "uuid"}""",
                valid = listOf("\"2EB8AA08-AA98-11EA-B4AA-73B441D16380\""),
                invalid = listOf("\"2eb8aa08aa98-11ea-b4aa-73b441d16380\""),
                block = asserting,
            )
            verify(
                schema = """{"format": "email"}""",
                valid = listOf("\"joe.bloggs@example.com\"", "\"a@[127.0.0.1]\""),
                invalid = listOf("\"joe..bloggs@example.com\"", "\"@example.com\"", "\"a@-b.com\""),
                block = asserting,
            )
            verify(
                schema = """{"format": "unknown-format"}""",
                valid = listOf("\"anything\""),
                invalid = emptyList(),
                block = asserting,
            )
        }

        test("content and meta-data keywords are annotations") {
            verify(
                schema = """{"contentMediaType": "application/json", "contentEncoding": "base64", "title": "t", """ +
                    """"deprecated": true, "default": 1, "examples": [2], "x-unknown": {"type": "string"}}""",
                valid = listOf("\"{not json\"", "1"),
                invalid = emptyList(),
            )
        }
    }

    context("applicators") {
        test("properties, patternProperties and additionalProperties") {
            verify(
                schema =
                """{"properties": {"a": {"type": "integer"}}, "patternProperties": {"^x-": {"type": "string"}}, """ +
                    """"additionalProperties": false}""",
                valid = listOf("""{"a": 1, "x-b": "c"}""", "{}", "[1]"),
                invalid = listOf("""{"a": "1"}""", """{"x-b": 1}""", OBJECT_B),
            )
        }

        test("propertyNames") {
            verify(
                schema = """{"propertyNames": {"maxLength": 2}}""",
                valid = listOf("""{"ab": 1}""", "[]"),
                invalid = listOf("""{"abc": 1}"""),
            )
        }

        test("dependentSchemas") {
            verify(
                schema = """{"dependentSchemas": {"a": {"required": ["b"]}}}""",
                valid = listOf("{}", """{"a": 1, "b": 1}"""),
                invalid = listOf(OBJECT_A),
            )
        }

        test("prefixItems and items") {
            verify(
                schema = """{"prefixItems": [{"type": "integer"}, {"type": "string"}], "items": {"type": "boolean"}}""",
                valid = listOf("[1, \"a\", true]", "[1]", "[]", "{}"),
                invalid = listOf(ARRAY_A, "[1, \"a\", 2]"),
            )
            verify(schema = """{"items": false}""", valid = listOf("[]"), invalid = listOf("[1]"))
        }

        test("allOf, anyOf, oneOf and not") {
            verify(
                schema = """{"allOf": [{"minimum": 1}, {"maximum": 2}]}""",
                valid = listOf("1.5"),
                invalid = listOf("3", "0"),
            )
            verify(
                schema = """{"anyOf": [{"type": "string"}, {"minimum": 2}]}""",
                valid = listOf(STRING_A, "3"),
                invalid = listOf("1"),
            )
            verify(
                schema = """{"oneOf": [{"type": "integer"}, {"minimum": 2}]}""",
                valid = listOf("1", "2.5"),
                invalid = listOf("3", "1.5"),
            )
            verify(schema = """{"not": {"type": "string"}}""", valid = listOf("1"), invalid = listOf(STRING_A))
        }

        test("if, then and else") {
            verify(
                schema = """{"if": {"type": "integer"}, "then": {"minimum": 0}, "else": {"type": "string"}}""",
                valid = listOf("1", STRING_A),
                invalid = listOf("-1", "1.5"),
            )
            verify(
                schema = """{"then": {"type": "string"}, "else": {"type": "string"}}""",
                valid = listOf("1"),
                invalid = emptyList(),
            )
            verify(
                schema = """{"if": {"type": "integer"}, "then": {"minimum": 0}}""",
                valid = listOf("1", "-1.5"),
                invalid = listOf("-1"),
            )
        }

        test("unevaluatedProperties sees what adjacent and in-place keywords evaluated") {
            verify(
                schema = """{"properties": {"a": true}, "allOf": [{"properties": {"b": true}}], """ +
                    """"anyOf": [{"properties": {"c": true}}, {"properties": {"d": true}}], """ +
                    """"if": {"properties": {"e": {"const": 1}}}, "then": {"properties": {"f": true}}, """ +
                    """"unevaluatedProperties": false}""",
                valid = listOf("""{"a": 1, "b": 1, "c": 1, "d": 1}""", """{"e": 1, "f": 1}"""),
                invalid = listOf("""{"g": 1}""", """{"e": 2, "f": 1}"""),
            )
            verify(
                schema = """{"anyOf": [{"properties": {"a": {"type": "string"}}}, true], """ +
                    """"unevaluatedProperties": false}""",
                valid = listOf("""{"a": "x"}"""),
                invalid = listOf(OBJECT_A),
            )
            verify(
                schema = """{"not": {"not": {"properties": {"a": true}}}, "unevaluatedProperties": false}""",
                valid = listOf("{}"),
                invalid = listOf(OBJECT_A),
            )
            verify(
                schema = """{"properties": {"a": {"unevaluatedProperties": false, "properties": {"b": true}}}, """ +
                    """"unevaluatedProperties": {"type": "string"}}""",
                valid = listOf("""{"a": {"b": 1}, "c": "x"}"""),
                invalid = listOf("""{"a": {"b": 1, "c": 1}}""", """{"c": 1}"""),
            )
        }

        test("unevaluatedItems sees prefixItems, items, contains and references") {
            verify(
                schema = $$"""{"prefixItems": [true], "contains": {"type": "string"}, """ +
                    $$""""unevaluatedItems": {"type": "integer"}}""",
                valid = listOf("[null, \"a\", 1]", "[null, \"a\"]"),
                invalid = listOf("[null, \"a\", true]"),
            )
            verify(
                schema =
                $$"""{"$ref": "#/$defs/pair", "unevaluatedItems": false, "$defs": {"pair": {"prefixItems": [true, """ +
                    $$"""true]}}}""",
                valid = listOf(PAIR),
                invalid = listOf("[1, 2, 3]"),
            )
        }
    }

    context("core") {
        test("references to definitions, anchors and embedded resources") {
            verify(
                schema = $$"""{"$id": "https://example.com/root", "properties": {"a": {"$ref": "#/$defs/a"}, """ +
                    $$""""b": {"$ref": "#b"}, "c": {"$ref": "c.json"}}, "$defs": {"a": {"type": "string"}, """ +
                    $$""""b": {"$anchor": "b", "type": "integer"}, "c": {"$id": "c.json", "maximum": 3}}}""",
                valid = listOf("""{"a": "x", "b": 1, "c": 3}"""),
                invalid = listOf(OBJECT_A, """{"b": "x"}""", """{"c": 4}"""),
            )
        }

        test("escaped JSON Pointers resolve") {
            verify(
                schema =
                $$"""{"$defs": {"a/b": {"type": "string"}, "c~d": {"type": "integer"}, "e%f": {"type": "null"}}, """ +
                    $$""""allOf": [{"$ref": "#/$defs/a~1b"}], "properties": {"x": {"$ref": "#/$defs/c~0d"}, """ +
                    $$""""y": {"$ref": "#/$defs/e%25f"}}}""",
                valid = listOf("\"s\"", "\"t\""),
                invalid = listOf("1"),
            )
        }

        test("boolean schemas") {
            verify(schema = "true", valid = listOf("1", "null"), invalid = emptyList())
            verify(schema = FALSE, valid = emptyList(), invalid = listOf("1", "null"))
        }

        test("a dynamic reference resolves to the outermost dynamic anchor in scope") {
            val base = $$"""{"$id": "https://example.com/list", "type": "array", """ +
                $$""""items": {"$dynamicRef": "#item"}, "$defs": {"item": {"$dynamicAnchor": "item"}}}"""
            val strings = $$"""{"$id": "https://example.com/strings", "$ref": "list", """ +
                $$""""$defs": {"item": {"$dynamicAnchor": "item", "type": "string"}}}"""
            verify(schema = strings, valid = listOf(ARRAY_A), invalid = listOf("[1]")) {
                schema(DocumentUri("https://example.com/list"), json(base))
            }
            verify(schema = base, valid = listOf("[1]"), invalid = listOf("1"))
        }

        test("a reference into an unknown keyword compiles that place as a schema") {
            verify(
                schema = $$"""{"x-defs": {"a": {"type": "string"}}, "$ref": "#/x-defs/a"}""",
                valid = listOf(STRING_A),
                invalid = listOf("1"),
            )
        }
    }
})
