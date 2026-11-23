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

package com.davils.kapi.spec.openapi.diagnostic

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode

internal object CodeRules {
    val warnings: Set<ViolationCode> = setOf(
        DiagnosticCodes.VERSION_UPGRADED,
        DiagnosticCodes.UPGRADE_LOSSY,
        DiagnosticCodes.REFERENCE_SIBLING,
        DiagnosticCodes.OPERATION_RESPONSES_EMPTY,
        DiagnosticCodes.PARAMETER_PATH_OPTIONAL,
        DiagnosticCodes.SCHEMA_VERSION_FORM,
        DiagnosticCodes.SCHEMA_PATTERN_UNSUPPORTED,
        DiagnosticCodes.SCHEMA_UNKNOWN_KEYWORD,
        DiagnosticCodes.SCHEMA_XML_DEPRECATED,
    )

    val adjustable: Set<ViolationCode> = setOf(
        DiagnosticCodes.OVERLAY_UNMATCHED,
        DiagnosticCodes.VERSION_PATCH_MISSING,
        DiagnosticCodes.FIELD_UNKNOWN,
        DiagnosticCodes.FIELD_VERSION,
        DiagnosticCodes.STRUCTURE_SCHEMA,
        DiagnosticCodes.PATH_AMBIGUOUS,
        DiagnosticCodes.PATH_PARAMETER_UNUSED,
        DiagnosticCodes.SERVER_VARIABLE_DEFAULT,
        DiagnosticCodes.SECURITY_SCOPES,
        DiagnosticCodes.SCHEMA_CONSTRAINT,
        DiagnosticCodes.SCHEMA_PATTERN_UNSUPPORTED,
        DiagnosticCodes.EXTENSION_NAME,
        DiagnosticCodes.TAG_DUPLICATE,
        DiagnosticCodes.LINK_TARGET,
        DiagnosticCodes.EXAMPLE_SHAPE,
        DiagnosticCodes.MEDIA_TYPE_INVALID,
        DiagnosticCodes.COMPONENT_NAME,
    )

    fun severityOf(code: ViolationCode): Severity {
        if (code in warnings) return Severity.Warning
        return Severity.Error
    }
}
