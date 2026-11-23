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
import com.davils.kore.core.text.SafeText

private const val SHOWN_SOURCE_LENGTH: Int = 64

/**
 * A regular expression of ECMA-262, as the keywords `pattern` and `patternProperties` of JSON Schema and OpenAPI use
 * them, compiled for a matcher that runs in time linear in the length of the input.
 *
 * [compile] reads the source with a hand-written parser of the ECMA-262 grammar in Unicode mode (the `u` flag) and
 * without any other flag, and compiles it to a nondeterministic automaton that a Pike VM runs breadth first. Matching
 * takes at most the number of states times the length of the input in steps, never backtracks and needs memory linear
 * in the number of states, so no input can make it catastrophically slow; `(a+)+$` takes as long as `a+$`. The engine
 * answers whether a pattern matches, not where: it reports no captures, so a lazy quantifier matches like a greedy
 * one.
 *
 * Supported are literals and the escapes `\t \n \v \f \r \0 \cX \xHH \uHHHH \u{H…}` and escaped syntax characters,
 * `.` (every code point but a line terminator), classes with ranges and negation, the class escapes `\d \D \w \W \s
 * \S` with their ECMA-262 definitions (`\d` and `\w` are ASCII only), the anchors `^` and `$` without multiline mode,
 * the word boundaries `\b` and `\B`, capturing, non-capturing and named groups, alternation, the quantifiers
 * `* + ? {n} {n,} {n,m}` and their lazy forms, and the property escapes `\p{…}` and `\P{…}` for every general
 * category and its aliases (`L`, `Letter`, `gc=Lu`, `General_Category=Decimal_Number`, `digit`, …), every script with
 * `Script=` or `sc=` and its long name or code (`Script=Greek`, `sc=Grek`), and the binary properties `Any`, `ASCII`,
 * `Assigned`, `ASCII_Hex_Digit`, `Hex_Digit`, `White_Space`, `Alphabetic`, `Lowercase`, `Uppercase` and
 * `Ideographic`, all on the tables of Unicode 16.0. Matching compares code points case-sensitively, and a surrogate
 * pair counts as one code point, in the source and in the input alike; a lone surrogate is a code point of its own.
 *
 * Refused with a [PatternRefusal] and the offset of the construct are back references, lookarounds, modifier groups
 * such as `(?i:…)`, other properties such as `Script_Extensions` or `Emoji`, and every syntax error, such as an
 * unbalanced parenthesis, a range like `[z-a]`, a dangling quantifier, a lone `]` or `}`, or an escape Unicode mode
 * does not allow, such as `\a` or `\-` outside a class. Two groups may share a name only in different alternatives,
 * as ECMAScript 2025 allows. Quantifier counts are compared exactly, so `a{99999999999,99999999998}` is a syntax
 * error, and counts above 10⁹ are treated as 10⁹, which [PatternLimits.maximumStates] refuses anyway.
 *
 * A pattern is immutable and safe to share between threads; every match allocates its own state.
 *
 * @property source the source the pattern was compiled from, unchanged.
 * @property limits the limits the pattern was compiled with, which also bound the inputs of its matches.
 * @since 1.0.0
 */
public class Pattern private constructor(
    public val source: String,
    public val limits: PatternLimits,
    private val program: Program,
) {
    private val identity: List<Any> get() = listOf(source, limits)

    /**
     * Returns whether the pattern matches anywhere in [input], as `RegExp.prototype.test` of ECMA-262 does: a pattern
     * without anchors matches if any part of [input] matches it.
     *
     * The work is at most the number of states times the length of [input].
     *
     * @param input the text to search, which may come from an attacker.
     * @return `true` if some part of [input] matches.
     * @throws IllegalArgumentException if [input] is longer than [PatternLimits.maximumInputLength] code units.
     * @since 1.0.0
     */
    public fun containsMatchIn(input: CharSequence): Boolean = matcher(input, isSearch = true).run()

    /**
     * Returns whether the pattern matches [input] from its first to its last code point, as if the pattern were
     * written `^(?:…)$`.
     *
     * The work is at most the number of states times the length of [input].
     *
     * @param input the text to match, which may come from an attacker.
     * @return `true` if the whole of [input] matches.
     * @throws IllegalArgumentException if [input] is longer than [PatternLimits.maximumInputLength] code units.
     * @since 1.0.0
     */
    public fun matchesEntirely(input: CharSequence): Boolean = matcher(input, isSearch = false).run()

    private fun matcher(input: CharSequence, isSearch: Boolean): Matcher {
        require(input.length <= limits.maximumInputLength) {
            "The input of a pattern may have at most ${limits.maximumInputLength} code units, but had ${input.length}"
        }
        return Matcher(program, input, isSearch)
    }

    override fun equals(other: Any?): Boolean = other is Pattern && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String = "Pattern(${SafeText.quoted(source, SHOWN_SOURCE_LENGTH)})"

    /**
     * Compiles patterns.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Compiles [source] as a regular expression of ECMA-262 in Unicode mode, within [limits].
         *
         * The source is checked against [PatternLimits.maximumSourceLength] before it is read, against
         * [PatternLimits.maximumNesting] while it is read and against [PatternLimits.maximumStates] before the
         * automaton is built, so the work and the memory of a compilation stay bounded by the limits. The whole source
         * is read before a construct the engine does not support is refused, so a syntax error anywhere wins over it.
         * This function never throws for any source and is safe to call from any thread.
         *
         * @param source the regular expression, without delimiters and flags; it may come from an attacker.
         * @param limits the limits to compile and later match within; [PatternLimits.Default] by default.
         * @return the [PatternCompilation.Compiled] pattern, or a [PatternCompilation.Refused] source with its reason,
         * the offset of the refused construct and a message that shows the source through `SafeText`.
         * @since 1.0.0
         */
        public fun compile(source: String, limits: PatternLimits = PatternLimits.Default): PatternCompilation {
            val result = PatternReading.read(source, limits)
            val node = result.node ?: return refused(source, result.issue)
            return PatternCompilation.Compiled(Pattern(source, limits, ProgramCompiler.compile(node)))
        }

        private fun refused(source: String, issue: PatternIssue?): PatternCompilation.Refused {
            val known = issue ?: PatternIssue(PatternRefusal.Syntax, 0, "the pattern could not be read")
            val message = "The pattern ${SafeText.quoted(source, SHOWN_SOURCE_LENGTH)} is refused at offset " +
                "${known.offset}: ${known.detail}"
            return PatternCompilation.Refused(known.reason, known.offset, message)
        }
    }
}
