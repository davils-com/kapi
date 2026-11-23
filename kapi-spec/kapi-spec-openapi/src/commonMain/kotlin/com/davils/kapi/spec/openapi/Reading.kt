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

public sealed interface Reading {
    public val diagnostics: List<Diagnostic>

    public val sources: List<SourceDocument>

    public data class Read(
        public val document: Document,
        override val diagnostics: List<Diagnostic>,
        override val sources: List<SourceDocument>,
    ) : Reading

    public data class Invalid(
        public val partial: Document,
        override val diagnostics: List<Diagnostic>,
        override val sources: List<SourceDocument>,
    ) : Reading

    public data class Malformed(
        override val diagnostics: List<Diagnostic>,
        override val sources: List<SourceDocument>,
    ) : Reading
}
