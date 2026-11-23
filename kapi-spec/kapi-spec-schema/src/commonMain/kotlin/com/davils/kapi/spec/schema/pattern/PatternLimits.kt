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

import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.ViolationCode
import kotlin.reflect.KProperty0

/**
 * The limits a [Pattern] is compiled and matched within, built with [patternLimits].
 *
 * Every limit has a hard ceiling in [PatternLimitsBuilder]. A source or an input beyond a limit is refused, never
 * truncated: [Pattern.compile] refuses a source that is too long, nests too deep or needs too many states, and the
 * match functions of [Pattern] throw for an input that is too long. Matching takes time proportional to
 * [maximumStates] times the length of the input at worst, whatever the pattern and the input are.
 *
 * Limits are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class PatternLimits internal constructor(builder: PatternLimitsBuilder) : Validatable {
    /**
     * The most UTF-16 code units of a source.
     *
     * @since 1.0.0
     */
    public val maximumSourceLength: Int = builder.maximumSourceLength

    /**
     * The most states of the automaton of a source, with every counted repetition expanded.
     *
     * @since 1.0.0
     */
    public val maximumStates: Int = builder.maximumStates

    /**
     * The most UTF-16 code units of an input.
     *
     * @since 1.0.0
     */
    public val maximumInputLength: Int = builder.maximumInputLength

    /**
     * The deepest nesting of groups and lookarounds in a source.
     *
     * @since 1.0.0
     */
    public val maximumNesting: Int = builder.maximumNesting

    private val identity: List<Int>
        get() = listOf(maximumSourceLength, maximumStates, maximumInputLength, maximumNesting)

    override fun ValidationScope.validate() {
        within(::maximumSourceLength, PatternLimitsBuilder.HARD_SOURCE_LENGTH)
        within(::maximumStates, PatternLimitsBuilder.HARD_STATES)
        within(::maximumInputLength, PatternLimitsBuilder.HARD_INPUT_LENGTH)
        within(::maximumNesting, PatternLimitsBuilder.HARD_NESTING)
    }

    override fun equals(other: Any?): Boolean = other is PatternLimits && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String =
        "PatternLimits(source=$maximumSourceLength, states=$maximumStates, input=$maximumInputLength, " +
            "nesting=$maximumNesting)"

    private fun ValidationScope.within(property: KProperty0<Int>, ceiling: Int) {
        val value = property.get()
        if (value !in 1..ceiling) {
            fail(property, "must lie between 1 and $ceiling, but was $value", ViolationCode.OUT_OF_RANGE)
        }
    }

    /**
     * The default limits.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The limits with every default of [PatternLimitsBuilder].
         *
         * @since 1.0.0
         */
        public val Default: PatternLimits = PatternLimitsBuilder().build()

        internal val Ceilings: PatternLimits = patternLimits {
            maximumSourceLength = PatternLimitsBuilder.HARD_SOURCE_LENGTH
            maximumStates = PatternLimitsBuilder.HARD_STATES
            maximumInputLength = PatternLimitsBuilder.HARD_INPUT_LENGTH
            maximumNesting = PatternLimitsBuilder.HARD_NESTING
        }
    }
}
