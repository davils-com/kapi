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

package com.davils.kapi.runtime.core.codec.internal

import kotlin.math.pow

private const val SIGN: Long = 0x8000

private const val EXPONENT_SHIFT: Int = 10

private const val EXPONENT_MASK: Int = 0x1F

private const val MANTISSA_MASK: Long = 0x3FF

private const val IMPLICIT: Double = 1024.0

private const val BIAS: Int = 25

private const val SPECIAL: Int = 31

private const val BASE: Double = 2.0

internal object CborFloats {
    fun half(bits: Long): Double {
        val exponent = (bits shr EXPONENT_SHIFT).toInt() and EXPONENT_MASK
        val mantissa = (bits and MANTISSA_MASK).toDouble()
        val implicit = IMPLICIT.takeIf { exponent > 0 } ?: 0.0
        val magnitude = (mantissa + implicit) * BASE.pow(maxOf(a = exponent, b = 1) - BIAS)
        val finite = magnitude.takeIf { exponent != SPECIAL } ?: Double.NaN
        return finite.takeIf { bits and SIGN == 0L } ?: -finite
    }

    fun single(bits: Long): Double = Float.fromBits(bits.toInt()).toDouble()

    fun double(bits: Long): Double = Double.fromBits(bits)
}
