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

/**
 * Why [Pattern.compile] refused a source.
 *
 * A refusal either means that the source is no regular expression of ECMA-262 at all ([Syntax]), that it is one but
 * uses a construct the linear engine of kapi cannot run ([Backreference], [Lookaround], [Flags],
 * [UnsupportedProperty]), or that it exceeds a [PatternLimits] ([TooLong], [TooDeep], [TooManyStates]).
 * [isValidSyntax] tells these cases apart, so the `regex` format can accept what is valid ECMA-262 although kapi does
 * not run it.
 *
 * @property isValidSyntax `true` if the whole source was read and is a valid regular expression of ECMA-262 in
 * Unicode mode, so that only the engine or a limit refused it; `false` if the source is invalid or was not read in full
 * because a limit stopped the reading first.
 * @since 1.0.0
 */
public enum class PatternRefusal(public val isValidSyntax: Boolean) {
    /**
     * The source breaks the grammar of ECMA-262 in Unicode mode, such as an unbalanced parenthesis, a range like
     * `[z-a]`, a quantifier without an atom, an escape Unicode mode does not allow or an unknown property.
     *
     * @since 1.0.0
     */
    Syntax(isValidSyntax = false),

    /**
     * The source refers back to a group with `\1` or `\k<name>`, which no linear-time engine can match.
     *
     * @since 1.0.0
     */
    Backreference(isValidSyntax = true),

    /**
     * The source asserts with a lookahead `(?=…)`, `(?!…)` or a lookbehind `(?<=…)`, `(?<!…)`, which the engine does
     * not support.
     *
     * @since 1.0.0
     */
    Lookaround(isValidSyntax = true),

    /**
     * The source changes flags inside a group with a modifier such as `(?i:…)`; patterns of JSON Schema run without
     * flags, so the engine supports none.
     *
     * @since 1.0.0
     */
    Flags(isValidSyntax = true),

    /**
     * The source names a Unicode property of ECMA-262 the engine has no table for, such as `\p{Emoji}` or
     * `\p{Script_Extensions=Latin}`.
     *
     * @since 1.0.0
     */
    UnsupportedProperty(isValidSyntax = true),

    /**
     * The source is longer than [PatternLimits.maximumSourceLength]; it was not read.
     *
     * @since 1.0.0
     */
    TooLong(isValidSyntax = false),

    /**
     * The groups of the source nest deeper than [PatternLimits.maximumNesting]; it was not read in full.
     *
     * @since 1.0.0
     */
    TooDeep(isValidSyntax = false),

    /**
     * The automaton of the source, with every counted repetition such as `a{1000}` expanded, would have more states
     * than [PatternLimits.maximumStates].
     *
     * @since 1.0.0
     */
    TooManyStates(isValidSyntax = true),
}
