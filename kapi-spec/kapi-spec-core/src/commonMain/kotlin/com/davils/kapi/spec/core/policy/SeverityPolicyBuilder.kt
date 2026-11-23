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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import com.davils.kore.core.validation.violation.ViolationCode

/**
 * Builds a [SeverityPolicy].
 *
 * A builder is not safe to share between threads; the policy it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class SeverityPolicyBuilder : ValidatingBuilder<SeverityPolicy>() {
    private val declared: MutableMap<String, String> = linkedMapOf()

    override val subject: String get() = "severity policy"

    /**
     * Whether every warning counts as an error; `false` by default.
     *
     * @since 1.0.0
     */
    public var areWarningsErrors: Boolean = false
        set(value) {
            checkOpen()
            field = value
        }

    internal val downgrades: Map<String, String> get() = declared

    /**
     * Reports the findings of [code] as warnings, for [reason]. A reader refuses a downgrade of a code that guards the
     * reading itself, such as a limit.
     *
     * @param code the code of the finding.
     * @param reason why the finding is acceptable; it must not be blank.
     * @since 1.0.0
     */
    public fun downgrade(code: ViolationCode, reason: String) {
        checkOpen()
        declared[code.value] = reason
    }

    override fun assemble(): SeverityPolicy = SeverityPolicy(this)
}
