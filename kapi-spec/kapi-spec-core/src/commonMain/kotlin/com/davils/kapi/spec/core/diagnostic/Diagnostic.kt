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

package com.davils.kapi.spec.core.diagnostic

import com.davils.kore.core.text.TextSnippet
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode

/**
 * A finding about a document: what rule it breaks, how severe it is, a message and where it is.
 *
 * Codes are stable: a tool may react to [code], never to [message], whose wording may change. The message may contain
 * text from the document; it is escaped when the diagnostic is rendered or shown by [toString].
 *
 * @property code the stable code of the rule, such as `openapi.reference.cycle`.
 * @property severity whether the finding is an error or a warning.
 * @property message what was found, in words.
 * @property location where it was found, or `null` for a finding about the reading as a whole.
 * @property snippet the line of the source around the finding, or `null`.
 * @since 1.0.0
 */
public class Diagnostic(
    public val code: ViolationCode,
    public val severity: Severity,
    public val message: String,
    public val location: Location? = null,
    public val snippet: TextSnippet? = null,
) {
    private val identity: List<Any?> get() = listOf(code, severity, message, location, snippet)

    /**
     * Whether the finding is an error.
     *
     * @since 1.0.0
     */
    public val isError: Boolean get() = severity == Severity.Error

    override fun equals(other: Any?): Boolean = other is Diagnostic && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String = DiagnosticRendering.line(this)
}
