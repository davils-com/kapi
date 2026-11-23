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

private const val OBJECT_A: String = """{"a": 1}"""

private const val PAIR: String = "[1, 2]"

private const val ARRAY_A: String = """["a"]"""

private const val STRING_A: String = "\"a\""

private const val META: String = "https://example.com/meta"

private val META_HEAD: String = """{"${'$'}schema": "https://json-schema.org/draft/2020-12/schema", """

private val DRAFT_06: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.Draft06 }

private val DRAFT_07: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.Draft07 }

private val DRAFT_2019: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.Draft201909 }

private val OPENAPI_30: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.OpenApi30 }

class DialectTest : FunSpec({
    context("draft-04") {
        test("exclusiveMaximum and exclusiveMinimum are booleans") {
            verify(
                schema = """{"maximum": 3, "exclusiveMaximum": true}""",
                valid = listOf("2.9"),
                invalid = listOf("3"),
                block = DRAFT_04,
            )
            verify(
                schema = """{"minimum": 3, "exclusiveMinimum": true}""",
                valid = listOf("3.1"),
                invalid = listOf("3"),
                block = DRAFT_04,
            )
            verify(
                schema = """{"maximum": 3, "exclusiveMaximum": false}""",
                valid = listOf("3"),
                invalid = listOf("4"),
                block = DRAFT_04,
            )
            diagnosticsOf("""{"exclusiveMaximum": 3}""", DRAFT_04).isEmpty() shouldBe false
        }

        test("only a number without fraction and exponent is an integer") {
            verify(
                schema = """{"type": "integer"}""",
                valid = listOf("1"),
                invalid = listOf("1.0", "1e2"),
                block = DRAFT_04,
            )
        }

        test("id names resources, a reference replaces its siblings and booleans are no schemas") {
            verify(
                schema =
                """{"id": "http://example.com/root", "definitions": {"a": {"id": "#a", "type": "string"}}, """ +
                    """"properties": {"x": {"${'$'}ref": "#a", "type": "integer"}}}""",
                valid = listOf("""{"x": "s"}"""),
                invalid = listOf("""{"x": 1}"""),
                block = DRAFT_04,
            )
            codesOf("""{"not": true}""", DRAFT_04) shouldBe listOf(DiagnosticCodes.NOT_A_SCHEMA)
            verify(
                schema = """{"additionalProperties": false}""",
                valid = listOf("{}"),
                invalid = listOf(OBJECT_A),
                block = DRAFT_04,
            )
        }

        test("items as an array with additionalItems, and dependencies") {
            verify(
                schema = """{"items": [{"type": "integer"}], "additionalItems": false}""",
                valid = listOf("[1]"),
                invalid = listOf(PAIR, ARRAY_A),
                block = DRAFT_04,
            )
            verify(
                schema = """{"items": {"type": "integer"}, "additionalItems": false}""",
                valid = listOf(PAIR),
                invalid = emptyList(),
                block = DRAFT_04,
            )
            verify(
                schema = """{"dependencies": {"a": ["b"], "c": {"required": ["d"]}}}""",
                valid = listOf("{}", """{"a": 1, "b": 1}""", """{"c": 1, "d": 1}"""),
                invalid = listOf(OBJECT_A, """{"c": 1}"""),
                block = DRAFT_04,
            )
        }

        test("newer keywords are unknown") {
            verify(
                schema = """{"const": 1, "contains": false, "propertyNames": false, "if": false}""",
                valid = listOf("2", "[1]", OBJECT_A),
                invalid = emptyList(),
                block = DRAFT_04,
            )
        }
    }

    context("draft-06 and draft-07") {
        test("exclusiveMaximum is a number and const, contains and propertyNames exist") {
            verify(
                schema = """{"exclusiveMaximum": 3, "const": 2}""",
                valid = listOf("2"),
                invalid = listOf("3", "1"),
                block = DRAFT_06,
            )
            verify(
                schema = """{"contains": {"const": 1}}""",
                valid = listOf("[1]"),
                invalid = listOf("[2]"),
                block = DRAFT_06,
            )
            verify(
                schema = """{"type": "integer"}""",
                valid = listOf("1.0"),
                invalid = emptyList(),
                block = DRAFT_06,
            )
        }

        test("if, then and else exist in draft-07 only") {
            verify(
                schema = """{"if": true, "then": false}""",
                valid = listOf("1"),
                invalid = emptyList(),
                block = DRAFT_06,
            )
            verify(
                schema = """{"if": true, "then": false}""",
                valid = emptyList(),
                invalid = listOf("1"),
                block = DRAFT_07,
            )
        }

        test("a reference replaces its siblings, and the keywords of 2019-09 are unknown") {
            verify(
                schema = """{"definitions": {"a": {"type": "string"}}, """ +
                    """"${'$'}ref": "#/definitions/a", "maxLength": 1}""",
                valid = listOf("\"long\""),
                invalid = listOf("1"),
                block = DRAFT_07,
            )
            verify(
                schema = """{"dependentRequired": {"a": ["b"]}, "minContains": 5, "contains": true}""",
                valid = listOf(OBJECT_A, "[1]"),
                invalid = emptyList(),
                block = DRAFT_07,
            )
        }
    }

    context("2019-09") {
        test("items as an array, a reference with siblings and contains that leaves items unevaluated") {
            verify(
                schema = """{"items": [true], "additionalItems": false}""",
                valid = listOf("[1]"),
                invalid = listOf(PAIR),
                block = DRAFT_2019,
            )
            verify(
                schema = """{"${'$'}defs": {"a": {"type": "string"}}, "${'$'}ref": "#/${'$'}defs/a", "maxLength": 1}""",
                valid = listOf(STRING_A),
                invalid = listOf("\"long\""),
                block = DRAFT_2019,
            )
            verify(
                schema = """{"contains": {"type": "string"}, "unevaluatedItems": false}""",
                valid = emptyList(),
                invalid = listOf(ARRAY_A),
                block = DRAFT_2019,
            )
            verify(
                schema = """{"contains": {"type": "string"}, "unevaluatedItems": false}""",
                valid = listOf(ARRAY_A),
                invalid = emptyList(),
            )
        }

        test("a recursive reference follows the outermost recursive anchor") {
            val tree = """{"${'$'}id": "https://example.com/tree", "${'$'}recursiveAnchor": true, """ +
                """"type": "object", "properties": {"children": {"type": "array", """ +
                """"items": {"${'$'}recursiveRef": "#"}}}}"""
            val strict = """{"${'$'}id": "https://example.com/strict", "${'$'}recursiveAnchor": true, """ +
                """"${'$'}ref": "tree", "unevaluatedProperties": false}"""
            val register: CompilerBuilder.() -> Unit = {
                defaultDialect = Dialect.Draft201909
                schema(DocumentUri("https://example.com/tree"), json(tree))
            }
            verify(
                schema = strict,
                valid = listOf("""{"children": [{"children": []}]}"""),
                invalid = listOf("""{"children": [{"child": []}]}"""),
                block = register,
            )
            verify(
                schema = tree,
                valid = listOf("""{"children": [{"child": []}]}"""),
                invalid = emptyList(),
                block = DRAFT_2019,
            )
        }
    }

    context("2020-12") {
        test("items is a single schema") {
            codesOf("""{"items": [true]}""") shouldBe listOf(DiagnosticCodes.KEYWORD_INVALID)
        }

        test("the dialect follows the schema keyword over the default") {
            verify(
                schema = """{"${'$'}schema": "http://json-schema.org/draft-04/schema#", "type": "integer"}""",
                valid = emptyList(),
                invalid = listOf("1.0"),
            )
            verify(
                schema = """{"${'$'}schema": "https://json-schema.org/draft/2020-12/schema", "type": "integer"}""",
                valid = listOf("1.0"),
                invalid = emptyList(),
                block = DRAFT_04,
            )
        }

        test("an embedded resource may change the dialect") {
            verify(
                schema = """{"${'$'}defs": {"old": {"${'$'}id": "https://example.com/old", """ +
                    """"${'$'}schema": "http://json-schema.org/draft-07/schema#", """ +
                    """"dependentRequired": {"a": ["b"]}}}, "${'$'}ref": "https://example.com/old"}""",
                valid = listOf(OBJECT_A),
                invalid = emptyList(),
            )
        }
    }

    context("vocabularies") {
        test("a custom meta-schema without the validation vocabulary turns off validation keywords") {
            val meta = META_HEAD +
                """"${'$'}id": "https://example.com/meta", """ +
                """"${'$'}vocabulary": {"https://json-schema.org/draft/2020-12/vocab/core": true, """ +
                """"https://json-schema.org/draft/2020-12/vocab/applicator": true, """ +
                """"https://example.com/vocab/optional": false}}"""
            verify(
                schema = """{"${'$'}schema": "https://example.com/meta", "properties": {"a": {"minimum": 10}}, """ +
                    """"additionalProperties": false}""",
                valid = listOf(OBJECT_A),
                invalid = listOf("""{"b": 1}"""),
            ) { schema(DocumentUri(META), json(meta)) }
        }

        test("a meta-schema that requires an unknown vocabulary is refused") {
            val meta = META_HEAD +
                """"${'$'}vocabulary": {"https://example.com/vocab/required": true}}"""
            val diagnostics = diagnosticsOf("""{"${'$'}schema": "https://example.com/meta"}""") {
                schema(DocumentUri(META), json(meta))
            }
            diagnostics.map { it.code } shouldContain DiagnosticCodes.VOCABULARY_UNKNOWN
        }

        test("the format-assertion vocabulary asserts formats") {
            val meta = META_HEAD +
                """"${'$'}vocabulary": {"https://json-schema.org/draft/2020-12/vocab/core": true, """ +
                """"https://json-schema.org/draft/2020-12/vocab/format-assertion": true}}"""
            val schema = """{"${'$'}schema": "https://example.com/meta", "format": "ipv4"}"""
            verify(schema = schema, valid = listOf("\"1.2.3.4\""), invalid = listOf("\"1.2.3\"")) {
                schema(DocumentUri(META), json(meta))
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
                    schema = """{"${'$'}ref": "$meta"}""",
                    valid = listOf("""{"type": "string", "properties": {"a": {"minimum": 1}}}""", "{}"),
                    invalid = listOf("""{"type": 5}""", """{"properties": {"a": {"minLength": -1}}}"""),
                )
            }
        }

        test("the OpenAPI 3.1 dialect checks its own keywords") {
            verify(
                schema = """{"${'$'}ref": "https://spec.openapis.org/oas/3.1/dialect/base"}""",
                valid = listOf("""{"discriminator": {"propertyName": "kind"}}"""),
                invalid = listOf("""{"discriminator": {"mapping": {}}}""", """{"xml": {"unknown": 1}}"""),
            )
        }
    }

    context("OpenAPI") {
        test("3.0 has nullable, a single type, boolean exclusive bounds and references that replace siblings") {
            verify(
                schema = """{"type": "string", "nullable": true}""",
                valid = listOf("null", STRING_A),
                invalid = listOf("1"),
                block = OPENAPI_30,
            )
            verify(
                schema = """{"type": "string"}""",
                valid = listOf(STRING_A),
                invalid = listOf("null"),
                block = OPENAPI_30,
            )
            verify(
                schema = """{"maximum": 1, "exclusiveMaximum": true}""",
                valid = listOf("0"),
                invalid = listOf("1"),
                block = OPENAPI_30,
            )
            codesOf("""{"type": ["string", "null"]}""", OPENAPI_30) shouldBe listOf(DiagnosticCodes.KEYWORD_INVALID)
            codesOf("""{"nullable": "yes"}""", OPENAPI_30) shouldBe listOf(DiagnosticCodes.KEYWORD_INVALID)
            verify(
                schema = """{"properties": {"a": {"${'$'}ref": "#/components/a", "maxLength": 1}}, """ +
                    """"components": {"a": {"type": "string"}}}""",
                valid = listOf("""{"a": "long"}"""),
                invalid = listOf(OBJECT_A),
                block = OPENAPI_30,
            )
            verify(
                schema = """{"discriminator": {"propertyName": "kind"}, "example": 1, "xml": {}, "externalDocs": {}}""",
                valid = listOf("1"),
                invalid = emptyList(),
                block = OPENAPI_30,
            )
        }

        test("3.1 is 2020-12 with annotations of its own") {
            verify(
                schema =
                """{"${'$'}schema": "https://spec.openapis.org/oas/3.1/dialect/base", "type": ["string", "null"], """ +
                    """"discriminator": {"propertyName": "kind"}, "example": 1}""",
                valid = listOf("null", STRING_A),
                invalid = listOf("1"),
            )
            verify(
                schema = """{"type": "string"}""",
                valid = listOf(STRING_A),
                invalid = listOf("1"),
            ) { defaultDialect = Dialect.OpenApi31 }
        }
    }
})
