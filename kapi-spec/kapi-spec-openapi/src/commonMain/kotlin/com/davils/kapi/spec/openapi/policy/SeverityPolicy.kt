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

package com.davils.kapi.spec.openapi.policy

import com.davils.kapi.spec.openapi.ReaderViolationCodes
import com.davils.kapi.spec.openapi.diagnostic.CodeRules
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode

public class SeverityPolicy internal constructor(
    builder: SeverityPolicyBuilder,
) : Validatable {
    public val areWarningsErrors: Boolean = builder.areWarningsErrors

    public val downgrades: Map<String, String> = builder.downgrades.toMap()

    override fun ValidationScope.validate() {
        each(::downgrades, downgrades) { reason ->
            if (reason.isBlank()) {
                fail(
                    "needs a reason that says why the finding is acceptable",
                    ReaderViolationCodes.REASON_MISSING,
                )
            }
        }
        val fixed = downgrades.keys.filterNot { code -> isAdjustable(code) }
        fixed.forEach { code ->
            fail(
                ::downgrades,
                "names $code, which keeps its severity because it guards the reader itself",
                ReaderViolationCodes.SEVERITY_FIXED,
            )
        }
    }

    public fun isDowngraded(code: ViolationCode): Boolean = code.value in downgrades

    override fun toString(): String =
        "SeverityPolicy(areWarningsErrors=$areWarningsErrors, downgrades=${downgrades.keys})"

    internal fun severityOf(code: ViolationCode, base: Severity = CodeRules.severityOf(code)): Severity {
        val promoted = base.takeUnless { areWarningsErrors } ?: Severity.Error
        return Severity.Warning.takeIf { isDowngraded(code) } ?: promoted
    }

    private fun isAdjustable(code: String): Boolean = CodeRules.adjustable.any { adjustable ->
        adjustable.value == code
    }

    public companion object {
        public val Default: SeverityPolicy = SeverityPolicyBuilder().build()
    }
}
