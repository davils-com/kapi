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

import com.davils.kapi.spec.core.diagnostic.Diagnostic

/**
 * The result of compiling a schema with a [Compiler].
 *
 * @since 1.0.0
 */
public sealed interface Compilation {
    /**
     * The schema compiled.
     *
     * @property schema the compiled schema, ready to validate values.
     * @since 1.0.0
     */
    public class Compiled internal constructor(public val schema: CompiledSchema) : Compilation {
        override fun toString(): String = "Compiled($schema)"
    }

    /**
     * The schema is not valid or could not be compiled.
     *
     * @property diagnostics every problem found, each an error with its code from [DiagnosticCodes] and its location:
     * the document and the JSON Pointer of the keyword. Compilation stops after [SchemaLimits.maximumErrors]
     * problems or at the first limit it reaches.
     * @since 1.0.0
     */
    public class Invalid internal constructor(public val diagnostics: List<Diagnostic>) : Compilation {
        override fun toString(): String = "Invalid(${diagnostics.joinToString("; ")})"
    }
}
