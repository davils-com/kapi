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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.ValidationScope
import kotlin.reflect.KProperty0

/**
 * Checks that the limit [property] lies between 1 and [ceiling]: a value below 1 fails with
 * [LimitViolationCodes.NOT_POSITIVE] and one above [ceiling] with [LimitViolationCodes.ABOVE_HARD_LIMIT], both
 * reported on [property].
 *
 * Internal to kapi: the limits of the runtime modules validate themselves with it.
 *
 * @param property the limit, read once as a whole number.
 * @param ceiling the hard ceiling of the limit.
 * @since 1.0.0
 */
@DavilsInternal
public fun ValidationScope.bounded(property: KProperty0<Number>, ceiling: Long) {
    val value = property.get().toLong()
    if (value < 1) {
        fail(property, "must be at least 1, but was $value", LimitViolationCodes.NOT_POSITIVE)
    }
    if (value > ceiling) {
        fail(property, "must not exceed $ceiling, but was $value", LimitViolationCodes.ABOVE_HARD_LIMIT)
    }
}
