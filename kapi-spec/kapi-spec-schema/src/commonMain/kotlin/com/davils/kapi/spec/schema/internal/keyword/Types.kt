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

import com.davils.kapi.spec.schema.internal.value.Instance

internal object Types {
    const val NULL: Int = 1

    const val BOOLEAN: Int = 2

    const val OBJECT: Int = 4

    const val ARRAY: Int = 8

    const val NUMBER: Int = 16

    const val STRING: Int = 32

    const val INTEGER: Int = 64

    private val bits: Map<String, Int> = mapOf(
        "null" to NULL,
        "boolean" to BOOLEAN,
        "object" to OBJECT,
        "array" to ARRAY,
        "number" to NUMBER,
        "string" to STRING,
        "integer" to INTEGER,
    )

    fun bitOf(name: String): Int? = bits[name]

    fun bitsOf(instance: Instance, isIntegerStrict: Boolean): Int = when (instance) {
        is Instance.NullValue -> NULL
        is Instance.BooleanValue -> BOOLEAN
        is Instance.ObjectValue -> OBJECT
        is Instance.ArrayValue -> ARRAY
        is Instance.StringValue -> STRING
        is Instance.NumberValue -> numberBits(instance, isIntegerStrict)
    }

    private fun numberBits(instance: Instance.NumberValue, isIntegerStrict: Boolean): Int {
        val isInteger = instance.value.isInteger && (!isIntegerStrict || instance.value.isPlain)
        return NUMBER or (INTEGER.takeIf { isInteger } ?: 0)
    }
}
