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

package com.davils.kapi.spec.schema.internal.keyword

import com.davils.kapi.spec.schema.internal.evaluate.Frame
import com.davils.kapi.spec.schema.internal.evaluate.Keyword
import com.davils.kapi.spec.schema.internal.value.Decimal
import com.davils.kapi.spec.schema.internal.value.Instance

internal class BoundKeyword(
    name: String,
    private val limit: Decimal,
    private val isMaximum: Boolean,
    private val isExclusive: Boolean,
) : Keyword(name) {
    override fun evaluate(frame: Frame): Boolean {
        val number = (frame.instance as? Instance.NumberValue ?: return true).value
        val order = number.compareTo(limit) * (1.takeIf { isMaximum } ?: -1)
        val isWithin = order < 0 || (order == 0 && !isExclusive)
        val relation =
            ("less than".takeIf { isMaximum } ?: "greater than") + " or equal to".takeIf { !isExclusive }.orEmpty()
        return isWithin || failed(frame, "The number $number must be $relation $limit")
    }
}
