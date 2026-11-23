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

package com.davils.kapi.spec.asyncapi.read.shape

import kotlin.math.abs

private const val MAXIMUM_DISTANCE: Int = 2

internal object Suggestions {
    fun nearest(name: String, candidates: Collection<String>): String? {
        val lowered = name.lowercase()
        val near = candidates.filter { candidate -> abs(candidate.length - name.length) <= MAXIMUM_DISTANCE }
        val distances = near.map { candidate -> candidate to distance(left = lowered, right = candidate.lowercase()) }
        val best = distances.minByOrNull { (_, distance) -> distance }
        return best?.takeIf { (_, distance) -> distance <= MAXIMUM_DISTANCE }?.first
    }

    private fun distance(left: String, right: String): Int {
        val start = IntArray(right.length + 1) { index -> index }
        val last = left.withIndex().fold(start) { previous, cell -> next(previous, right, cell) }
        return last[right.length]
    }

    private fun next(previous: IntArray, right: String, cell: IndexedValue<Char>): IntArray {
        val current = IntArray(right.length + 1)
        current[0] = cell.index + 1
        right.indices.forEach { column ->
            val substitution = previous[column] + cost(left = cell.value, right = right[column])
            current[column + 1] = minOf(a = previous[column + 1] + 1, b = current[column] + 1, c = substitution)
        }
        return current
    }

    private fun cost(left: Char, right: Char): Int {
        if (left == right) return 0
        return 1
    }
}
