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

import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.ViolationCode
import kotlin.reflect.KProperty0

/**
 * The limits of compiling schemas and of validating values, each with a hard ceiling.
 *
 * What exceeds a limit is refused, never truncated: a schema that exceeds a limit of compilation is
 * [Compilation.Invalid] with a [DiagnosticCodes.LIMIT_EXCEEDED] diagnostic, and a validation that exceeds a limit of
 * validation is [Validation.Refused] with its [RefusalReason]. Limits are immutable and safe to share between threads.
 *
 * @property maximumSchemaDepth the most levels a schema document may nest, at most [CEILING_SCHEMA_DEPTH].
 * @property maximumSubschemas the most subschemas a compilation may compile, the meta-schemas and every document it
 * loads included, at most [CEILING_SUBSCHEMAS].
 * @property maximumDocuments the most documents a compilation may read: the schema, registered and loaded documents
 * and the built-in meta-schemas it refers to, at most [CEILING_DOCUMENTS].
 * @property maximumEvaluationSteps the most steps one validation may take, at most [CEILING_EVALUATION_STEPS]. Every
 * subschema applied, every failing keyword, every comparison of `uniqueItems` and `enum`, every pattern and format
 * and every long string read costs steps, so no combination of schema and value can make a validation run long.
 * @property maximumEvaluationDepth the most subschemas a validation may nest, through the value and through
 * references alike, at most [CEILING_EVALUATION_DEPTH], which keeps the stack bounded.
 * @property maximumInstanceDepth the most levels a validated value may nest, at most [CEILING_INSTANCE_DEPTH].
 * @property maximumInstanceNodes the most nodes a validated YAML value may have, aliases expanded, at most
 * [CEILING_INSTANCE_NODES].
 * @property maximumErrors the most errors a validation may report and the most diagnostics a compilation may report,
 * at most [CEILING_ERRORS]. A value with more errors is refused with [RefusalReason.ReportedErrors].
 * @property maximumStringLength the longest string a pattern or an asserted format may read, at most
 * [CEILING_STRING_LENGTH].
 * @property maximumPatternLength the longest pattern a schema may have, at most [CEILING_PATTERN_LENGTH].
 * @property maximumEnumValues the most values an `enum` may have, at most [CEILING_ENUM_VALUES].
 * @property maximumLiteralNodes the most nodes the value of a `const` or an `enum` may have, at most
 * [CEILING_LITERAL_NODES].
 * @since 1.0.0
 */
public class SchemaLimits internal constructor(builder: SchemaLimitsBuilder) : Validatable {
    public val maximumSchemaDepth: Int = builder.maximumSchemaDepth

    public val maximumSubschemas: Int = builder.maximumSubschemas

    public val maximumDocuments: Int = builder.maximumDocuments

    public val maximumEvaluationSteps: Long = builder.maximumEvaluationSteps

    public val maximumEvaluationDepth: Int = builder.maximumEvaluationDepth

    public val maximumInstanceDepth: Int = builder.maximumInstanceDepth

    public val maximumInstanceNodes: Int = builder.maximumInstanceNodes

    public val maximumErrors: Int = builder.maximumErrors

    public val maximumStringLength: Int = builder.maximumStringLength

    public val maximumPatternLength: Int = builder.maximumPatternLength

    public val maximumEnumValues: Int = builder.maximumEnumValues

    public val maximumLiteralNodes: Int = builder.maximumLiteralNodes

    private val identity: List<Long>
        get() = listOf(
            maximumSchemaDepth.toLong(),
            maximumSubschemas.toLong(),
            maximumDocuments.toLong(),
            maximumEvaluationSteps,
            maximumEvaluationDepth.toLong(),
            maximumInstanceDepth.toLong(),
            maximumInstanceNodes.toLong(),
            maximumErrors.toLong(),
            maximumStringLength.toLong(),
            maximumPatternLength.toLong(),
            maximumEnumValues.toLong(),
            maximumLiteralNodes.toLong(),
        )

