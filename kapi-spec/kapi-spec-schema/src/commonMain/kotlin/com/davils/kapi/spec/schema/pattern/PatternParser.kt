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

private const val MODIFIER_FLAGS: String = "ims"
private const val MAXIMUM_COUNT_DIGITS: Int = 9
private const val COUNT_CEILING: Int = 1_000_000_000

internal class PatternParser(source: String, private val maximumNesting: Int) {
    private val cursor: PatternCursor = PatternCursor(source)
    private val escapes: EscapeReader = EscapeReader(cursor, GroupScan.of(source))
    private val classes: ClassReader = ClassReader(cursor, escapes)
    private val names: GroupNameScope = GroupNameScope()
    private var depth: Int = 0

    val unsupported: PatternIssue? get() = cursor.unsupported

    fun parse(): PatternNode {
        val node = disjunction()
        if (!cursor.isAtEnd) cursor.fail(cursor.index, "a ) closes a group that was never opened")
        return node
    }

    private fun disjunction(): PatternNode {
        val mark = names.mark()
        val closed = mutableListOf<String>()
        val options = mutableListOf(alternative())
        while (cursor.accept("|")) {
            closed += names.closeAlternative(mark)
            options += alternative()
        }
        names.restore(closed)
        return options.singleOrNull() ?: PatternNode.Choice(options)
    }

    private fun alternative(): PatternNode {
        val items = mutableListOf<PatternNode>()
        while (!cursor.isAtEnd && !cursor.isAt('|') && !cursor.isAt(')')) items += term()
        return items.singleOrNull() ?: PatternNode.Sequence(items)
    }

    private fun term(): PatternNode {
        val assertion = anchor() ?: lookaround()
        if (assertion != null) {
            if (cursor.isAtQuantifier) cursor.fail(cursor.index, "an assertion cannot be repeated")
            return assertion
        }
        return quantified(atom())
    }

    private fun anchor(): PatternNode? {
        val anchor = ANCHORS.firstOrNull { (token, _) -> cursor.accept(token) }
        return anchor?.let { (_, kind) -> PatternNode.Anchor(kind) }
    }

    private fun lookaround(): PatternNode? {
        val start = cursor.index
        val token = LOOKAROUNDS.firstOrNull { cursor.source.startsWith(it, start) } ?: return null
        enter(start)
        cursor.index += token.length
        disjunction()
        close(start)
        cursor.record(PatternRefusal.Lookaround, start, "a lookaround cannot run in the linear engine")
        return PatternNode.Empty
    }

    private fun quantified(atom: PatternNode): PatternNode {
        val bounds = quantifier() ?: return atom
        cursor.accept("?")
        if (cursor.isAtQuantifier) cursor.fail(cursor.index, "a quantifier cannot be repeated")
        val (minimum, maximum) = bounds
        return PatternNode.Repetition(atom, minimum, maximum)
    }

    private fun quantifier(): Pair<Int, Int?>? {
        val simple = SIMPLE_QUANTIFIERS.firstOrNull { (token, _) -> cursor.accept(token) }
        return simple?.second ?: braced()
    }

    private fun braced(): Pair<Int, Int?>? {
        if (!cursor.isAt('{')) return null
        val start = cursor.index
        cursor.index++
        val minimum = cursor.digits()
        val hasComma = cursor.accept(",")
        val maximum = cursor.digits().takeIf { hasComma } ?: minimum
        val isComplete = minimum.isNotEmpty() && cursor.accept("}")
        if (!isComplete) cursor.fail(start, "a { starts no complete quantifier such as {2}, {2,} or {2,5}")
        val isUnbounded = hasComma && maximum.isEmpty()
        val isOrdered = isUnbounded || compareCounts(smaller = minimum, larger = maximum) <= 0
        if (!isOrdered) cursor.fail(start, "the quantifier counts are out of order")
        return count(minimum) to count(maximum).takeUnless { isUnbounded }
    }

    private fun atom(): PatternNode = group() ?: classes.characterClass() ?: escape() ?: dot() ?: literal()

    private fun dot(): PatternNode? {
        val isDot = cursor.accept(".")
        return PatternNode.Characters(CharacterSets.dot).takeIf { isDot }
    }

