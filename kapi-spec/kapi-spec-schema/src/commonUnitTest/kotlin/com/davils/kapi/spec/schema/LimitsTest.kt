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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

private const val NESTED_LEVELS: Int = 5

private const val ALL_OF_SUBSCHEMAS: Int = 4

private const val DOCUMENTS: Int = 3

private const val STEPS_OF_THREE_ITEMS: Long = 4L

private const val REFERENCE_DEPTH: Int = 5

private const val INSTANCE_DEPTH: Int = 3

private const val ERRORS: Int = 3

private const val PATTERN_LENGTH: Int = 3

private const val LITERAL_NODES: Int = 3

private const val THREE_ITEMS: String = "[1, 2, 3]"

private const val MANY_ITEMS: Int = 1_000

private fun limited(block: SchemaLimitsBuilder.() -> Unit): CompilerBuilder.() -> Unit = { limits(block) }

private fun arrays(depth: Int): JsonElement {
    val leaf: JsonElement = JsonPrimitive(1)
    return (1..depth).fold(leaf) { inner, _ -> JsonArray(listOf(inner)) }
}

class LimitsTest : FunSpec({
    test("every limit lies between 1 and its ceiling") {
        val setters: List<SchemaLimitsBuilder.(Long) -> Unit> = listOf(
            { maximumSchemaDepth = it.toInt() },
            { maximumSubschemas = it.toInt() },
            { maximumDocuments = it.toInt() },
            { maximumEvaluationSteps = it },
            { maximumEvaluationDepth = it.toInt() },
            { maximumInstanceDepth = it.toInt() },
            { maximumInstanceNodes = it.toInt() },
            { maximumErrors = it.toInt() },
            { maximumStringLength = it.toInt() },
            { maximumPatternLength = it.toInt() },
            { maximumEnumValues = it.toInt() },
            { maximumLiteralNodes = it.toInt() },
        )
        val ceilings = listOf(
            SchemaLimits.CEILING_SCHEMA_DEPTH.toLong(),
            SchemaLimits.CEILING_SUBSCHEMAS.toLong(),
            SchemaLimits.CEILING_DOCUMENTS.toLong(),
            SchemaLimits.CEILING_EVALUATION_STEPS,
            SchemaLimits.CEILING_EVALUATION_DEPTH.toLong(),
            SchemaLimits.CEILING_INSTANCE_DEPTH.toLong(),
            SchemaLimits.CEILING_INSTANCE_NODES.toLong(),
            SchemaLimits.CEILING_ERRORS.toLong(),
            SchemaLimits.CEILING_STRING_LENGTH.toLong(),
            SchemaLimits.CEILING_PATTERN_LENGTH.toLong(),
            SchemaLimits.CEILING_ENUM_VALUES.toLong(),
            SchemaLimits.CEILING_LITERAL_NODES.toLong(),
        )
        setters.zip(ceilings).forEach { (set, ceiling) ->
            JsonSchema.compiler(limited { set(ceiling) }).limits.toString() shouldContain "SchemaLimits"
            shouldThrow<ValidationException> { JsonSchema.compiler(limited { set(ceiling + 1) }) }
            shouldThrow<ValidationException> { JsonSchema.compiler(limited { set(0) }) }
        }
        SchemaLimits.Default shouldBe SchemaLimitsBuilder().build()
        val built = SchemaLimitsBuilder().build()
        SchemaLimits.Default.hashCode() shouldBe built.hashCode()
    }

    test("the schema depth") {
        val schema = nested(open = """{"not": """, center = "{}", close = "}", depth = NESTED_LEVELS)
        schemaOf(schema, limited { maximumSchemaDepth = NESTED_LEVELS }).accepts("1") shouldBe false
        codesOf(
            schema,
            limited { maximumSchemaDepth = NESTED_LEVELS - 1 },
        ) shouldBe listOf(DiagnosticCodes.LIMIT_EXCEEDED)
    }

    test("the number of subschemas") {
        val schema = """{"allOf": [{}, {}, {}]}"""
        schemaOf(schema, limited { maximumSubschemas = ALL_OF_SUBSCHEMAS }).subschemas shouldBe ALL_OF_SUBSCHEMAS
        codesOf(
            schema,
            limited { maximumSubschemas = ALL_OF_SUBSCHEMAS - 1 },
        ) shouldBe listOf(DiagnosticCodes.LIMIT_EXCEEDED)
    }

    test("the number of documents") {
        val schema = """{"allOf": [{"${'$'}ref": "https://example.com/a"}, {"${'$'}ref": "https://example.com/b"}]}"""
        val documents: (Int) -> CompilerBuilder.() -> Unit = { maximum ->
            {
                schema(DocumentUri("https://example.com/a"), json("{}"))
                schema(DocumentUri("https://example.com/b"), json("{}"))
                limits { maximumDocuments = maximum }
            }
        }
        schemaOf(schema, documents(DOCUMENTS)).accepts("1") shouldBe true
        codesOf(schema, documents(DOCUMENTS - 1)) shouldContain DiagnosticCodes.LIMIT_EXCEEDED
    }

    test("the evaluation steps") {
        val schema = """{"items": {"type": "integer"}}"""
        schemaOf(schema, limited { maximumEvaluationSteps = STEPS_OF_THREE_ITEMS }).accepts(THREE_ITEMS) shouldBe true
        val strict = schemaOf(schema, limited { maximumEvaluationSteps = STEPS_OF_THREE_ITEMS - 1 })
        strict.refusalOf(json(THREE_ITEMS)).reason shouldBe
            RefusalReason.EvaluationSteps
    }

    test("the evaluation depth") {
        val schema = """{"items": {"${'$'}ref": "#"}}"""
        schemaOf(schema, limited { maximumEvaluationDepth = REFERENCE_DEPTH }).validate(arrays(2)).isValid shouldBe true
        val strict = schemaOf(schema, limited { maximumEvaluationDepth = REFERENCE_DEPTH - 1 })
        strict.refusalOf(arrays(2)).reason shouldBe
            RefusalReason.EvaluationDepth
    }

    test("the evaluation depth at its ceiling stays within the stack") {
        val compiled = schemaOf("""{"items": {"${'$'}ref": "#"}}""") {
            limits {
                maximumEvaluationDepth = SchemaLimits.CEILING_EVALUATION_DEPTH
                maximumInstanceDepth = SchemaLimits.CEILING_INSTANCE_DEPTH
            }
        }
        compiled.validate(arrays(SchemaLimits.CEILING_EVALUATION_DEPTH / 2 - 1)).isValid shouldBe true
        compiled.refusalOf(arrays(SchemaLimits.CEILING_EVALUATION_DEPTH)).reason shouldBe RefusalReason.EvaluationDepth
    }

    test("the instance depth") {
        val compiled = schemaOf("{}", limited { maximumInstanceDepth = INSTANCE_DEPTH })
        compiled.validate(arrays(INSTANCE_DEPTH)).isValid shouldBe true
        val refusal = compiled.refusalOf(arrays(INSTANCE_DEPTH + 1))
        refusal.reason shouldBe RefusalReason.InstanceDepth
        refusal.instanceLocation.text shouldBe "/0/0/0/0"
    }

    test("the instance depth at its ceiling stays within the stack") {
        val deepest = arrays(SchemaLimits.CEILING_INSTANCE_DEPTH)
        val compiled = schemaOf("""{"const": 1, "uniqueItems": true}""") {
            limits { maximumInstanceDepth = SchemaLimits.CEILING_INSTANCE_DEPTH }
        }
        compiled.validate(deepest).isValid shouldBe false
        compiled.validate(JsonArray(listOf(deepest, deepest))).isValid shouldBe false
        compiled.refusalOf(JsonArray(listOf(deepest))).reason shouldBe RefusalReason.InstanceDepth
    }

    test("the nodes of a YAML value") {
        val compiled = schemaOf("{}", limited { maximumInstanceNodes = 3 })
        compiled.validate(yamlOf("[1, 2]")).isValid shouldBe true
        val refused = compiled.validate(yamlOf("[1, 2, 3]")).shouldBeInstanceOf<Validation.Refused>()
        refused.reason shouldBe RefusalReason.InstanceSize
    }

    test("the reported errors") {
        val schema = """{"items": {"type": "string"}}"""
        schemaOf(schema, limited { maximumErrors = ERRORS }).errorsOf(THREE_ITEMS).size shouldBe ERRORS
        val strict = schemaOf(schema, limited { maximumErrors = ERRORS - 1 })
        strict.refusalOf(json(THREE_ITEMS)).reason shouldBe RefusalReason.ReportedErrors
        strict.validate(json(THREE_ITEMS), OutputFormat.Flag).isValid shouldBe false
    }

    test("the reported errors stop the validation once there are too many") {
        val schema = """{"items": {"type": "string"}}"""
        val many = JsonArray(List(MANY_ITEMS) { index -> JsonPrimitive(index) })
        val limits = limited {
            maximumErrors = ERRORS
            maximumEvaluationSteps = MANY_ITEMS.toLong()
        }
        val strict = schemaOf(schema, limits)

        strict.refusalOf(many).reason shouldBe RefusalReason.ReportedErrors
    }

    test("the diagnostics of a compilation") {
        val schema = """{"minLength": -1, "maxLength": -1, "minItems": -1}"""
        diagnosticsOf(schema, limited { maximumErrors = ERRORS }).size shouldBe ERRORS
        diagnosticsOf(schema, limited { maximumErrors = ERRORS - 1 }).size shouldBe ERRORS - 1
    }

    test("the length of a string a pattern or a format reads") {
        val pattern = schemaOf("""{"pattern": "a"}""", limited { maximumStringLength = 3 })
        pattern.accepts("\"aaa\"") shouldBe true
        pattern.refusalOf(json("\"aaaa\"")).reason shouldBe RefusalReason.StringLength
        val names = schemaOf("""{"patternProperties": {"a": true}}""", limited { maximumStringLength = 3 })
        names.refusalOf(json("""{"aaaa": 1}""")).reason shouldBe RefusalReason.StringLength
        val format = schemaOf("""{"format": "date"}""") {
            isFormatAsserted = true
            limits { maximumStringLength = 10 }
        }
        format.accepts("\"2024-01-01\"") shouldBe true
        format.refusalOf(json("\"2024-01-01x\"")).reason shouldBe RefusalReason.StringLength
    }

    test("the length of a pattern") {
        schemaOf(
            """{"pattern": "abc"}""",
            limited { maximumPatternLength = PATTERN_LENGTH },
        ).accepts("\"abc\"") shouldBe true
        codesOf("""{"pattern": "abcd"}""", limited { maximumPatternLength = PATTERN_LENGTH }) shouldBe
            listOf(DiagnosticCodes.LIMIT_EXCEEDED)
    }

    test("the values of an enumeration and the nodes of a literal") {
        schemaOf("""{"enum": [1, 2]}""", limited { maximumEnumValues = 2 }).accepts("2") shouldBe true
        codesOf("""{"enum": [1, 2, 3]}""", limited { maximumEnumValues = 2 }) shouldBe
            listOf(DiagnosticCodes.LIMIT_EXCEEDED)
        schemaOf(
            """{"const": [1, 2]}""",
            limited { maximumLiteralNodes = LITERAL_NODES },
        ).accepts("[1, 2]") shouldBe true
        codesOf("""{"const": [1, 2, 3]}""", limited { maximumLiteralNodes = LITERAL_NODES }) shouldBe
            listOf(DiagnosticCodes.LIMIT_EXCEEDED)
        codesOf("""{"enum": [[1, 2, 3, 4]]}""", limited { maximumLiteralNodes = LITERAL_NODES }) shouldBe
            listOf(DiagnosticCodes.LIMIT_EXCEEDED)
    }
})
