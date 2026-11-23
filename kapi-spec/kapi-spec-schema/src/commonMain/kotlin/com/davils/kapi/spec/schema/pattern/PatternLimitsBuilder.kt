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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures [PatternLimits]; obtained through [patternLimits].
 *
 * Every limit starts at its default and must lie between 1 and its hard ceiling, which [build] and [validate] check
 * together, reporting every limit out of range at once. A builder is meant for one thread and one evaluation.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class PatternLimitsBuilder internal constructor() : ValidatingBuilder<PatternLimits>() {
    override val subject: String get() = "pattern limits"

    /**
     * The most UTF-16 code units a source may have, from 1 to [HARD_SOURCE_LENGTH]; [DEFAULT_SOURCE_LENGTH] by
     * default.
     *
     * @since 1.0.0
     */
    public var maximumSourceLength: Int = DEFAULT_SOURCE_LENGTH

    /**
     * The most states the automaton of a source may have after every counted repetition is expanded, from 1 to
     * [HARD_STATES]; [DEFAULT_STATES] by default. It bounds the memory of a pattern and the work per input character.
     *
     * @since 1.0.0
     */
    public var maximumStates: Int = DEFAULT_STATES

    /**
     * The most UTF-16 code units an input of a match may have, from 1 to [HARD_INPUT_LENGTH]; [DEFAULT_INPUT_LENGTH]
     * by default.
     *
     * @since 1.0.0
     */
    public var maximumInputLength: Int = DEFAULT_INPUT_LENGTH

    /**
     * The deepest the groups of a source may nest, from 1 to [HARD_NESTING]; [DEFAULT_NESTING] by default. It bounds
     * the stack the reader needs.
     *
     * @since 1.0.0
     */
    public var maximumNesting: Int = DEFAULT_NESTING

    override fun assemble(): PatternLimits = PatternLimits(this)

    /**
     * The defaults and the hard ceilings of the limits.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default of [maximumSourceLength]: 1024 code units.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_SOURCE_LENGTH: Int = 1024

        /**
         * The hard ceiling of [maximumSourceLength]: 65 536 code units.
         *
         * @since 1.0.0
         */
        public const val HARD_SOURCE_LENGTH: Int = 65_536

        /**
         * The default of [maximumStates]: 10 000 states.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_STATES: Int = 10_000

        /**
         * The hard ceiling of [maximumStates]: 1 000 000 states.
         *
         * @since 1.0.0
         */
        public const val HARD_STATES: Int = 1_000_000

        /**
         * The default of [maximumInputLength]: 1 MiB of code units.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_INPUT_LENGTH: Int = 1024 * 1024

        /**
         * The hard ceiling of [maximumInputLength]: 64 MiB of code units.
         *
         * @since 1.0.0
         */
        public const val HARD_INPUT_LENGTH: Int = 64 * 1024 * 1024

        /**
         * The default of [maximumNesting]: 64 levels of groups.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_NESTING: Int = 64

        /**
         * The hard ceiling of [maximumNesting]: 256 levels of groups.
         *
         * @since 1.0.0
         */
        public const val HARD_NESTING: Int = 256
    }
}
