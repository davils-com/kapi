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

package com.davils.kapi.spec.schema.internal.value

import com.davils.kapi.spec.schema.internal.Meter

private const val CHARACTERS_PER_STEP: Int = 64

internal object Canonical {
    fun of(instance: Instance, meter: Meter): String = buildString { write(instance, this, meter) }

    private fun write(instance: Instance, out: StringBuilder, meter: Meter) {
        meter.charge(1L)
        when (instance) {
            is Instance.NullValue -> out.append('n')
            is Instance.BooleanValue -> out.append('t'.takeIf { instance.value } ?: 'f')
            is Instance.NumberValue -> number(instance.value, out)
            is Instance.StringValue -> text(instance.value, out, meter)
            is Instance.ArrayValue -> items(instance, out, meter)
            is Instance.ObjectValue -> members(instance, out, meter)
        }
    }

    private fun text(value: String, out: StringBuilder, meter: Meter) {
        meter.charge(value.length.toLong() / CHARACTERS_PER_STEP)
        out.append('s')
        out.append(value.length)
        out.append(':')
        out.append(value)
    }

    private fun number(value: Decimal, out: StringBuilder) {
        out.append('d')
        out.append(value.canonical)
        out.append(';')
    }

    private fun items(instance: Instance.ArrayValue, out: StringBuilder, meter: Meter) {
        out.append('[')
        instance.items.forEach { item -> write(item, out, meter) }
        out.append(']')
    }

    private fun members(instance: Instance.ObjectValue, out: StringBuilder, meter: Meter) {
        out.append('{')
        val order = instance.keys.indices.sortedBy { index -> instance.keys[index] }
        order.forEach { index ->
            text(instance.keys[index], out, meter)
            write(instance.values[index], out, meter)
        }
        out.append('}')
    }
}
