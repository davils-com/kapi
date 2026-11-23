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

package com.davils.kapi.runtime.core.pattern

/**
 * The result of [Pattern.compile]: a [Compiled] pattern or a [Refused] source.
 *
 * Both results are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public sealed interface PatternCompilation {
    /**
     * The source was compiled.
     *
     * @property pattern the compiled pattern, ready to match.
     * @since 1.0.0
     */
    public class Compiled(public val pattern: Pattern) : PatternCompilation {
        private val identity: List<Any> get() = listOf(pattern)

        override fun equals(other: Any?): Boolean = other is Compiled && identity == other.identity

        override fun hashCode(): Int = identity.hashCode()

        override fun toString(): String = "Compiled($pattern)"
    }

    /**
     * The source was refused.
     *
     * @property reason why the source was refused.
     * @property offset where the refused construct starts, in UTF-16 code units from the start of the source; for
     * [PatternRefusal.TooLong] the length that was allowed and for [PatternRefusal.TooManyStates] zero.
     * @property message a description for people, which shows the source escaped and shortened through `SafeText`, so
     * it is safe to log and to show.
     * @since 1.0.0
     */
    public class Refused(
        public val reason: PatternRefusal,
        public val offset: Int,
        public val message: String,
    ) : PatternCompilation {
        private val identity: List<Any> get() = listOf(reason, offset, message)

        override fun equals(other: Any?): Boolean = other is Refused && identity == other.identity

        override fun hashCode(): Int = identity.hashCode()

        override fun toString(): String = "Refused(reason=$reason, offset=$offset, message=$message)"
    }
}
