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

import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kapi.codegen.core.options.GeneratorOptions
import com.davils.kapi.codegen.core.pipeline.GenerationRun

public class Generator internal constructor(public val options: GeneratorOptions) {
    public fun generate(
        document: Document,
        sources: List<SourceDocument> = emptyList(),
        catalogs: List<ModelCatalog> = emptyList(),
    ): Generation = GenerationRun(options, catalogs).run(document, sources)

    override fun toString(): String = "Generator($options)"
}
