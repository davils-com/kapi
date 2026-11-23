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

import com.davils.kapi.spec.schema.internal.Meter
import com.davils.kapi.spec.schema.internal.value.Instance
import com.davils.kapi.spec.schema.internal.value.Texts

private const val CHARACTERS_PER_STEP: Int = 64

internal enum class Measure(val unit: String) {
    Length("characters"),
    Items("items"),
    Properties("properties"),
    ;

    fun of(instance: Instance, meter: Meter): Long? {
        val size = when (this) {
            Length -> (instance as? Instance.StringValue)?.let { text -> lengthOf(text.value, meter) }
            Items -> (instance as? Instance.ArrayValue)?.run { items.size }
            Properties -> (instance as? Instance.ObjectValue)?.run { keys.size }
        }
        return size?.toLong()
    }

    private fun lengthOf(text: String, meter: Meter): Int {
        meter.charge(text.length.toLong() / CHARACTERS_PER_STEP)
        return Texts.codePoints(text)
    }
}
