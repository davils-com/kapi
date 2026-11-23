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

package com.davils.kapi.spec.schema.pattern

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet

internal class GroupScan private constructor(val captures: Int, val names: Set<String>) {
    companion object {
        fun of(source: String): GroupScan {
            val names = OrderedSet<String>()
            var captures = 0
            var isInClass = false
            var index = 0
            while (index < source.length) {
                val char = source[index]
                val isOpeningGroup = !isInClass && char == '('
                val isCapture = isOpeningGroup && capturesAt(source, index + 1)
                if (isCapture) captures++
                if (isCapture && source.startsWith("?<", index + 1)) {
                    readGroupName(source, index + "(?<".length)?.let { names.add(it.value) }
                }
                isInClass = (isInClass || char == '[') && !(isInClass && char == ']')
                index += 2.takeIf { char == '\\' } ?: 1
            }
            return GroupScan(captures, names)
        }

        private fun capturesAt(source: String, index: Int): Boolean {
            val isPlain = source.getOrNull(index) != '?'
            val isNamed = source.startsWith("?<", index) && source.getOrNull(index + 2) !in setOf('=', '!')
            return isPlain || isNamed
        }
    }
}
