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

package com.davils.kapi.spec.asyncapi

import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.source.SourceDocument

private const val READING: String = "Reading"

/**
 * The outcome of one reading: the document, the findings in the order of their place and the documents the reading
 * loaded.
 *
 * A reading is immutable and safe to share between threads. [toString] names the outcome and the number of findings,
 * never content of the document.
 *
 * @since 1.0.0
 */
public sealed class Reading {
    /**
     * The findings of the reading, sorted by document, place and code.
     *
     * @since 1.0.0
     */
    public abstract val diagnostics: List<Diagnostic>

    /**
     * The documents the reading loaded, the root first, with their digests.
     *
     * @since 1.0.0
     */
    public abstract val sources: List<SourceDocument>

    internal abstract val identity: List<Any?>

    override fun equals(other: Any?): Boolean =
        other is Reading && other::class == this::class && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String {
        val name = this::class.simpleName ?: READING
        return "$name(${diagnostics.size} diagnostics)"
    }

    /**
     * The document was read without errors; [diagnostics] holds warnings only.
     *
     * @property document the document.
     * @property diagnostics the findings of the reading, sorted by document, place and code.
     * @property sources the documents the reading loaded, the root first, with their digests.
     * @since 1.0.0
     */
    public class Read internal constructor(
        public val document: Document,
        override val diagnostics: List<Diagnostic>,
        override val sources: List<SourceDocument>,
    ) : Reading() {
        override val identity: List<Any?> get() = listOf(document, diagnostics, sources)
    }

    /**
     * The document was read, but breaks rules of AsyncAPI; [diagnostics] holds at least one error.
     *
     * @property partial what could be read of the document.
     * @property diagnostics the findings of the reading, sorted by document, place and code.
     * @property sources the documents the reading loaded, the root first, with their digests.
     * @since 1.0.0
     */
    public class Invalid internal constructor(
        public val partial: Document,
        override val diagnostics: List<Diagnostic>,
        override val sources: List<SourceDocument>,
    ) : Reading() {
        override val identity: List<Any?> get() = listOf(partial, diagnostics, sources)
    }

    /**
     * No document could be read, such as for text that is no YAML, no object or no AsyncAPI 3 document.
     *
     * @property diagnostics the findings of the reading, sorted by document, place and code.
     * @property sources the documents the reading loaded, the root first, with their digests.
     * @since 1.0.0
     */
    public class Malformed internal constructor(
        override val diagnostics: List<Diagnostic>,
        override val sources: List<SourceDocument>,
    ) : Reading() {
        override val identity: List<Any?> get() = listOf(diagnostics, sources)
    }
}
