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

internal object CborFormat {
    const val MAJOR_SHIFT: Int = 5

    const val INFO_MASK: Int = 0x1F

    const val DIRECT: Int = 24

    const val INDEFINITE: Int = 31

    const val BYTE_BITS: Int = 8

    const val BYTE_MASK: Int = 0xFF

    const val FALSE: Int = 20

    const val TRUE: Int = 21

    const val NULL: Int = 22

    const val UNDEFINED: Int = 23

    const val HALF: Int = 25

    const val FLOAT: Int = 26

    const val DOUBLE: Int = 27

    val widths: Map<Int, Int> = mapOf(DIRECT to 1, HALF to 2, FLOAT to Int.SIZE_BYTES, DOUBLE to Long.SIZE_BYTES)

    fun initial(major: CborMajor, info: Int): Byte = (major.ordinal shl MAJOR_SHIFT or info).toByte()
}
