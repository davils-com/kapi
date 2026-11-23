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

package com.davils.kapi.runtime.core.conditional.internal

import com.davils.kapi.runtime.core.internal.FieldGrammar

private const val MAX_LENGTH: Int = 40

private const val GMT: String = "GMT"

private const val COMMA: Char = ','

private const val SECONDS_PER_MINUTE: Long = 60L

private const val SECONDS_PER_HOUR: Long = 3_600L

private const val SECONDS_PER_DAY: Long = 86_400L

private const val LAST_HOUR: Int = 23

private const val LAST_MINUTE: Int = 59

private const val LAST_SECOND: Int = 60

private const val CENTURY: Int = 100

private const val HALF_CENTURY: Int = 50

private const val FIRST_YEAR: Int = 1

private const val LAST_YEAR: Int = 9_999

private const val IMF_TOKENS: Int = 6

private const val RFC850_TOKENS: Int = 4

private const val ASCTIME_TOKENS: Int = 5

private const val ASCTIME_PADDED_TOKENS: Int = 6

private const val TIME_PARTS: Int = 3

private const val FULL_YEAR_DIGITS: Int = 4

private const val TWO_DIGITS: Int = 2

internal class DateReader(private val text: String, private val nowYear: Int?) {
    private val tokens: List<String> = text.split(' ')

    fun read(): Long? {
        val isReadable = text.length <= MAX_LENGTH && text.all(FieldGrammar::isVisibleOrSpace)
        val parts = (imf() ?: rfc850() ?: asctime()).takeIf { isReadable }
        return parts?.let(::instant)
    }

    private fun imf(): DateParts? {
        val isShaped = tokens.size == IMF_TOKENS && tokens.last() == GMT && isNamed(tokens[0])
        if (!isShaped) return null
        return DateParts(
            dayName = tokens[0].dropLast(1),
            names = Calendar.DAY_NAMES,
            day = tokens[1].takeIf(::isTwoDigits),
            month = tokens[2],
            year = tokens[3].takeIf { year -> year.length == FULL_YEAR_DIGITS }?.let(::number),
            time = tokens[4],
        )
    }

    private fun rfc850(): DateParts? {
        val dateToken = tokens.getOrNull(1)
        val date = dateToken?.split('-').orEmpty()
        val isShaped = tokens.size == RFC850_TOKENS && tokens.last() == GMT && isNamed(tokens[0])
        if (!isShaped || date.size != TIME_PARTS) return null
        val year = date[2].takeIf(::isTwoDigits)?.let(::number)
        return DateParts(
            dayName = tokens[0].dropLast(1),
            names = Calendar.LONG_DAY_NAMES,
            day = date[0].takeIf(::isTwoDigits),
            month = date[1],
            year = year?.let(::fullYear),
            time = tokens[2],
        )
    }

    private fun asctime(): DateParts? {
        val isPadded = tokens.size == ASCTIME_PADDED_TOKENS && tokens[2].isEmpty() && tokens[3].length == 1
        val isTwoDigitDay = tokens.size == ASCTIME_TOKENS && isTwoDigits(tokens[2])
        if (!isPadded && !isTwoDigitDay) return null
        val shift = 1.takeIf { isPadded } ?: 0
        return DateParts(
            dayName = tokens[0],
            names = Calendar.DAY_NAMES,
            day = tokens[2 + shift],
            month = tokens[1],
            year = tokens[4 + shift].takeIf { year -> year.length == FULL_YEAR_DIGITS }?.let(::number),
            time = tokens[3 + shift],
        )
    }

    private fun isNamed(token: String): Boolean = token.length > 1 && token.last() == COMMA

    private fun isTwoDigits(text: String): Boolean = text.length == TWO_DIGITS

    private fun fullYear(twoDigits: Int): Int? {
        val now = nowYear ?: return null
        val candidate = now - now % CENTURY + twoDigits
        return candidate - (CENTURY.takeIf { candidate > now + HALF_CENTURY } ?: 0)
    }

    private fun instant(parts: DateParts): Long? {
        val day = parts.day?.let(::number)
        val month = Calendar.MONTH_NAMES.indexOf(parts.month) + 1
        val year = parts.year?.takeIf { found -> found in FIRST_YEAR..LAST_YEAR }
        val seconds = timeOf(parts.time)
        if (day == null || year == null || seconds == null) return null
        val days = Calendar.daysFromCivil(year, month, day)
        val isDay = month > 0 && day in 1..Calendar.monthLength(year, month)
        val isWeekday = parts.names.getOrNull(Calendar.weekday(days)) == parts.dayName
        return (days * SECONDS_PER_DAY + seconds).takeIf { isDay && isWeekday }
    }

    private fun timeOf(time: String): Long? {
        val parts = time.split(':').takeIf { found -> found.size == TIME_PARTS && found.all(::isTwoDigits) }
        val numbers = parts?.mapNotNull(::number).orEmpty()
        val isComplete = numbers.size == TIME_PARTS
        val hour = numbers.getOrNull(0)?.takeIf { value -> isComplete && value <= LAST_HOUR }
        val minute = numbers.getOrNull(1)?.takeIf { value -> value <= LAST_MINUTE }
        val second = numbers.getOrNull(2)?.takeIf { value -> value <= LAST_SECOND }
        if (hour == null || minute == null || second == null) return null
        return hour * SECONDS_PER_HOUR + minute * SECONDS_PER_MINUTE + second
    }

    private fun number(text: String): Int? {
        val isNumber = text.isNotEmpty() && text.length <= FULL_YEAR_DIGITS && text.all(FieldGrammar::isDigit)
        return text.takeIf { isNumber }?.toInt()
    }
}
