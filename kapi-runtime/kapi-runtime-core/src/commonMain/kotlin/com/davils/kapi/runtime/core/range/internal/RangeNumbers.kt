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

package com.davils.kapi.runtime.core.range.internal

import com.davils.kapi.runtime.core.internal.FieldGrammar

private const val MOST_DIGITS: Int = 18

internal object RangeNumbers {
    fun parse(text: String): Long? {
        val isNumber = text.length in 1..MOST_DIGITS && text.all(FieldGrammar::isDigit)
        return text.takeIf { isNumber }?.toLong()
    }
}
