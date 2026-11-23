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

package com.davils.kapi.spec.core.policy

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.reflect.KProperty0

/**
 * The limits of a reading, which bound the work any document can cause before the work is done.
 *
 * Every limit must be at least 1 and at most its hard ceiling, such as [HARD_DOCUMENT_LENGTH]; building limits outside
 * these bounds fails. A reading refuses what exceeds a limit with a diagnostic and never truncates silently. Limits are
 * immutable and safe to share between threads.
 *
 * @param builder the builder whose values the limits take over.
 * @since 1.0.0
 */
public class ReadLimits internal constructor(builder: ReadLimitsBuilder) : Validatable {
    /**
     * The most bytes one document may have.
     *
     * @since 1.0.0
     */
    public val maximumDocumentLength: Int = builder.maximumDocumentLength

    /**
     * The most documents one reading may load.
     *
     * @since 1.0.0
     */
    public val maximumDocuments: Int = builder.maximumDocuments

    /**
     * The deepest nesting of a document.
     *
     * @since 1.0.0
     */
    public val maximumDepth: Int = builder.maximumDepth

    /**
     * The most nodes of one document.
     *
     * @since 1.0.0
     */
    public val maximumNodes: Long = builder.maximumNodes

    /**
     * The most nodes that aliases of one document may expand to.
     *
     * @since 1.0.0
     */
    public val maximumAliasExpansion: Long = builder.maximumAliasExpansion

    /**
     * The longest chain of references a reading follows.
     *
     * @since 1.0.0
     */
    public val maximumReferenceHops: Int = builder.maximumReferenceHops

    /**
     * The longest scalar of a document, in characters.
     *
     * @since 1.0.0
     */
    public val maximumScalarLength: Int = builder.maximumScalarLength

    /**
     * The longest regular expression of a schema, in characters.
     *
     * @since 1.0.0
     */
    public val maximumPatternLength: Int = builder.maximumPatternLength

    /**
     * The most diagnostics a reading reports.
     *
     * @since 1.0.0
     */
    public val maximumDiagnostics: Int = builder.maximumDiagnostics

    override fun ValidationScope.validate() {
        bounded(
            property = ::maximumDocumentLength,
            value = maximumDocumentLength.toLong(),
            ceiling = HARD_DOCUMENT_LENGTH.toLong(),
        )
        bounded(property = ::maximumDocuments, value = maximumDocuments.toLong(), ceiling = HARD_DOCUMENTS.toLong())
        bounded(property = ::maximumDepth, value = maximumDepth.toLong(), ceiling = HARD_DEPTH.toLong())
        bounded(property = ::maximumNodes, value = maximumNodes, ceiling = HARD_NODES)
        bounded(property = ::maximumAliasExpansion, value = maximumAliasExpansion, ceiling = HARD_ALIAS_EXPANSION)
        bounded(
            property = ::maximumReferenceHops,
            value = maximumReferenceHops.toLong(),
            ceiling = HARD_REFERENCE_HOPS.toLong(),
        )
        bounded(
            property = ::maximumScalarLength,
            value = maximumScalarLength.toLong(),
            ceiling = HARD_SCALAR_LENGTH.toLong(),
        )
        bounded(
            property = ::maximumPatternLength,
            value = maximumPatternLength.toLong(),
            ceiling = HARD_PATTERN_LENGTH.toLong(),
        )
        bounded(
            property = ::maximumDiagnostics,
            value = maximumDiagnostics.toLong(),
            ceiling = HARD_DIAGNOSTICS.toLong(),
        )
    }

    override fun toString(): String =
        "ReadLimits(document=$maximumDocumentLength, documents=$maximumDocuments, depth=$maximumDepth, " +
            "nodes=$maximumNodes, aliasExpansion=$maximumAliasExpansion, referenceHops=$maximumReferenceHops, " +
            "scalar=$maximumScalarLength, pattern=$maximumPatternLength, diagnostics=$maximumDiagnostics)"

    private fun ValidationScope.bounded(property: KProperty0<*>, value: Long, ceiling: Long) {
        if (value < 1) fail(property, "must be at least 1, but was $value", PolicyCodes.NOT_POSITIVE)
        if (value > ceiling) fail(property, "must not exceed $ceiling, but was $value", PolicyCodes.ABOVE_HARD_LIMIT)
    }

    /**
     * The default limits and the hard ceilings no configuration may exceed.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The ceiling of [maximumDocumentLength]: the longest input the YAML parser reads.
         *
         * @since 1.0.0
         */
        public const val HARD_DOCUMENT_LENGTH: Int = YamlLimits.HARD_INPUT_LENGTH

        /**
         * The ceiling of [maximumDocuments]: 4096 documents.
         *
         * @since 1.0.0
         */
        public const val HARD_DOCUMENTS: Int = 4096

        /**
         * The ceiling of [maximumDepth]: the depth the YAML parser nests safely.
         *
         * @since 1.0.0
         */
        public const val HARD_DEPTH: Int = YamlLimits.HARD_DEPTH

        /**
         * The ceiling of [maximumNodes]: 50 million nodes.
         *
         * @since 1.0.0
         */
        public const val HARD_NODES: Long = 50_000_000L

        /**
         * The ceiling of [maximumAliasExpansion]: 10 million expanded nodes.
         *
         * @since 1.0.0
         */
        public const val HARD_ALIAS_EXPANSION: Long = 10_000_000L

        /**
         * The ceiling of [maximumReferenceHops]: 1024 references.
         *
         * @since 1.0.0
         */
        public const val HARD_REFERENCE_HOPS: Int = 1024

        /**
         * The ceiling of [maximumScalarLength]: the longest input the YAML parser reads.
         *
         * @since 1.0.0
         */
        public const val HARD_SCALAR_LENGTH: Int = YamlLimits.HARD_INPUT_LENGTH

        /**
         * The ceiling of [maximumPatternLength]: 64 KiB characters.
         *
         * @since 1.0.0
         */
        public const val HARD_PATTERN_LENGTH: Int = 64 * 1024

        /**
         * The ceiling of [maximumDiagnostics]: 100 000 diagnostics.
         *
         * @since 1.0.0
         */
        public const val HARD_DIAGNOSTICS: Int = 100_000

        /**
         * The default limits.
         *
         * @since 1.0.0
         */
        public val Default: ReadLimits = ReadLimitsBuilder().build()
    }
}
