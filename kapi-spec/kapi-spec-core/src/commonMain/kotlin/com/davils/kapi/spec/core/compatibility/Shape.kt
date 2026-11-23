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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.core.compatibility

import com.davils.kore.core.annotation.DavilsInternal

private const val REFUSING: Int = 0

private const val CONSTRAINED: Int = 1

private const val ACCEPTING: Int = 2

private val names: List<String> = listOf("accepting nothing", "a schema object", "accepting everything")

internal enum class Shape {
    Opaque,
    Fixed,
    Keywords,
    ;

    companion object {
        fun of(pairing: Pairing): Shape {
            val isOpaque = pairing.baseline.opaque != null || pairing.candidate.opaque != null
            val isFixed = pairing.baseline.isAccepting != null || pairing.candidate.isAccepting != null
            val fixed = Fixed.takeIf { isFixed } ?: Keywords
            return Opaque.takeIf { isOpaque } ?: fixed
        }

        fun rankOf(view: SchemaView): Int {
            val isAccepting = view.isAccepting ?: return CONSTRAINED
            return ACCEPTING.takeIf { isAccepting } ?: REFUSING
        }

        fun nameOf(rank: Int): String = names[rank]
    }
}
