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

package com.davils.kapi.runtime.core.conditional

import com.davils.kapi.runtime.core.conditional.internal.Calendar
import com.davils.kapi.runtime.core.conditional.internal.DateReader
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.jvm.JvmInline

private const val SECONDS_PER_DAY: Long = 86_400L

private const val SECONDS_PER_HOUR: Long = 3_600L

private const val SECONDS_PER_MINUTE: Long = 60L

private const val FIRST_SECOND: Long = -62_135_596_800L

private const val LAST_SECOND: Long = 253_402_300_799L

private const val TWO_DIGITS: Int = 2

private const val YEAR_DIGITS: Int = 4

/**
 * An HTTP-date of RFC 9110 §5.6.7: a moment in whole seconds, written as an IMF-fixdate such as
 * `Sun, 06 Nov 1994 08:49:37 GMT`.
 *
 * It is read from the IMF-fixdate and from the two obsolete formats every recipient must accept: the rfc850-date
 * `Sunday, 06-Nov-94 08:49:37 GMT` and the asctime date `Sun Nov  6 08:49:37 1994`. A day name that does not belong
 * to the date, a day the month does not have and an hour, a minute or a second out of range are refused, as is text
 * longer than 40 characters. A date lies between the years 1 and 9999.
 *
 * @property epochSeconds the seconds since 1970-01-01T00:00:00Z.
 * @since 1.0.0
 */
@JvmInline
public value class HttpDate(public val epochSeconds: Long) : Comparable<HttpDate> {
    init {
        require(epochSeconds in FIRST_SECOND..LAST_SECOND) { "An HTTP-date lies between the years 1 and 9999" }
    }

    /**
     * The year of the date, from 1 to 9999.
     *
     * @since 1.0.0
     */
    public val year: Int get() = Calendar.civilFromDays(epochSeconds.floorDiv(SECONDS_PER_DAY)).first

    override fun compareTo(other: HttpDate): Int = epochSeconds.compareTo(other.epochSeconds)

    override fun toString(): String {
        val days = epochSeconds.floorDiv(SECONDS_PER_DAY)
        val (year, month, day) = Calendar.civilFromDays(days)
        val secondOfDay = epochSeconds.mod(SECONDS_PER_DAY)
        val hour = two(secondOfDay / SECONDS_PER_HOUR)
        val minute = two(secondOfDay % SECONDS_PER_HOUR / SECONDS_PER_MINUTE)
        val second = two(secondOfDay % SECONDS_PER_MINUTE)
        val name = Calendar.DAY_NAMES[Calendar.weekday(days)]
        val shownYear = year.toString().padStart(YEAR_DIGITS, '0')
        val date = "${two(day.toLong())} ${Calendar.MONTH_NAMES[month - 1]} $shownYear"
        return "$name, $date $hour:$minute:$second GMT"
    }

    private fun two(value: Long): String = value.toString().padStart(TWO_DIGITS, '0')

    /**
     * Reads HTTP-dates.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads [text] as an HTTP-date in one of the three formats of RFC 9110 §5.6.7.
         *
         * An rfc850-date carries two digits of its year, which RFC 9110 reads relative to the present: it is read
         * only when [now] is given, as the year with these digits that lies at most 50 years after [now]. Without
         * [now] such a date is refused rather than guessed.
         *
         * @param text the text, which may come from an attacker.
         * @param now the present, to read a two-digit year; `null` to refuse every rfc850-date.
         * @return the date, or `null` if [text] is no HTTP-date.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String, now: HttpDate? = null): HttpDate? {
            val seconds = DateReader(text, now?.year).read()
            return seconds?.takeIf { value -> value <= LAST_SECOND }?.let(::HttpDate)
        }

        /**
         * Reads [text] like [parseOrNull].
         *
         * @param text the text.
         * @param now the present, to read a two-digit year.
         * @return the date.
         * @throws IllegalArgumentException if [text] is no HTTP-date.
         * @since 1.0.0
         */
        public fun parse(text: String, now: HttpDate? = null): HttpDate =
            requireNotNull(parseOrNull(text, now)) { "${SafeText.quoted(text)} is no HTTP-date" }
    }
}
