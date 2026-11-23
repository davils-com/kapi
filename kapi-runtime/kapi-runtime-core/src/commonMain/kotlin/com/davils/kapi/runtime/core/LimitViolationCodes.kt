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

package com.davils.kapi.runtime.core

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations a set of limits of the runtime reports when it is configured.
 *
 * The codes are stable API: they never change their meaning once released.
 *
 * @since 1.0.0
 */
public object LimitViolationCodes {
    /**
     * A limit is zero or negative.
     *
     * @since 1.0.0
     */
    public val NOT_POSITIVE: ViolationCode = ViolationCode("kapi.runtime.limits.not-positive")

    /**
     * A limit lies above the hard ceiling the runtime allows for it.
     *
     * @since 1.0.0
     */
    public val ABOVE_HARD_LIMIT: ViolationCode = ViolationCode("kapi.runtime.limits.above-hard-limit")
}
