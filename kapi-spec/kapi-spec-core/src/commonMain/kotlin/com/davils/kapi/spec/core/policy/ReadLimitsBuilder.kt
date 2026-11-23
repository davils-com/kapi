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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Builds [ReadLimits]. Every limit starts at its default, which suits documents from an untrusted source.
 *
 * A builder is not safe to share between threads; the limits it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ReadLimitsBuilder : ValidatingBuilder<ReadLimits>() {
    override val subject: String get() = "read limits"

    /**
     * The most bytes one document may have, from 1 to [ReadLimits.HARD_DOCUMENT_LENGTH].
     *
     * @since 1.0.0
     */
    public var maximumDocumentLength: Int = DEFAULT_DOCUMENT_LENGTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The most documents one reading may load, the root and every referenced document, from 1 to
     * [ReadLimits.HARD_DOCUMENTS].
     *
     * @since 1.0.0
     */
    public var maximumDocuments: Int = DEFAULT_DOCUMENTS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The deepest nesting of a document, from 1 to [ReadLimits.HARD_DEPTH].
     *
     * @since 1.0.0
     */
    public var maximumDepth: Int = DEFAULT_DEPTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The most nodes of one document, from 1 to [ReadLimits.HARD_NODES].
     *
     * @since 1.0.0
     */
    public var maximumNodes: Long = DEFAULT_NODES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The most nodes that aliases of one document may expand to, from 1 to [ReadLimits.HARD_ALIAS_EXPANSION].
     *
     * @since 1.0.0
     */
    public var maximumAliasExpansion: Long = DEFAULT_ALIAS_EXPANSION
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The longest chain of references a reading follows, from 1 to [ReadLimits.HARD_REFERENCE_HOPS].
     *
     * @since 1.0.0
     */
    public var maximumReferenceHops: Int = DEFAULT_REFERENCE_HOPS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The longest scalar of a document, in characters, from 1 to [ReadLimits.HARD_SCALAR_LENGTH].
     *
     * @since 1.0.0
     */
    public var maximumScalarLength: Int = DEFAULT_SCALAR_LENGTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The longest regular expression of a schema, in characters, from 1 to [ReadLimits.HARD_PATTERN_LENGTH].
     *
     * @since 1.0.0
     */
    public var maximumPatternLength: Int = DEFAULT_PATTERN_LENGTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The most diagnostics a reading reports before it stops reporting, from 1 to [ReadLimits.HARD_DIAGNOSTICS].
     *
     * @since 1.0.0
     */
    public var maximumDiagnostics: Int = DEFAULT_DIAGNOSTICS
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): ReadLimits = ReadLimits(this)

    /**
     * The defaults of the limits.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * 16 MiB.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_DOCUMENT_LENGTH: Int = 16 * 1024 * 1024

        /**
         * 64 documents.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_DOCUMENTS: Int = 64

        /**
         * 128 levels.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_DEPTH: Int = 128

        /**
         * Two million nodes.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_NODES: Long = 2_000_000L

        /**
         * 100 000 expanded nodes.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_ALIAS_EXPANSION: Long = 100_000L

        /**
         * 32 references.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_REFERENCE_HOPS: Int = 32

        /**
         * 1 MiB characters.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_SCALAR_LENGTH: Int = 1024 * 1024

        /**
         * 1024 characters.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_PATTERN_LENGTH: Int = 1024

        /**
         * 1000 diagnostics.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_DIAGNOSTICS: Int = 1000
    }
}
