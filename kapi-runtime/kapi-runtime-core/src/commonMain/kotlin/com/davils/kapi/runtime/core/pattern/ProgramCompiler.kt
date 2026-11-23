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

private const val SIZE_CEILING: Long = Long.MAX_VALUE / 4
private const val LOOP_OVERHEAD: Long = 2

internal class ProgramCompiler private constructor() {
    private val operations = mutableListOf<Int>()
    private val targets = mutableListOf<Int>()
    private val alternatives = mutableListOf<Int>()
    private val sets = mutableListOf<CodePointSet?>()
    private val anchors = mutableListOf<AnchorKind?>()

    private fun emit(node: PatternNode) {
        when (node) {
            PatternNode.Empty -> Unit
            is PatternNode.Characters -> add(Program.CHARACTERS, set = node.set)
            is PatternNode.Anchor -> add(Program.ANCHOR, anchor = node.kind)
            is PatternNode.Sequence -> node.items.forEach(::emit)
            is PatternNode.Choice -> choice(node.options)
            is PatternNode.Repetition -> repetition(node)
        }
    }

    private fun choice(options: List<PatternNode>) {
        val jumps = mutableListOf<Int>()
        options.dropLast(1).forEach { option ->
            val split = add(Program.SPLIT)
            targets[split] = split + 1
            emit(option)
            jumps += add(Program.JUMP)
            alternatives[split] = operations.size
        }
        emit(options.last())
        jumps.forEach { jump -> targets[jump] = operations.size }
    }

    private fun repetition(node: PatternNode.Repetition) {
        repeat(node.minimum) { emit(node.body) }
        val maximum = node.maximum
        if (maximum == null) {
            val split = add(Program.SPLIT)
            targets[split] = split + 1
            emit(node.body)
            val jump = add(Program.JUMP)
            targets[jump] = split
            alternatives[split] = operations.size
            return
        }
        val splits = List(maximum - node.minimum) {
            val split = add(Program.SPLIT)
            targets[split] = split + 1
            emit(node.body)
            split
        }
        splits.forEach { split -> alternatives[split] = operations.size }
    }

    private fun add(operation: Int, set: CodePointSet? = null, anchor: AnchorKind? = null): Int {
        operations += operation
        targets += 0
        alternatives += 0
        sets += set
        anchors += anchor
        return operations.size - 1
    }

    private fun toProgram(): Program = Program(
        operations = operations.toIntArray(),
        targets = targets.toIntArray(),
        alternatives = alternatives.toIntArray(),
        sets = sets.toList(),
        anchors = anchors.toList(),
    )

    companion object {
        fun compile(node: PatternNode): Program {
            val compiler = ProgramCompiler()
            compiler.emit(node)
            compiler.add(Program.MATCH)
            return compiler.toProgram()
        }

        fun states(node: PatternNode): Long = size(node) + 1

        private fun size(node: PatternNode): Long = when (node) {
            PatternNode.Empty -> 0
            is PatternNode.Characters -> 1
            is PatternNode.Anchor -> 1
            is PatternNode.Sequence -> node.items.fold(0L) { total, item -> total saturatedPlus size(item) }
            is PatternNode.Choice -> choiceSize(node.options)
            is PatternNode.Repetition -> repetitionSize(node)
        }

        private fun choiceSize(options: List<PatternNode>): Long {
            val bodies = options.fold(0L) { total, option -> total saturatedPlus size(option) }
            return bodies saturatedPlus LOOP_OVERHEAD * (options.size - 1)
        }

        private fun repetitionSize(node: PatternNode.Repetition): Long {
            val body = size(node.body)
            val required = node.minimum.toLong() saturatedTimes body
            val optional = node.maximum?.let { maximum -> (maximum - node.minimum).toLong() saturatedTimes body + 1 }
            return required saturatedPlus (optional ?: (body saturatedPlus LOOP_OVERHEAD))
        }

        private infix fun Long.saturatedPlus(other: Long): Long = (this + other).coerceAtMost(SIZE_CEILING)

        private infix fun Long.saturatedTimes(other: Long): Long {
            val isOverflowing = other != 0L && this > SIZE_CEILING / other
            return SIZE_CEILING.takeIf { isOverflowing } ?: (this * other).coerceAtMost(SIZE_CEILING)
        }
    }
}
