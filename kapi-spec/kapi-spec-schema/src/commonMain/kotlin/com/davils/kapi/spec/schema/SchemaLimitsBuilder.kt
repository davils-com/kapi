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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures the [SchemaLimits] of a [Compiler] in the `limits { }` block of [CompilerBuilder].
 *
 * Every limit starts at its default and must lie between 1 and its ceiling in [SchemaLimits]; a value outside is
 * reported when the compiler is built. A builder is not thread-safe; it is meant for one block of the DSL.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class SchemaLimitsBuilder internal constructor() : ValidatingBuilder<SchemaLimits>() {
    override val subject: String get() = "schema limits"

    /**
     * The most levels a schema document may nest; see [SchemaLimits.maximumSchemaDepth].
     *
     * @since 1.0.0
     */
    public var maximumSchemaDepth: Int = DEFAULT_SCHEMA_DEPTH

    /**
     * The most subschemas a compilation may compile; see [SchemaLimits.maximumSubschemas].
     *
     * @since 1.0.0
     */
    public var maximumSubschemas: Int = DEFAULT_SUBSCHEMAS

    /**
     * The most documents a compilation may read; see [SchemaLimits.maximumDocuments].
     *
     * @since 1.0.0
     */
    public var maximumDocuments: Int = DEFAULT_DOCUMENTS

    /**
     * The most steps a validation may take; see [SchemaLimits.maximumEvaluationSteps].
     *
     * @since 1.0.0
     */
    public var maximumEvaluationSteps: Long = DEFAULT_EVALUATION_STEPS

    /**
     * The most subschemas a validation may nest; see [SchemaLimits.maximumEvaluationDepth].
     *
     * @since 1.0.0
     */
    public var maximumEvaluationDepth: Int = DEFAULT_EVALUATION_DEPTH

    /**
     * The most levels a value may nest; see [SchemaLimits.maximumInstanceDepth].
     *
     * @since 1.0.0
     */
    public var maximumInstanceDepth: Int = DEFAULT_INSTANCE_DEPTH

    /**
     * The most nodes a value may have; see [SchemaLimits.maximumInstanceNodes].
     *
     * @since 1.0.0
     */
    public var maximumInstanceNodes: Int = DEFAULT_INSTANCE_NODES

    /**
     * The most errors a validation or a compilation may report; see [SchemaLimits.maximumErrors].
     *
     * @since 1.0.0
     */
    public var maximumErrors: Int = DEFAULT_ERRORS

    /**
     * The longest string a pattern or a format may read; see [SchemaLimits.maximumStringLength].
     *
     * @since 1.0.0
     */
    public var maximumStringLength: Int = DEFAULT_STRING_LENGTH

    /**
     * The longest pattern a schema may have; see [SchemaLimits.maximumPatternLength].
     *
     * @since 1.0.0
     */
    public var maximumPatternLength: Int = DEFAULT_PATTERN_LENGTH

    /**
     * The most values an `enum` may have; see [SchemaLimits.maximumEnumValues].
     *
     * @since 1.0.0
     */
    public var maximumEnumValues: Int = DEFAULT_ENUM_VALUES

    /**
     * The most nodes the value of a `const` or an `enum` may have; see [SchemaLimits.maximumLiteralNodes].
     *
     * @since 1.0.0
     */
    public var maximumLiteralNodes: Int = DEFAULT_LITERAL_NODES

    override fun assemble(): SchemaLimits = SchemaLimits(this)

    /**
     * The defaults of the limits.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default of [maximumSchemaDepth].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_SCHEMA_DEPTH: Int = 128

        /**
         * The default of [maximumSubschemas].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_SUBSCHEMAS: Int = 100_000

        /**
         * The default of [maximumDocuments].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_DOCUMENTS: Int = 64

        /**
         * The default of [maximumEvaluationSteps].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_EVALUATION_STEPS: Long = 10_000_000L

        /**
         * The default of [maximumEvaluationDepth].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_EVALUATION_DEPTH: Int = 256

        /**
         * The default of [maximumInstanceDepth].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_INSTANCE_DEPTH: Int = 128

        /**
         * The default of [maximumInstanceNodes].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_INSTANCE_NODES: Int = 1_000_000

        /**
         * The default of [maximumErrors].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_ERRORS: Int = 100

        /**
         * The default of [maximumStringLength].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_STRING_LENGTH: Int = 65_536

        /**
         * The default of [maximumPatternLength].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_PATTERN_LENGTH: Int = 4_096

        /**
         * The default of [maximumEnumValues].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_ENUM_VALUES: Int = 10_000

        /**
         * The default of [maximumLiteralNodes].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_LITERAL_NODES: Int = 100_000
    }
}
