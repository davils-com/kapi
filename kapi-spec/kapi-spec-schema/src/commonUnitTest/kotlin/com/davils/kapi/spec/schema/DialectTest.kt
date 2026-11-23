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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

private val DRAFT_04: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.Draft04 }

private val DRAFT_06: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.Draft06 }

private val DRAFT_07: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.Draft07 }

private val DRAFT_2019: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.Draft201909 }

private val OPENAPI_30: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.OpenApi30 }

class DialectTest : FunSpec({
    context("draft-04") {
        test("exclusiveMaximum and exclusiveMinimum are booleans") {
            verify("""{"maximum": 3, "exclusiveMaximum": true}""", listOf("2.9"), listOf("3"), DRAFT_04)
            verify("""{"minimum": 3, "exclusiveMinimum": true}""", listOf("3.1"), listOf("3"), DRAFT_04)
            verify("""{"maximum": 3, "exclusiveMaximum": false}""", listOf("3"), listOf("4"), DRAFT_04)
            diagnosticsOf("""{"exclusiveMaximum": 3}""", DRAFT_04).isEmpty() shouldBe false
        }

        test("only a number without fraction and exponent is an integer") {
            verify("""{"type": "integer"}""", listOf("1"), listOf("1.0", "1e2"), DRAFT_04)
        }

        test("id names resources, a reference replaces its siblings and booleans are no schemas") {
            verify(
                """{"id": "http://example.com/root", "definitions": {"a": {"id": "#a", "type": "string"}},
                    "properties": {"x": {"${'$'}ref": "#a", "type": "integer"}}}""",
                listOf("""{"x": "s"}"""),
                listOf("""{"x": 1}"""),
                DRAFT_04,
            )
            codesOf("""{"not": true}""", DRAFT_04) shouldBe listOf(DiagnosticCodes.NOT_A_SCHEMA)
            verify("""{"additionalProperties": false}""", listOf("{}"), listOf("""{"a": 1}"""), DRAFT_04)
        }

        test("items as an array with additionalItems, and dependencies") {
            verify(
                """{"items": [{"type": "integer"}], "additionalItems": false}""",
                listOf("[1]"),
                listOf("[1, 2]", "[\"a\"]"),
                DRAFT_04,
            )
            verify(
                """{"items": {"type": "integer"}, "additionalItems": false}""",
                listOf("[1, 2]"),
                emptyList(),
                DRAFT_04,
            )
            verify(
                """{"dependencies": {"a": ["b"], "c": {"required": ["d"]}}}""",
                listOf("{}", """{"a": 1, "b": 1}""", """{"c": 1, "d": 1}"""),
                listOf("""{"a": 1}""", """{"c": 1}"""),
                DRAFT_04,
            )
        }

        test("newer keywords are unknown") {
            verify(
                """{"const": 1, "contains": false, "propertyNames": false, "if": false}""",
                listOf("2", "[1]", """{"a": 1}"""),
                emptyList(),
                DRAFT_04,
            )
        }
    }

    context("draft-06 and draft-07") {
        test("exclusiveMaximum is a number and const, contains and propertyNames exist") {
            verify("""{"exclusiveMaximum": 3, "const": 2}""", listOf("2"), listOf("3", "1"), DRAFT_06)
            verify("""{"contains": {"const": 1}}""", listOf("[1]"), listOf("[2]"), DRAFT_06)
            verify("""{"type": "integer"}""", listOf("1.0"), emptyList(), DRAFT_06)
        }

        test("if, then and else exist in draft-07 only") {
            verify("""{"if": true, "then": false}""", listOf("1"), emptyList(), DRAFT_06)
            verify("""{"if": true, "then": false}""", emptyList(), listOf("1"), DRAFT_07)
        }

        test("a reference replaces its siblings, and the keywords of 2019-09 are unknown") {
            verify(
                """{"definitions": {"a": {"type": "string"}}, "${'$'}ref": "#/definitions/a", "maxLength": 1}""",
                listOf("\"long\""),
                listOf("1"),
                DRAFT_07,
            )
            verify(
                """{"dependentRequired": {"a": ["b"]}, "minContains": 5, "contains": true}""",
                listOf("""{"a": 1}""", "[1]"),
                emptyList(),
                DRAFT_07,
            )
        }
    }

    context("2019-09") {
        test("items as an array, a reference with siblings and contains that leaves items unevaluated") {
            verify("""{"items": [true], "additionalItems": false}""", listOf("[1]"), listOf("[1, 2]"), DRAFT_2019)
            verify(
                """{"${'$'}defs": {"a": {"type": "string"}}, "${'$'}ref": "#/${'$'}defs/a", "maxLength": 1}""",
                listOf("\"a\""),
                listOf("\"long\""),
                DRAFT_2019,
            )
            verify(
                """{"contains": {"type": "string"}, "unevaluatedItems": false}""",
                emptyList(),
                listOf("[\"a\"]"),
                DRAFT_2019,
            )
            verify("""{"contains": {"type": "string"}, "unevaluatedItems": false}""", listOf("[\"a\"]"), emptyList())
        }

        test("a recursive reference follows the outermost recursive anchor") {
            val tree = """{"${'$'}id": "https://example.com/tree", "${'$'}recursiveAnchor": true,
                "type": "object", "properties": {"children": {"type": "array",
                "items": {"${'$'}recursiveRef": "#"}}}}"""
            val strict = """{"${'$'}id": "https://example.com/strict", "${'$'}recursiveAnchor": true,
                "${'$'}ref": "tree", "unevaluatedProperties": false}"""
            val register: CompilerBuilder.() -> Unit = {
                defaultDialect = Dialect.Draft201909
                schema(DocumentUri("https://example.com/tree"), json(tree))
            }
            verify(
                strict,
                listOf("""{"children": [{"children": []}]}"""),
                listOf("""{"children": [{"child": []}]}"""),
                register,
            )
            verify(tree, listOf("""{"children": [{"child": []}]}"""), emptyList(), DRAFT_2019)
        }
    }

    context("2020-12") {
        test("items is a single schema") {
            codesOf("""{"items": [true]}""") shouldBe listOf(DiagnosticCodes.KEYWORD_INVALID)
        }

        test("the dialect follows the schema keyword over the default") {
            verify(
                """{"${'$'}schema": "http://json-schema.org/draft-04/schema#", "type": "integer"}""",
                emptyList(),
                listOf("1.0"),
            )
            verify(
                """{"${'$'}schema": "https://json-schema.org/draft/2020-12/schema", "type": "integer"}""",
                listOf("1.0"),
                emptyList(),
                DRAFT_04,
            )
        }

        test("an embedded resource may change the dialect") {
            verify(
                """{"${'$'}defs": {"old": {"${'$'}id": "https://example.com/old",
                    "${'$'}schema": "http://json-schema.org/draft-07/schema#",
                    "dependentRequired": {"a": ["b"]}}}, "${'$'}ref": "https://example.com/old"}""",
                listOf("""{"a": 1}"""),
                emptyList(),
            )
        }
    }

    context("vocabularies") {
        test("a custom meta-schema without the validation vocabulary turns off validation keywords") {
            val meta = """{"${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
                "${'$'}id": "https://example.com/meta",
                "${'$'}vocabulary": {"https://json-schema.org/draft/2020-12/vocab/core": true,
                "https://json-schema.org/draft/2020-12/vocab/applicator": true,
                "https://example.com/vocab/optional": false}}"""
            verify(
                """{"${'$'}schema": "https://example.com/meta", "properties": {"a": {"minimum": 10}},
                    "additionalProperties": false}""",
                listOf("""{"a": 1}"""),
                listOf("""{"b": 1}"""),
            ) { schema(DocumentUri("https://example.com/meta"), json(meta)) }
        }

        test("a meta-schema that requires an unknown vocabulary is refused") {
            val meta = """{"${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
                "${'$'}vocabulary": {"https://example.com/vocab/required": true}}"""
            val diagnostics = diagnosticsOf("""{"${'$'}schema": "https://example.com/meta"}""") {
                schema(DocumentUri("https://example.com/meta"), json(meta))
            }
            diagnostics.map { it.code } shouldContain DiagnosticCodes.VOCABULARY_UNKNOWN
        }

        test("the format-assertion vocabulary asserts formats") {
            val meta = """{"${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
                "${'$'}vocabulary": {"https://json-schema.org/draft/2020-12/vocab/core": true,
                "https://json-schema.org/draft/2020-12/vocab/format-assertion": true}}"""
            val schema = """{"${'$'}schema": "https://example.com/meta", "format": "ipv4"}"""
            verify(schema, listOf("\"1.2.3.4\""), listOf("\"1.2.3\"")) {
                schema(DocumentUri("https://example.com/meta"), json(meta))
            }
        }
    }

    context("meta-schemas") {
        test("every bundled meta-schema validates schemas offline") {
            val metas = listOf(
                "http://json-schema.org/draft-04/schema#",
                "http://json-schema.org/draft-06/schema#",
                "http://json-schema.org/draft-07/schema#",
                "https://json-schema.org/draft/2019-09/schema",
                "https://json-schema.org/draft/2020-12/schema",
                "https://spec.openapis.org/oas/3.1/dialect/base",
            )
            metas.forEach { meta ->
                verify(
                    """{"${'$'}ref": "$meta"}""",
                    listOf("""{"type": "string", "properties": {"a": {"minimum": 1}}}""", "{}"),
                    listOf("""{"type": 5}""", """{"properties": {"a": {"minLength": -1}}}"""),
                )
            }
        }

        test("the OpenAPI 3.1 dialect checks its own keywords") {
            verify(
                """{"${'$'}ref": "https://spec.openapis.org/oas/3.1/dialect/base"}""",
                listOf("""{"discriminator": {"propertyName": "kind"}}"""),
                listOf("""{"discriminator": {"mapping": {}}}""", """{"xml": {"unknown": 1}}"""),
            )
        }
    }

    context("OpenAPI") {
        test("3.0 has nullable, a single type, boolean exclusive bounds and references that replace siblings") {
            verify("""{"type": "string", "nullable": true}""", listOf("null", "\"a\""), listOf("1"), OPENAPI_30)
            verify("""{"type": "string"}""", listOf("\"a\""), listOf("null"), OPENAPI_30)
            verify("""{"maximum": 1, "exclusiveMaximum": true}""", listOf("0"), listOf("1"), OPENAPI_30)
            codesOf("""{"type": ["string", "null"]}""", OPENAPI_30) shouldBe listOf(DiagnosticCodes.KEYWORD_INVALID)
            codesOf("""{"nullable": "yes"}""", OPENAPI_30) shouldBe listOf(DiagnosticCodes.KEYWORD_INVALID)
            verify(
                """{"properties": {"a": {"${'$'}ref": "#/components/a", "maxLength": 1}},
                    "components": {"a": {"type": "string"}}}""",
                listOf("""{"a": "long"}"""),
                listOf("""{"a": 1}"""),
                OPENAPI_30,
            )
            verify(
                """{"discriminator": {"propertyName": "kind"}, "example": 1, "xml": {}, "externalDocs": {}}""",
                listOf("1"),
                emptyList(),
                OPENAPI_30,
            )
        }

        test("3.1 is 2020-12 with annotations of its own") {
            verify(
                """{"${'$'}schema": "https://spec.openapis.org/oas/3.1/dialect/base", "type": ["string",
                    "null"], "discriminator": {"propertyName": "kind"}, "example": 1}""",
                listOf("null", "\"a\""),
                listOf("1"),
            )
            verify("""{"type": "string"}""", listOf("\"a\""), listOf("1")) { defaultDialect = Dialect.OpenApi31 }
        }
    }
})
