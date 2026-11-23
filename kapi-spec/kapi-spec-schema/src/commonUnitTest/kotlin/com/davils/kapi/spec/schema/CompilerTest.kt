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
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private val REMOTE: DocumentUri = DocumentUri("https://example.com/schemas/name.json")

class CompilerTest : FunSpec({
    test("a compiler has defaults that a block changes") {
        val defaults = JsonSchema.compiler()
        defaults.defaultDialect shouldBe Dialect.Draft202012
        defaults.formatAssertion shouldBe false
        defaults.limits shouldBe SchemaLimits.Default
        val configured = JsonSchema.compiler {
            defaultDialect = Dialect.Draft07
            formatAssertion = true
            limits { maximumErrors = 5 }
        }
        configured.defaultDialect shouldBe Dialect.Draft07
        configured.limits.maximumErrors shouldBe 5
        configured.toString() shouldContain "Draft07"
    }

    test("a compiler refuses a URI registered twice") {
        shouldThrow<ValidationException> {
            JsonSchema.compiler {
                schema(REMOTE, json("{}"))
                schema(REMOTE, json("{}"))
            }
        }
    }

    test("a reference resolves to a registered document") {
        val compiled = schemaOf("""{"${'$'}ref": "https://example.com/schemas/name.json"}""") {
            schema(REMOTE, json("""{"type": "string"}"""))
        }
        compiled.accepts("\"pet\"") shouldBe true
        compiled.accepts("1") shouldBe false
    }

    test("a reference resolves through the loader, which is asked once per document") {
        val asked = ArrayList<String>()
        val compiled = schemaOf("""{"anyOf": [{"${'$'}ref": "https://example.com/a.json"},
            {"${'$'}ref": "https://example.com/a.json#/${'$'}defs/b"}]}""") {
            loader = SchemaLoader { uri ->
                asked += uri.value
                json("""{"type": "integer", "${'$'}defs": {"b": {"type": "string"}}}""")
            }
        }
        compiled.accepts("1") shouldBe true
        compiled.accepts("\"b\"") shouldBe true
        compiled.accepts("true") shouldBe false
        asked shouldBe listOf("https://example.com/a.json")
    }

    test("an unknown remote reference is a located compile error") {
        val diagnostics = diagnosticsOf("""{"properties": {"a": {"${'$'}ref": "https://example.com/missing.json"}}}""")
        diagnostics.map { it.code } shouldContain DiagnosticCodes.REFERENCE_UNRESOLVED
        diagnostics.first().location?.pointer?.text shouldBe "/properties/a/\$ref"
    }

    test("a loader that returns nothing leaves the reference unresolved") {
        val diagnostics = diagnosticsOf("""{"${'$'}ref": "https://example.com/missing.json"}""") {
            loader = SchemaLoader { null }
        }
        diagnostics.map { it.code } shouldBe listOf(DiagnosticCodes.REFERENCE_UNRESOLVED)
    }

    test("a failing loader is contained and reported") {
        val diagnostics = diagnosticsOf("""{"${'$'}ref": "https://example.com/broken.json"}""") {
            loader = SchemaLoader { throw IllegalStateException("broken") }
        }
        diagnostics.map { it.code } shouldContain DiagnosticCodes.LOADER_FAILED
    }

    test("a keyword of the wrong shape is reported at its pointer") {
        val diagnostics = diagnosticsOf("""{"properties": {"a": {"minLength": -1, "type": 5}}}""")
        diagnostics.map { it.location?.pointer?.text } shouldBe listOf("/properties/a/minLength", "/properties/a/type")
        diagnostics.map { it.code }.toSet() shouldBe setOf(DiagnosticCodes.KEYWORD_INVALID)
    }

    test("every malformed keyword is reported") {
        val keywords = listOf(
            """"allOf": []""", """"anyOf": {}""", """"oneOf": 1""", """"properties": []""",
            """"patternProperties": 1""", """"dependentSchemas": 1""", """"dependentRequired": 1""",
            """"dependentRequired": {"a": [1]}""", """"required": [1]""", """"enum": 1""", """"multipleOf": 0""",
            """"multipleOf": "1"""", """"maximum": "1"""", """"minimum": null""", """"exclusiveMaximum": true""",
            """"maxItems": 1.5""", """"minContains": -1""", """"pattern": 1""", """"uniqueItems": 1""",
            """"format": 1""", """"items": [{}]""", """"prefixItems": []""", """"type": ["string", "text"]""",
            """"${'$'}ref": 1""", """"not": 1""",
        )
        keywords.forEach { keyword -> diagnosticsOf("{$keyword}").isEmpty() shouldBe false }
    }

    test("an invalid pattern is refused") {
        diagnosticsOf("""{"pattern": "("}""").map { it.code } shouldBe listOf(DiagnosticCodes.PATTERN_REFUSED)
    }

    test("identifiers and anchors are checked") {
        codesOf("""{"${'$'}anchor": "1a"}""") shouldBe listOf(DiagnosticCodes.ANCHOR_INVALID)
        codesOf("""{"${'$'}id": "https://example.com/a#frag"}""") shouldContain
            DiagnosticCodes.ID_INVALID
        val duplicate = """{"${'$'}defs": {"a": {"${'$'}id": "https://example.com/x"},
            "b": {"${'$'}id": "https://example.com/x"}}}"""
        codesOf(duplicate) shouldContain DiagnosticCodes.ID_DUPLICATE
        val anchors = """{"${'$'}defs": {"a": {"${'$'}anchor": "x"}, "b": {"${'$'}anchor": "x"}}}"""
        codesOf(anchors) shouldContain DiagnosticCodes.ID_DUPLICATE
    }

    test("an unknown meta-schema is reported") {
        val diagnostics = diagnosticsOf("""{"${'$'}schema": "https://example.com/meta"}""")
        diagnostics.map { it.code } shouldBe listOf(DiagnosticCodes.DIALECT_UNKNOWN)
        diagnostics.first().location?.pointer?.text shouldBe "/\$schema"
    }

    test("a schema compiles from YAML") {
        val compiled = JsonSchema.compiler().compile(yamlOf("type: object\nrequired: [id]\n"))
        val schema = compiled.shouldBeInstanceOf<Compilation.Compiled>().schema
        schema.accepts("""{"id": 1}""") shouldBe true
        schema.accepts("{}") shouldBe false
        schema.toString() shouldContain "subschemas"
    }

    test("a YAML schema that JSON cannot express is reported") {
        val compiled = JsonSchema.compiler().compile(yamlOf("maximum: .inf\n"))
        compiled.shouldBeInstanceOf<Compilation.Invalid>().diagnostics.map { it.code } shouldBe
            listOf(DiagnosticCodes.NOT_JSON)
        compiled.toString() shouldContain "Invalid"
    }

    test("the base URI is the id or the URI the schema was compiled with") {
        schemaOf("""{"${'$'}id": "https://example.com/root.json"}""").uri shouldBe "https://example.com/root.json"
        val compiled = JsonSchema.compiler().compile(json("{}"), DocumentUri("https://example.com/given.json"))
        compiled.shouldBeInstanceOf<Compilation.Compiled>().schema.uri shouldBe "https://example.com/given.json"
        schemaOf("{}").uri shouldBe "memory:/document"
    }

    test("a relative reference resolves against the URI the schema was compiled with") {
        val compiler = JsonSchema.compiler { schema(REMOTE, json("""{"type": "string"}""")) }
        val root = DocumentUri("https://example.com/schemas/root.json")
        val compiled = compiler.compile(json("""{"${'$'}ref": "name.json"}"""), root)
        compiled.shouldBeInstanceOf<Compilation.Compiled>().schema.accepts("\"x\"") shouldBe true
    }
})
