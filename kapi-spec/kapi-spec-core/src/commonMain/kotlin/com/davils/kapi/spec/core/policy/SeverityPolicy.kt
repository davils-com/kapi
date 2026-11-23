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

package com.davils.kapi.spec.core.policy

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode

/**
 * How severe the findings of a reading are: every warning may become an error, and chosen findings may become
 * warnings, each with its reason.
 *
 * A policy is immutable and safe to share between threads.
 *
 * @param builder the builder whose values the policy takes over.
 * @since 1.0.0
 */
public class SeverityPolicy internal constructor(
    builder: SeverityPolicyBuilder,
) : Validatable {
    /**
     * Whether every warning counts as an error.
     *
     * @since 1.0.0
     */
    public val areWarningsErrors: Boolean = builder.areWarningsErrors

    /**
     * The codes whose findings are warnings, each with its reason.
     *
     * @since 1.0.0
     */
    public val downgrades: Map<String, String> = builder.downgrades.toMap()

    override fun ValidationScope.validate() {
        each(::downgrades, downgrades) { reason ->
            if (reason.isBlank()) {
                fail("needs a reason that says why the finding is acceptable", PolicyCodes.REASON_MISSING)
            }
        }
    }

    /**
     * Whether the findings of [code] are downgraded to warnings.
     *
     * @param code the code.
     * @return `true` if the policy downgrades it.
     * @since 1.0.0
     */
    public fun isDowngraded(code: ViolationCode): Boolean = code.value in downgrades

    /**
     * Returns the severity of a finding of [code] whose own severity is [base].
     *
     * @param code the code of the finding.
     * @param base the severity of the finding without the policy.
     * @return the severity under the policy.
     * @since 1.0.0
     */
    @DavilsInternal
    public fun severityOf(code: ViolationCode, base: Severity): Severity {
        val promoted = base.takeUnless { areWarningsErrors } ?: Severity.Error
        return Severity.Warning.takeIf { isDowngraded(code) } ?: promoted
    }

    override fun toString(): String =
        "SeverityPolicy(areWarningsErrors=$areWarningsErrors, downgrades=${downgrades.keys})"

    /**
     * Holds the default policy.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Every finding keeps its own severity.
         *
         * @since 1.0.0
         */
        public val Default: SeverityPolicy = SeverityPolicyBuilder().build()
    }
}
