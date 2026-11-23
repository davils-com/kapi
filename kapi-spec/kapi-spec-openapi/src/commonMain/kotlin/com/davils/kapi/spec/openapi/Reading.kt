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

package com.davils.kapi.spec.openapi

import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.source.SourceDocument

/**
 * The outcome of reading a document: the document with its warnings, a partial document with errors, or no document
 * at all.
 *
 * @since 1.0.0
 */
public sealed interface Reading {
    /**
     * Every finding of the reading, sorted by document and place.
     *
     * @since 1.0.0
     */
    public val diagnostics: List<Diagnostic>

    /**
     * Every document and overlay the reading loaded, the root first.
     *
     * @since 1.0.0
     */
    public val sources: List<SourceDocument>

    /**
     * The document was read without an error.
     *
     * @property document the document.
     * @property diagnostics the warnings of the reading.
     * @property sources the documents the reading loaded.
     * @since 1.0.0
     */
    public class Read(
        public val document: Document,
        override val diagnostics: List<Diagnostic>,
        override val sources: List<SourceDocument>,
    ) : Reading {
        override fun equals(other: Any?): Boolean = other is Read &&
            document == other.document &&
            diagnostics == other.diagnostics &&
            sources == other.sources

        override fun hashCode(): Int = listOf(document, diagnostics, sources).hashCode()

        override fun toString(): String = "Read(${diagnostics.size} diagnostics, ${sources.size} sources)"
    }

    /**
     * The document was read, but breaks the specification; [partial] holds what could be read and must not be
     * trusted.
     *
     * @property partial what could be read.
     * @property diagnostics the findings, at least one of them an error.
     * @property sources the documents the reading loaded.
     * @since 1.0.0
     */
    public class Invalid(
        public val partial: Document,
        override val diagnostics: List<Diagnostic>,
        override val sources: List<SourceDocument>,
    ) : Reading {
        override fun equals(other: Any?): Boolean = other is Invalid &&
            partial == other.partial &&
            diagnostics == other.diagnostics &&
            sources == other.sources

        override fun hashCode(): Int = listOf(partial, diagnostics, sources).hashCode()

        override fun toString(): String = "Invalid(${diagnostics.size} diagnostics, ${sources.size} sources)"
    }

    /**
     * No document could be read, such as for a source that is no YAML or names no version.
     *
     * @property diagnostics the findings.
     * @property sources the documents the reading loaded.
     * @since 1.0.0
     */
    public class Malformed(
        override val diagnostics: List<Diagnostic>,
        override val sources: List<SourceDocument>,
    ) : Reading {
        override fun equals(other: Any?): Boolean =
            other is Malformed && diagnostics == other.diagnostics && sources == other.sources

        override fun hashCode(): Int = listOf(diagnostics, sources).hashCode()

        override fun toString(): String = "Malformed(${diagnostics.size} diagnostics, ${sources.size} sources)"
    }
}
