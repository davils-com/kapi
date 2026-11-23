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

package com.davils.kapi.codegen.core

import com.davils.kapi.spec.core.diagnostic.Diagnostic

/**
 * What a [Generator] made of a document: the generated files, or the refusal and its reasons.
 *
 * @since 1.0.0
 */
public sealed interface Generation {
    /**
     * The findings of the generation, sorted by their location; a refusal holds at least one error.
     *
     * @since 1.0.0
     */
    public val diagnostics: List<Diagnostic>

    /**
     * The generator emitted Kotlin for the document.
     *
     * @property files the generated files, the provenance last.
     * @property diagnostics the warnings of the generation.
     * @property provenance the options, the sources and the digest of every file.
     * @property catalog the models the generation emitted, for documents that reference them.
     * @since 1.0.0
     */
    public class Generated(
        public val files: List<GeneratedFile>,
        override val diagnostics: List<Diagnostic>,
        public val provenance: Provenance,
        public val catalog: ModelCatalog,
    ) : Generation {
        override fun equals(other: Any?): Boolean = other is Generated &&
            files == other.files &&
            diagnostics == other.diagnostics &&
            provenance == other.provenance &&
            catalog == other.catalog

        override fun hashCode(): Int = listOf(files, diagnostics, provenance, catalog).hashCode()

        override fun toString(): String = "Generated(${files.size} files, ${diagnostics.size} diagnostics)"
    }

    /**
     * The generator refused the document, because it is invalid or asks for what the generator cannot express.
     *
     * @since 1.0.0
     */
    public class Refused(override val diagnostics: List<Diagnostic>) : Generation {
        override fun equals(other: Any?): Boolean = other is Refused && diagnostics == other.diagnostics

        override fun hashCode(): Int = diagnostics.hashCode()

        override fun toString(): String = "Refused(${diagnostics.size} diagnostics)"
    }
}
