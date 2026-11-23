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

package com.davils.kapi.spec.schema.pattern

private const val BACKSPACE: Int = 0x08
private const val ESCAPE_AT_END: String = "a \\ ends the pattern"

internal class ClassReader(private val cursor: PatternCursor, private val escapes: EscapeReader) {
    fun characterClass(): PatternNode? {
        if (!cursor.isAt('[')) return null
        val start = cursor.index
        cursor.index++
        val isNegated = cursor.accept("^")
        val ranges = CodePointSet.Ranges()
        while (!cursor.isAt(']')) {
            if (cursor.isAtEnd) cursor.fail(start, "a [ opens a class that is never closed")
            range(ranges)
        }
        cursor.index++
        val set = ranges.toSet()
        return PatternNode.Characters(set.complement().takeIf { isNegated } ?: set)
    }

    private fun range(ranges: CodePointSet.Ranges) {
        val start = cursor.index
        val (first, firstSet) = atom()
        val hasDash = cursor.isAt('-') && cursor.index + 1 < cursor.source.length
        val isRange = hasDash && cursor.source[cursor.index + 1] != ']'
        if (!isRange) {
            firstSet?.let { ranges.addBounds(it.bounds()) }
            first?.let(ranges::addSingle)
            return
        }
        cursor.index++
        val (second, _) = atom()
        val low = first ?: cursor.fail(start, "a range of a class cannot start at a class escape")
        val high = second ?: cursor.fail(start, "a range of a class cannot end at a class escape")
        if (low > high) cursor.fail(start, "the range of a class is out of order")
        ranges.add(first = low, last = high)
    }

    private fun atom(): Pair<Int?, CodePointSet?> {
        val isEscape = cursor.isAt('\\')
        if (isEscape) return escape()
        return cursor.codePoint() to null
    }

    private fun escape(): Pair<Int?, CodePointSet?> {
        val start = cursor.index
        cursor.index++
        if (cursor.isAtEnd) cursor.fail(start, ESCAPE_AT_END)
        val set = escapes.escapeSet(start)
        if (set != null) return null to set
        return (classOnlyEscape(start) ?: escapes.characterEscape(start)) to null
    }

    private fun classOnlyEscape(start: Int): Int? {
        val isBackspace = cursor.accept("b")
        val isDash = !isBackspace && cursor.accept("-")
        if (!isBackspace && !isDash && cursor.isAt('k')) cursor.fail(start, "\\k cannot appear in a class")
        return BACKSPACE.takeIf { isBackspace } ?: '-'.code.takeIf { isDash }
    }
}
