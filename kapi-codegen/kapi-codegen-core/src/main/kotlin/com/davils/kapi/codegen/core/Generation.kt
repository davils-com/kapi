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

public sealed interface Generation {
    public val diagnostics: List<Diagnostic>

    public data class Generated(
        public val files: List<GeneratedFile>,
        override val diagnostics: List<Diagnostic>,
        public val provenance: Provenance,
        public val catalog: ModelCatalog,
    ) : Generation

    public data class Refused(override val diagnostics: List<Diagnostic>) : Generation
}
