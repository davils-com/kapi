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

class KeywordTest : FunSpec({
    context("validation") {
        test("type") {
            verify("""{"type": "integer"}""", listOf("1", "1.0", "1e2", "-0"), listOf("1.5", "\"1\"", "null"))
            verify("""{"type": ["string", "null"]}""", listOf("\"a\"", "null"), listOf("1", "[]", "{}", "true"))
            verify("""{"type": "number"}""", listOf("1", "1.5"), listOf("true"))
            verify("""{"type": "boolean"}""", listOf("false"), listOf("0"))
            verify("""{"type": "array"}""", listOf("[]"), listOf("{}"))
            verify("""{"type": "object"}""", listOf("{}"), listOf("[]"))
        }

        test("const compares by JSON equality") {
            verify(
                """{"const": {"a": [1, 2.0], "b": null}}""",
                listOf("""{"b": null, "a": [1.0, 2]}"""),
                listOf("""{"a": [2, 1], "b": null}""", """{"a": [1, 2]}""", """{"a": [1, 2], "b": null, "c": 1}"""),
            )
            verify("""{"const": false}""", listOf("false"), listOf("0", "null"))
        }

        test("enum") {
            verify(
                """{"enum": [1, "a", [true], {"x": 1}]}""",
                listOf("1.0", "\"a\"", "[true]", """{"x": 1}"""),
                listOf("2", "[1]"),
            )
        }

        test("multipleOf is exact") {
            verify("""{"multipleOf": 0.01}""", listOf("0.07", "19.99", "0", "-0.5", "\"x\""), listOf("0.075", "1e-3"))
            verify("""{"multipleOf": 0.5}""", listOf("1e308", "1e4000", "2.5"), listOf("1.25"))
            verify("""{"multipleOf": 0.123456789}""", listOf("0.246913578"), listOf("1e308"))
            verify(
                """{"multipleOf": 123456789012345678901234567890}""",
                listOf("246913578024691357802469135780"),
                listOf("1e29"),
            )
            verify("""{"multipleOf": 7e-400}""", listOf("1.4e-399", "7"), listOf("1e-400"))
            verify("""{"multipleOf": 20}""", listOf("1e10", "40"), listOf("10", "5e-1"))
        }

        test("bounds compare exact decimals") {
            verify(
                """{"maximum": 18446744073709551615}""",
                listOf("18446744073709551600", "18446744073709551615"),
                listOf("18446744073709551616"),
            )
            verify("""{"exclusiveMaximum": 1.5}""", listOf("1.4999999999999999999999"), listOf("1.5", "2"))
            verify("""{"minimum": -1e-5}""", listOf("-0.00001", "0"), listOf("-0.000011"))
            verify("""{"exclusiveMinimum": 0}""", listOf("1e-400"), listOf("0", "-1", "-0.0"))
        }

        test("lengths count code points") {
            verify("""{"maxLength": 2}""", listOf("\"💩💩\"", "\"ab\"", "1"), listOf("\"abc\""))
            verify("""{"minLength": 2}""", listOf("\"ab\""), listOf("\"💩\""))
        }

        test("pattern is not anchored") {
            verify("""{"pattern": "b+"}""", listOf("\"abbc\"", "1"), listOf("\"ac\""))
        }

        test("item counts and uniqueness") {
            verify("""{"maxItems": 1}""", listOf("[1]", "{}"), listOf("[1, 2]"))
            verify("""{"minItems": 1}""", listOf("[1]"), listOf("[]"))
            verify(
                """{"uniqueItems": true}""",
                listOf("[1, 2]", "[[1], [true]]", "[{\"a\": 1}, {\"a\": 2}]", "[0, false]", "[\"1\", 1]"),
                listOf(
                    "[1, 1.0]",
                    "[{\"a\": 1, \"b\": 2}, {\"b\": 2, \"a\": 1}]",
                    "[[1, 2], [1, 2.0]]",
                    "[null, null]",
                ),
            )
            verify("""{"uniqueItems": false}""", listOf("[1, 1]"), emptyList())
        }

        test("contains with minContains and maxContains") {
            verify("""{"contains": {"type": "string"}}""", listOf("[1, \"a\"]", "{}"), listOf("[1]", "[]"))
            verify(
                """{"contains": {"type": "string"}, "minContains": 2, "maxContains": 3}""",
                listOf("[\"a\", \"b\"]", "[\"a\", 1, \"b\", \"c\"]"),
                listOf("[\"a\"]", "[\"a\", \"b\", \"c\", \"d\"]"),
            )
            verify("""{"contains": {"type": "string"}, "minContains": 0}""", listOf("[]", "[1]"), emptyList())
        }

        test("property counts, required and dependentRequired") {
            verify("""{"maxProperties": 1}""", listOf("{}", "[1, 2]"), listOf("""{"a": 1, "b": 2}"""))
            verify("""{"minProperties": 1}""", listOf("""{"a": 1}"""), listOf("{}"))
            verify("""{"required": ["a"]}""", listOf("""{"a": null}""", "[]"), listOf("""{"b": 1}"""))
            verify(
                """{"dependentRequired": {"a": ["b"]}}""",
                listOf("{}", """{"a": 1, "b": 2}""", """{"b": 1}"""),
                listOf("""{"a": 1}"""),
            )
        }

        test("format is an annotation unless it asserts") {
            verify("""{"format": "date"}""", listOf("\"not a date\"", "1"), emptyList())
            val asserting: CompilerBuilder.() -> Unit = { formatAssertion = true }
            verify(
                """{"format": "date"}""",
                listOf("\"2024-02-29\"", "1"),
                listOf("\"2023-02-29\"", "\"2024-13-01\""),
                asserting,
            )
            verify(
                """{"format": "date-time"}""",
                listOf("\"2024-01-01T23:59:60Z\"", "\"2024-01-01T10:00:00.5+01:00\""),
                listOf("\"2024-01-01T10:00:60Z\"", "\"2024-01-01 10:00:00Z\"", "\"2024-01-01T10:00:00.Z\""),
                asserting,
            )
            verify(
                """{"format": "ipv4"}""",
                listOf("\"192.168.0.1\""),
                listOf("\"256.1.1.1\"", "\"01.1.1.1\"", "\"1.1.1\""),
                asserting,
            )
            verify(
                """{"format": "uuid"}""",
                listOf("\"2EB8AA08-AA98-11EA-B4AA-73B441D16380\""),
                listOf("\"2eb8aa08aa98-11ea-b4aa-73b441d16380\""),
                asserting,
            )
            verify(
                """{"format": "email"}""",
                listOf("\"joe.bloggs@example.com\"", "\"a@[127.0.0.1]\""),
                listOf("\"joe..bloggs@example.com\"", "\"@example.com\"", "\"a@-b.com\""),
                asserting,
            )
            verify("""{"format": "unknown-format"}""", listOf("\"anything\""), emptyList(), asserting)
        }

        test("content and meta-data keywords are annotations") {
            verify(
                """{"contentMediaType": "application/json", "contentEncoding": "base64", "title": "t",
                    "deprecated": true, "default": 1, "examples": [2], "x-unknown": {"type": "string"}}""",
                listOf("\"{not json\"", "1"),
                emptyList(),
            )
        }
    }

    context("applicators") {
        test("properties, patternProperties and additionalProperties") {
            verify(
                """{"properties": {"a": {"type": "integer"}}, "patternProperties": {"^x-": {"type": "string"}},
                    "additionalProperties": false}""",
                listOf("""{"a": 1, "x-b": "c"}""", "{}", "[1]"),
                listOf("""{"a": "1"}""", """{"x-b": 1}""", """{"b": 1}"""),
            )
        }

        test("propertyNames") {
            verify("""{"propertyNames": {"maxLength": 2}}""", listOf("""{"ab": 1}""", "[]"), listOf("""{"abc": 1}"""))
        }

        test("dependentSchemas") {
            verify(
                """{"dependentSchemas": {"a": {"required": ["b"]}}}""",
                listOf("{}", """{"a": 1, "b": 1}"""),
                listOf("""{"a": 1}"""),
            )
        }

        test("prefixItems and items") {
            verify(
                """{"prefixItems": [{"type": "integer"}, {"type": "string"}], "items": {"type": "boolean"}}""",
                listOf("[1, \"a\", true]", "[1]", "[]", "{}"),
                listOf("[\"a\"]", "[1, \"a\", 2]"),
            )
            verify("""{"items": false}""", listOf("[]"), listOf("[1]"))
        }

        test("allOf, anyOf, oneOf and not") {
            verify("""{"allOf": [{"minimum": 1}, {"maximum": 2}]}""", listOf("1.5"), listOf("3", "0"))
            verify("""{"anyOf": [{"type": "string"}, {"minimum": 2}]}""", listOf("\"a\"", "3"), listOf("1"))
            verify("""{"oneOf": [{"type": "integer"}, {"minimum": 2}]}""", listOf("1", "2.5"), listOf("3", "1.5"))
            verify("""{"not": {"type": "string"}}""", listOf("1"), listOf("\"a\""))
        }

        test("if, then and else") {
            verify(
                """{"if": {"type": "integer"}, "then": {"minimum": 0}, "else": {"type": "string"}}""",
                listOf("1", "\"a\""),
                listOf("-1", "1.5"),
            )
            verify("""{"then": {"type": "string"}, "else": {"type": "string"}}""", listOf("1"), emptyList())
            verify("""{"if": {"type": "integer"}, "then": {"minimum": 0}}""", listOf("1", "-1.5"), listOf("-1"))
        }

        test("unevaluatedProperties sees what adjacent and in-place keywords evaluated") {
            verify(
                """{"properties": {"a": true}, "allOf": [{"properties": {"b": true}}],
                    "anyOf": [{"properties": {"c": true}}, {"properties": {"d": true}}],
                    "if": {"properties": {"e": {"const": 1}}}, "then": {"properties": {"f": true}},
                    "unevaluatedProperties": false}""",
                listOf("""{"a": 1, "b": 1, "c": 1, "d": 1}""", """{"e": 1, "f": 1}"""),
                listOf("""{"g": 1}""", """{"e": 2, "f": 1}"""),
            )
            verify(
                """{"anyOf": [{"properties": {"a": {"type": "string"}}}, true], "unevaluatedProperties": false}""",
                listOf("""{"a": "x"}"""),
                listOf("""{"a": 1}"""),
            )
            verify(
                """{"not": {"not": {"properties": {"a": true}}}, "unevaluatedProperties": false}""",
                listOf("{}"),
                listOf("""{"a": 1}"""),
            )
            verify(
                """{"properties": {"a": {"unevaluatedProperties": false, "properties": {"b": true}}},
                    "unevaluatedProperties": {"type": "string"}}""",
                listOf("""{"a": {"b": 1}, "c": "x"}"""),
                listOf("""{"a": {"b": 1, "c": 1}}""", """{"c": 1}"""),
            )
        }

        test("unevaluatedItems sees prefixItems, items, contains and references") {
            verify(
                $$"""{"prefixItems": [true], "contains": {"type": "string"},
                    "unevaluatedItems": {"type": "integer"}}""",
                listOf("[null, \"a\", 1]", "[null, \"a\"]"),
                listOf("[null, \"a\", true]"),
            )
            verify(
                $$"""{"$ref": "#/$defs/pair", "unevaluatedItems": false,
                    "$defs": {"pair": {"prefixItems": [true, true]}}}""",
                listOf("[1, 2]"),
                listOf("[1, 2, 3]"),
            )
        }
    }

    context("core") {
        test("references to definitions, anchors and embedded resources") {
            verify(
                $$"""{"$id": "https://example.com/root",
                    "properties": {"a": {"$ref": "#/$defs/a"}, "b": {"$ref": "#b"}, "c": {"$ref": "c.json"}},
                    "$defs": {"a": {"type": "string"}, "b": {"$anchor": "b", "type": "integer"},
                    "c": {"$id": "c.json", "maximum": 3}}}""",
                listOf("""{"a": "x", "b": 1, "c": 3}"""),
                listOf("""{"a": 1}""", """{"b": "x"}""", """{"c": 4}"""),
            )
        }

        test("escaped JSON Pointers resolve") {
            verify(
                $$"""{"$defs": {"a/b": {"type": "string"}, "c~d": {"type": "integer"}, "e%f": {"type": "null"}},
                    "allOf": [{"$ref": "#/$defs/a~1b"}], "properties": {"x": {"$ref": "#/$defs/c~0d"},
                    "y": {"$ref": "#/$defs/e%25f"}}}""",
                listOf("\"s\"", "\"t\""),
                listOf("1"),
            )
        }

        test("boolean schemas") {
            verify("true", listOf("1", "null"), emptyList())
            verify("false", emptyList(), listOf("1", "null"))
        }

        test("a dynamic reference resolves to the outermost dynamic anchor in scope") {
            val base = $$"""{"$id": "https://example.com/list", "type": "array",
                "items": {"$dynamicRef": "#item"}, "$defs": {"item": {"$dynamicAnchor": "item"}}}"""
            val strings = $$"""{"$id": "https://example.com/strings", "$ref": "list",
                "$defs": {"item": {"$dynamicAnchor": "item", "type": "string"}}}"""
            verify(strings, listOf("[\"a\"]"), listOf("[1]")) {
                schema(DocumentUri("https://example.com/list"), json(base))
            }
            verify(base, listOf("[1]"), listOf("1"))
        }

        test("a reference into an unknown keyword compiles that place as a schema") {
            verify($$"""{"x-defs": {"a": {"type": "string"}}, "$ref": "#/x-defs/a"}""", listOf("\"a\""), listOf("1"))
        }
    }
})
