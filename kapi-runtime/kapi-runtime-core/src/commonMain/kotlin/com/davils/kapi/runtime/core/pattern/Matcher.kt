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

package com.davils.kapi.runtime.core.pattern

import com.davils.kore.core.annotation.DavilsInternal

internal class Matcher(private val program: Program, private val input: CharSequence, private val isSearch: Boolean) {
    private var current = IntArray(program.size)
    private var currentSize = 0
    private var next = IntArray(program.size)
    private var nextSize = 0
    private val marks = IntArray(program.size)
    private val stack = IntArray(program.size * 2 + 2)
    private var generation = 0
    private var hasMatched = false
    private var previous = -1
    private var position = 0

    fun run(): Boolean {
        generation++
        var isDone = false
        while (!isDone) {
            if (isSearch || position == 0) addClosure(0, toNext = false)
            val isAtEnd = position >= input.length
            val isAccepted = hasMatched && (isSearch || isAtEnd)
            isDone = isAccepted || isAtEnd || (currentSize == 0 && !isSearch)
            if (!isDone) step()
        }
        return hasMatched && (isSearch || position >= input.length)
    }

    private fun step() {
        val codePoint = codePointAt(input, position)
        val following = position + charCount(codePoint)
        previous = codePoint
        position = following
        generation++
        hasMatched = false
        nextSize = 0
        repeat(currentSize) { index ->
            val pc = current[index]
            val set = program.sets[pc]
            val isAccepted = program.operations[pc] == Program.CHARACTERS && set != null && codePoint in set
            if (isAccepted) addClosure(pc + 1, toNext = true)
        }
        val swap = current
        current = next
        next = swap
        currentSize = nextSize
    }

    private fun addClosure(start: Int, toNext: Boolean) {
        var top = 0
        stack[top++] = start
        while (top > 0) {
            val pc = stack[--top]
            val isNew = marks[pc] != generation
            marks[pc] = generation
            if (isNew) top = expand(pc, top, toNext)
        }
    }

    private fun expand(pc: Int, top: Int, toNext: Boolean): Int {
        var pushed = top
        val operation = program.operations[pc]
        if (operation == Program.CHARACTERS) keep(pc, toNext)
        if (operation == Program.MATCH) hasMatched = true
        if (operation == Program.JUMP) stack[pushed++] = program.targets[pc]
        if (operation == Program.SPLIT) {
            stack[pushed++] = program.alternatives[pc]
            stack[pushed++] = program.targets[pc]
        }
        if (operation == Program.ANCHOR && holds(program.anchors[pc])) stack[pushed++] = pc + 1
        return pushed
    }

    private fun keep(pc: Int, toNext: Boolean) {
        if (toNext) next[nextSize++] = pc
        if (!toNext) current[currentSize++] = pc
    }

    private fun holds(kind: AnchorKind?): Boolean {
        val following = position.takeIf { it < input.length }?.let { codePointAt(input, it) } ?: -1
        val isBoundary = CharacterSets.isWord(previous) != CharacterSets.isWord(following)
        return when (kind) {
            AnchorKind.INPUT_START -> position == 0
            AnchorKind.INPUT_END -> position >= input.length
            AnchorKind.WORD_BOUNDARY -> isBoundary
            AnchorKind.NOT_WORD_BOUNDARY -> !isBoundary
            null -> false
        }
    }
}
