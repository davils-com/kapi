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

package com.davils.kapi.runtime.core.header

import com.davils.kapi.runtime.core.conditional.HttpDate
import com.davils.kapi.runtime.core.internal.FieldGrammar

private const val MOST_DIGITS: Int = 18

/**
 * The value of `Retry-After` of RFC 9110 §10.2.3: a delay in seconds or the HTTP-date after which to retry.
 *
 * A value is immutable; [toString] is the field value.
 *
 * @since 1.0.0
 */
public sealed interface RetryAfter {
    /**
     * Returns how many seconds to wait from [now].
     *
     * @param now the present.
     * @return the delay, never negative.
     * @since 1.0.0
     */
    public fun secondsFrom(now: HttpDate): Long

    /**
     * A delay in seconds.
     *
     * @property seconds the delay, zero or more.
     * @throws IllegalArgumentException if [seconds] is negative.
     * @since 1.0.0
     */
    public class Delay(public val seconds: Long) : RetryAfter {
        init {
            require(seconds >= 0) { "A delay is not negative" }
        }

        override fun secondsFrom(now: HttpDate): Long = seconds

        override fun equals(other: Any?): Boolean = other is Delay && seconds == other.seconds

        override fun hashCode(): Int = seconds.hashCode()

        override fun toString(): String = seconds.toString()
    }

    /**
     * An HTTP-date.
     *
     * @property date the date.
     * @since 1.0.0
     */
    public class Date(public val date: HttpDate) : RetryAfter {
        override fun secondsFrom(now: HttpDate): Long = maxOf(a = 0L, b = date.epochSeconds - now.epochSeconds)

        override fun equals(other: Any?): Boolean = other is Date && date == other.date

        override fun hashCode(): Int = date.hashCode()

        override fun toString(): String = date.toString()
    }

    /**
     * Reads `Retry-After`.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads the value of `Retry-After`.
         *
         * @param text the field value, which may come from an attacker.
         * @param now the present, to read an HTTP-date with a two-digit year.
         * @return the value, or `null` if [text] is neither up to 18 digits nor an HTTP-date.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String, now: HttpDate? = null): RetryAfter? {
            val trimmed = FieldGrammar.trimmed(text)
            val isDelay = trimmed.length in 1..MOST_DIGITS && trimmed.all(FieldGrammar::isDigit)
            val delay = trimmed.takeIf { isDelay }?.let { digits -> Delay(digits.toLong()) }
            return delay ?: HttpDate.parseOrNull(trimmed, now)?.let(::Date)
        }
    }
}
