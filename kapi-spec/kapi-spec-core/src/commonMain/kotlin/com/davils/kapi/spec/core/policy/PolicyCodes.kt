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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations a reading configuration reports when it is built.
 *
 * @since 1.0.0
 */
public object PolicyCodes {
    /**
     * A limit is zero or negative.
     *
     * @since 1.0.0
     */
    public val NOT_POSITIVE: ViolationCode = ViolationCode("kapi.spec.policy.not-positive")

    /**
     * A limit lies above its hard ceiling.
     *
     * @since 1.0.0
     */
    public val ABOVE_HARD_LIMIT: ViolationCode = ViolationCode("kapi.spec.policy.above-hard-limit")

    /**
     * A downgrade of a finding names no reason.
     *
     * @since 1.0.0
     */
    public val REASON_MISSING: ViolationCode = ViolationCode("kapi.spec.policy.reason-missing")

    /**
     * The root of the references names no directory.
     *
     * @since 1.0.0
     */
    public val ROOT_NOT_DIRECTORY: ViolationCode = ViolationCode("kapi.spec.policy.root-not-directory")
}
