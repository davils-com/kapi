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
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

private const val META: String = "https://example.com/meta"

private const val STRING_A: String = "\"a\""

private const val SCHEMA_DEPTH: Int = 3

private const val ALTERNATIVE_ERRORS: Int = 4

private val DRAFT_07: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.Draft07 }

private val DRAFT_2019: CompilerBuilder.() -> Unit = { defaultDialect = Dialect.Draft201909 }

private val CUSTOM_META: String = """{"${'$'}schema": "https://json-schema.org/draft/2020-12/schema", """ +
    """"${'$'}id": "$META", "${'$'}vocabulary": {"https://json-schema.org/draft/2020-12/vocab/core": true, """ +
    """"https://json-schema.org/draft/2020-12/vocab/applicator": true}}"""

private val WITH_META: CompilerBuilder.() -> Unit = { schema(DocumentUri(META), json(CUSTOM_META)) }

class ReferenceTest : FunSpec({
    test("anchors follow the naming rules of their draft") {
        codesOf("""{"${'$'}anchor": ""}""") shouldBe listOf(DiagnosticCodes.ANCHOR_INVALID)
        codesOf("""{"${'$'}anchor": "a${'$'}b"}""") shouldBe listOf(DiagnosticCodes.ANCHOR_INVALID)
        codesOf("""{"${'$'}anchor": "a~"}""") shouldBe listOf(DiagnosticCodes.ANCHOR_INVALID)
        codesOf("""{"${'$'}anchor": "_a"}""", DRAFT_2019) shouldBe listOf(DiagnosticCodes.ANCHOR_INVALID)
        val modern = """{"${'$'}defs": {"a": {"${'$'}anchor": "_Z9-.x", "type": "string"}}, "${'$'}ref": "#_Z9-.x"}"""
        verify(schema = modern, valid = listOf(STRING_A), invalid = listOf("1"))
        val legacy = """{"definitions": {"a": {"${'$'}id": "#Ab:c", "type": "string"}}, "${'$'}ref": "#Ab:c"}"""
        verify(schema = legacy, valid = listOf(STRING_A), invalid = listOf("1"), block = DRAFT_07)
    }

    test("identifiers that are no URI references and misplaced keywords are reported or ignored") {
        codesOf("""{"${'$'}id": "a b"}""") shouldBe listOf(DiagnosticCodes.ID_INVALID)
        verify(
            schema = """{"${'$'}schema": 1, "${'$'}dynamicAnchor": 1, "${'$'}defs": {"a": {"${'$'}schema": "x"}}}""",
            valid = listOf("1"),
            invalid = emptyList(),
        )
        verify(
            schema = """{"${'$'}dynamicAnchor": "a", "${'$'}defs": {"b": {"${'$'}recursiveAnchor": true}}}""",
            valid = listOf("1"),
            invalid = emptyList(),
            block = DRAFT_2019,
        )
    }

    test("a root identifier equal to the retrieval URI names one resource") {
        val uri = DocumentUri("https://example.com/s")
        val compilation = JsonSchema.compiler().compile(json("""{"${'$'}id": "https://example.com/s"}"""), uri)
        (compilation as Compilation.Compiled).schema.uri shouldBe "https://example.com/s"
    }

    test("a dynamic or recursive reference without a dynamic scope falls back to its initial target") {
        val other = """{"${'$'}id": "https://example.com/other", "${'$'}dynamicAnchor": "n", "type": "string"}"""
        verify(
            schema = """{"${'$'}dynamicRef": "https://example.com/other#n"}""",
            valid = listOf(STRING_A),
            invalid = listOf("1"),
        ) { schema(DocumentUri("https://example.com/other"), json(other)) }
        val recursive = """{"${'$'}id": "https://example.com/other", "${'$'}recursiveAnchor": true, "type": "string"}"""
        verify(
            schema = """{"${'$'}recursiveRef": "https://example.com/other"}""",
            valid = listOf(STRING_A),
            invalid = listOf("1"),
        ) {
            defaultDialect = Dialect.Draft201909
            schema(DocumentUri("https://example.com/other"), json(recursive))
        }
        verify(
            schema = """{"${'$'}defs": {"a": {"type": "string"}}, "${'$'}dynamicRef": "#/${'$'}defs/a"}""",
            valid = listOf(STRING_A),
            invalid = listOf("1"),
        )
        verify(
            schema = """{"${'$'}defs": {"a": true}, "${'$'}recursiveRef": "#/${'$'}defs/a"}""",
            valid = listOf("1"),
            invalid = emptyList(),
            block = DRAFT_2019,
        )
    }

    test("a reference into a value that was not scanned as a schema compiles it on demand") {
        verify(
            schema = """{"${'$'}ref": "#/x/y", "x": {"y": {"${'$'}anchor": "1bad", "type": "string"}}}""",
            valid = listOf(STRING_A),
            invalid = listOf("1"),
        )
        val duplicate = """{"${'$'}defs": {"a": {"${'$'}id": "https://example.com/x", "${'$'}anchor": "z"}}, """ +
            """"${'$'}ref": "#/x/y", "x": {"y": {"${'$'}id": "https://example.com/x", "${'$'}anchor": "z"}}}"""
        schemaOf(duplicate).accepts("1") shouldBe true
        val unknown = """{"${'$'}ref": "#/x", "x": {"${'$'}id": "https://example.com/x", """ +
            """"${'$'}schema": "https://example.com/unknown", "type": "string"}}"""
        verify(schema = unknown, valid = listOf(STRING_A), invalid = listOf("1"))
        val deep = """{"${'$'}ref": "#/x", "x": {"not": {"not": {"not": {}}}}}"""
        codesOf(deep) { limits { maximumSchemaDepth = SCHEMA_DEPTH } } shouldBe listOf(DiagnosticCodes.LIMIT_EXCEEDED)
    }

    test("a pointer that leads nowhere leaves the reference unresolved") {
        val pointers = listOf("#/nope", "#/%zz", "#/allOf/x", "#/allOf/01", "#/type/0")
        pointers.forEach { pointer ->
            val schema = """{"allOf": [{}], "type": "string", "${'$'}ref": "$pointer"}"""
            withClue(pointer) { codesOf(schema) shouldBe listOf(DiagnosticCodes.REFERENCE_UNRESOLVED) }
        }
    }

    test("a dynamic reference is dynamic only to an anchor its target declares") {
        val targets = listOf(
            """{"${'$'}defs": {"a": {"${'$'}id": "https://example.com/a", "type": "string"}}, """ +
                """"${'$'}dynamicRef": "https://example.com/a"}""",
            """{"${'$'}defs": {"a": {"${'$'}dynamicAnchor": {}, "type": "string"}}, """ +
                """"${'$'}dynamicRef": "#/${'$'}defs/a"}""",
        )
        targets.forEach { schema -> verify(schema = schema, valid = listOf(STRING_A), invalid = listOf("1")) }
    }

    test("a reference cycle through several schemas is found") {
        val schema =
            """{"${'$'}defs": {"a": {"${'$'}ref": "#/${'$'}defs/b"}, "b": {"${'$'}ref": "#/${'$'}defs/c"}, """ +
                """"c": {"${'$'}ref": "#/${'$'}defs/b"}}, "${'$'}ref": "#/${'$'}defs/a"}"""
        schemaOf(schema).refusalOf(json("1")).reason shouldBe RefusalReason.ReferenceCycle
    }

    test("a custom meta-schema already loaded as a document is used as a dialect") {
        val schema = """{"allOf": [{"${'$'}ref": "$META"}, {"${'$'}ref": "https://example.com/b"}]}"""
        val b = """{"${'$'}schema": "$META", "minimum": 10}"""
        verify(schema = schema, valid = listOf("1"), invalid = emptyList()) {
            WITH_META()
            schema(DocumentUri("https://example.com/b"), json(b))
        }
    }

    test("a meta-schema found while references are pending discards what the first scan collected") {
        val schema =
            """{"${'$'}ref": "https://example.com/a", "${'$'}defs": {"b": {"${'$'}id": "https://example.com/b", """ +
                """"${'$'}schema": "$META", "minimum": 10}}}"""
        verify(schema = schema, valid = listOf("1"), invalid = emptyList()) {
            WITH_META()
            schema(DocumentUri("https://example.com/a"), json("{}"))
        }
    }

    test("a relative, circular or unloadable meta-schema is unknown") {
        codesOf("""{"${'$'}schema": "meta"}""") shouldBe listOf(DiagnosticCodes.DIALECT_UNKNOWN)
        val circular = """{"${'$'}schema": "$META", "${'$'}id": "$META"}"""
        codesOf("""{"${'$'}schema": "$META"}""") { schema(DocumentUri(META), json(circular)) } shouldContain
            DiagnosticCodes.DIALECT_UNKNOWN
    }

    test("every failing property name is reported, and flag output stops at the first") {
        val names = schemaOf("""{"propertyNames": {"maxLength": 1}}""")
        names.errorsOf("""{"ab": 1, "cd": 2}""") shouldHaveSize 2
        val alternatives = schemaOf("""{"anyOf": [{"type": "string"}], "oneOf": [{"type": "string"}]}""")
        alternatives.validate(json("1"), OutputFormat.Flag).isValid shouldBe false
        withClue("basic") { alternatives.errorsOf("1").size shouldBe ALTERNATIVE_ERRORS }
    }
})