    private fun literal(): PatternNode {
        val char = cursor.current
        if (cursor.isAtQuantifier) cursor.fail(cursor.index, "a quantifier has nothing to repeat")
        if (char == ']' || char == '}') cursor.fail(cursor.index, "a lone $char must be escaped as \\$char")
        return PatternNode.Characters(CodePointSet.single(cursor.codePoint()))
    }

    private fun group(): PatternNode? {
        if (!cursor.isAt('(')) return null
        val start = cursor.index
        enter(start)
        cursor.index++
        groupHead(start)
        val body = disjunction()
        close(start)
        return body
    }

    private fun groupHead(start: Int) {
        val isSpecial = cursor.isAt('?') && !cursor.accept("?:")
        if (!isSpecial) return
        val isNamed = cursor.accept("?<")
        if (isNamed) declare(start)
        if (!isNamed) modifiers(start)
    }

    private fun declare(start: Int) {
        val name = readGroupName(cursor.source, cursor.index)
        val declared = name ?: cursor.fail(cursor.index, "a group name is no valid identifier")
        if (!names.declare(declared.value)) cursor.fail(start, "a group name is declared twice in one alternative")
        cursor.index = declared.end
    }

    private fun modifiers(start: Int) {
        cursor.index++
        val added = flags()
        val hasRemoval = cursor.accept("-")
        val removed = flags()
        val all = added + removed
        val isDistinct = all.toSet().size == all.length
        val isValid = cursor.accept(":") && all.isNotEmpty() && isDistinct && (hasRemoval || removed.isEmpty())
        if (!isValid) cursor.fail(start, "a group starts with an invalid (? sequence")
        cursor.record(PatternRefusal.Flags, start, "a modifier group changes flags, which patterns do not support")
    }

    private fun flags(): String {
        val start = cursor.index
        while (!cursor.isAtEnd && cursor.current in MODIFIER_FLAGS) cursor.index++
        return cursor.source.substring(startIndex = start, endIndex = cursor.index)
    }

    private fun escape(): PatternNode? {
        if (!cursor.isAt('\\')) return null
        val start = cursor.index
        cursor.index++
        if (cursor.isAtEnd) cursor.fail(start, "a \\ ends the pattern")
        val set = escapes.reference(start) ?: escapes.escapeSet(start)?.let { PatternNode.Characters(it) }
        return set ?: PatternNode.Characters(CodePointSet.single(escapes.characterEscape(start)))
    }

    private fun enter(start: Int) {
        depth++
        if (depth > maximumNesting) {
            throw PatternFailure(PatternIssue(PatternRefusal.TooDeep, start, "the groups nest deeper than allowed"))
        }
    }

    private fun close(start: Int) {
        if (!cursor.accept(")")) cursor.fail(start, "a ( opens a group that is never closed")
        depth--
    }

    private companion object {
        val ANCHORS: List<Pair<String, AnchorKind>> = listOf(
            "^" to AnchorKind.INPUT_START,
            "$" to AnchorKind.INPUT_END,
            "\\b" to AnchorKind.WORD_BOUNDARY,
            "\\B" to AnchorKind.NOT_WORD_BOUNDARY,
        )

        val LOOKAROUNDS: List<String> = listOf("(?=", "(?!", "(?<=", "(?<!")

        val SIMPLE_QUANTIFIERS: List<Pair<String, Pair<Int, Int?>>> = listOf(
            "*" to (0 to null),
            "+" to (1 to null),
            "?" to (0 to 1),
        )

        fun count(digits: String): Int {
            val significant = digits.trimStart('0')
            val isHuge = significant.length > MAXIMUM_COUNT_DIGITS
            val text = significant.ifEmpty { "0" }
            val value = text.takeUnless { isHuge }?.toInt() ?: COUNT_CEILING
            return value.coerceAtMost(COUNT_CEILING)
        }

        fun compareCounts(smaller: String, larger: String): Int {
            val first = smaller.trimStart('0')
            val second = larger.trimStart('0')
            val byLength = first.length.compareTo(second.length)
            return byLength.takeIf { it != 0 } ?: first.compareTo(second)
        }
    }
}
