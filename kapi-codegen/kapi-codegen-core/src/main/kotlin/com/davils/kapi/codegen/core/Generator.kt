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

import com.davils.kapi.codegen.core.options.GeneratorOptions
import com.davils.kapi.codegen.core.pipeline.GenerationRun
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kapi.spec.openapi.Document

/**
 * Turns OpenAPI documents into Kotlin, made by [Codegen.openApi].
 *
 * A generator holds no state but its options and is safe to share between threads.
 *
 * @property options the options of the generator.
 * @since 1.0.0
 */
public class Generator internal constructor(public val options: GeneratorOptions) {
    /**
     * Generates the Kotlin of [document].
     *
     * The same document, options and generator version yield the same bytes. Every name from the document becomes
     * a valid Kotlin identifier or is refused, and every text is escaped for where it is written.
     *
     * @param document the document, read without an error.
     * @param sources the documents it was read from, whose digests go into the provenance.
     * @param catalogs the models of other generations that the document references.
     * @return the generated files, or the refusal with a located diagnostic for everything the generator cannot
     * express faithfully.
     * @since 1.0.0
     */
    public fun generate(
        document: Document,
        sources: List<SourceDocument> = emptyList(),
        catalogs: List<ModelCatalog> = emptyList(),
    ): Generation = GenerationRun(options, catalogs).run(document, sources)

    override fun toString(): String = "Generator($options)"
}
