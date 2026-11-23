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

package com.davils.kapi.spec.asyncapi.diagnostic

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode

internal object CodeRules {
    val warnings: Set<ViolationCode> = setOf(
        DiagnosticCodes.REFERENCE_SIBLING,
        DiagnosticCodes.SCHEMA_FORMAT_UNKNOWN,
    )

    val adjustable: Set<ViolationCode> = setOf(
        DiagnosticCodes.VERSION_PATCH_MISSING,
        DiagnosticCodes.FIELD_UNKNOWN,
        DiagnosticCodes.FIELD_VERSION,
        DiagnosticCodes.REFERENCE_SIBLING,
        DiagnosticCodes.KEY_INVALID,
        DiagnosticCodes.SERVER_VARIABLE_DEFAULT,
        DiagnosticCodes.SECURITY_SCOPES,
        DiagnosticCodes.MESSAGE_CONTENT_TYPE,
        DiagnosticCodes.MESSAGE_EXAMPLE_EMPTY,
        DiagnosticCodes.SCHEMA_FORMAT_UNKNOWN,
        DiagnosticCodes.TAG_DUPLICATE,
        DiagnosticCodes.STRUCTURE_SCHEMA,
    )

    fun severityOf(code: ViolationCode): Severity {
        if (code in warnings) return Severity.Warning
        return Severity.Error
    }
}