    override fun ValidationScope.validate() {
        bounded(::maximumSchemaDepth, maximumSchemaDepth.toLong(), CEILING_SCHEMA_DEPTH.toLong())
        bounded(::maximumSubschemas, maximumSubschemas.toLong(), CEILING_SUBSCHEMAS.toLong())
        bounded(::maximumDocuments, maximumDocuments.toLong(), CEILING_DOCUMENTS.toLong())
        bounded(::maximumEvaluationSteps, maximumEvaluationSteps, CEILING_EVALUATION_STEPS)
        bounded(::maximumEvaluationDepth, maximumEvaluationDepth.toLong(), CEILING_EVALUATION_DEPTH.toLong())
        bounded(::maximumInstanceDepth, maximumInstanceDepth.toLong(), CEILING_INSTANCE_DEPTH.toLong())
        bounded(::maximumInstanceNodes, maximumInstanceNodes.toLong(), CEILING_INSTANCE_NODES.toLong())
        bounded(::maximumErrors, maximumErrors.toLong(), CEILING_ERRORS.toLong())
        bounded(::maximumStringLength, maximumStringLength.toLong(), CEILING_STRING_LENGTH.toLong())
        bounded(::maximumPatternLength, maximumPatternLength.toLong(), CEILING_PATTERN_LENGTH.toLong())
        bounded(::maximumEnumValues, maximumEnumValues.toLong(), CEILING_ENUM_VALUES.toLong())
        bounded(::maximumLiteralNodes, maximumLiteralNodes.toLong(), CEILING_LITERAL_NODES.toLong())
    }

    private fun ValidationScope.bounded(property: KProperty0<*>, value: Long, ceiling: Long) {
        if (value !in 1L..ceiling) {
            fail(property, "must lie between 1 and $ceiling, but was $value", ViolationCode.OUT_OF_RANGE)
        }
    }

    override fun equals(other: Any?): Boolean = other is SchemaLimits && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String =
        "SchemaLimits(schemaDepth=$maximumSchemaDepth, subschemas=$maximumSubschemas, documents=$maximumDocuments, " +
            "evaluationSteps=$maximumEvaluationSteps, evaluationDepth=$maximumEvaluationDepth, " +
            "instanceDepth=$maximumInstanceDepth, instanceNodes=$maximumInstanceNodes, errors=$maximumErrors, " +
            "stringLength=$maximumStringLength, patternLength=$maximumPatternLength, " +
            "enumValues=$maximumEnumValues, literalNodes=$maximumLiteralNodes)"

    /**
     * The default limits and the ceiling of every limit.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The ceiling of [maximumSchemaDepth].
         *
         * @since 1.0.0
         */
        public const val CEILING_SCHEMA_DEPTH: Int = 512

        /**
         * The ceiling of [maximumSubschemas].
         *
         * @since 1.0.0
         */
        public const val CEILING_SUBSCHEMAS: Int = 1_000_000

        /**
         * The ceiling of [maximumDocuments].
         *
         * @since 1.0.0
         */
        public const val CEILING_DOCUMENTS: Int = 1_024

        /**
         * The ceiling of [maximumEvaluationSteps].
         *
         * @since 1.0.0
         */
        public const val CEILING_EVALUATION_STEPS: Long = 1_000_000_000L

        /**
         * The ceiling of [maximumEvaluationDepth].
         *
         * @since 1.0.0
         */
        public const val CEILING_EVALUATION_DEPTH: Int = 1_024

        /**
         * The ceiling of [maximumInstanceDepth].
         *
         * @since 1.0.0
         */
        public const val CEILING_INSTANCE_DEPTH: Int = 1_024

        /**
         * The ceiling of [maximumInstanceNodes].
         *
         * @since 1.0.0
         */
        public const val CEILING_INSTANCE_NODES: Int = 100_000_000

        /**
         * The ceiling of [maximumErrors].
         *
         * @since 1.0.0
         */
        public const val CEILING_ERRORS: Int = 100_000

        /**
         * The ceiling of [maximumStringLength].
         *
         * @since 1.0.0
         */
        public const val CEILING_STRING_LENGTH: Int = 16_777_216

        /**
         * The ceiling of [maximumPatternLength].
         *
         * @since 1.0.0
         */
        public const val CEILING_PATTERN_LENGTH: Int = 65_536

        /**
         * The ceiling of [maximumEnumValues].
         *
         * @since 1.0.0
         */
        public const val CEILING_ENUM_VALUES: Int = 1_000_000

        /**
         * The ceiling of [maximumLiteralNodes].
         *
         * @since 1.0.0
         */
        public const val CEILING_LITERAL_NODES: Int = 10_000_000

        /**
         * The default limits.
         *
         * @since 1.0.0
         */
        public val Default: SchemaLimits = SchemaLimitsBuilder().build()
    }
}
