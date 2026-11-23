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

private const val DAYS_PER_ERA: Long = 146_097L

private const val YEARS_PER_ERA: Long = 400L

private const val EPOCH_SHIFT: Long = 719_468L

private const val DAYS_PER_YEAR: Long = 365L

private const val MONTH_SPAN: Long = 153L

private const val MONTH_ROUNDING: Long = 2L

private const val MONTH_DIVISOR: Long = 5L

private const val FROM_MARCH: Long = 3L

private const val FROM_JANUARY: Long = 9L

private const val MONTHS_FROM_MARCH: Long = 10L

private const val FIRST_CYCLE: Long = 1_460L

private const val CENTURY_CYCLE: Long = 36_524L

private const val ERA_CYCLE: Long = 146_096L

private const val LEAP_CYCLE: Long = 4L

private const val CENTURY: Long = 100L

private const val WEEK: Long = 7L

private const val THURSDAY: Long = 4L

private const val FEBRUARY: Int = 2

private const val LONG_FEBRUARY: Int = 29

private val monthLengths: List<Int> = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

internal object Calendar {
    val DAY_NAMES: List<String> = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val LONG_DAY_NAMES: List<String> =
        listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    val MONTH_NAMES: List<String> =
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val isEarly = month <= FEBRUARY
        val shiftedYear = year.toLong() - (1L.takeIf { isEarly } ?: 0L)
        val shiftedMonth = month + (FROM_JANUARY.takeIf { isEarly } ?: -FROM_MARCH)
        val era = shiftedYear / YEARS_PER_ERA
        val yearOfEra = shiftedYear - era * YEARS_PER_ERA
        val dayOfYear = (MONTH_SPAN * shiftedMonth + MONTH_ROUNDING) / MONTH_DIVISOR + day - 1
        val dayOfEra = yearOfEra * DAYS_PER_YEAR + yearOfEra / LEAP_CYCLE - yearOfEra / CENTURY + dayOfYear
        return era * DAYS_PER_ERA + dayOfEra - EPOCH_SHIFT
    }

    fun civilFromDays(days: Long): Triple<Int, Int, Int> {
        val shifted = days + EPOCH_SHIFT
        val era = shifted / DAYS_PER_ERA
        val dayOfEra = shifted - era * DAYS_PER_ERA
        val cycles = dayOfEra - dayOfEra / FIRST_CYCLE + dayOfEra / CENTURY_CYCLE - dayOfEra / ERA_CYCLE
        val yearOfEra = cycles / DAYS_PER_YEAR
        val dayOfYear = dayOfEra - (DAYS_PER_YEAR * yearOfEra + yearOfEra / LEAP_CYCLE - yearOfEra / CENTURY)
        val shiftedMonth = (MONTH_DIVISOR * dayOfYear + MONTH_ROUNDING) / MONTH_SPAN
        val day = dayOfYear - (MONTH_SPAN * shiftedMonth + MONTH_ROUNDING) / MONTH_DIVISOR + 1
        val month = (shiftedMonth + FROM_MARCH).takeIf { shiftedMonth < MONTHS_FROM_MARCH }
            ?: (shiftedMonth - FROM_JANUARY)
        val year = yearOfEra + era * YEARS_PER_ERA + (1L.takeIf { month <= FEBRUARY } ?: 0L)
        return Triple(first = year.toInt(), second = month.toInt(), third = day.toInt())
    }

    fun weekday(days: Long): Int = (days + THURSDAY).mod(WEEK).toInt()

    fun monthLength(year: Int, month: Int): Int {
        val isLeap = (year % LEAP_CYCLE == 0L && year % CENTURY != 0L) || year % YEARS_PER_ERA == 0L
        if (month == FEBRUARY && isLeap) return LONG_FEBRUARY
        return monthLengths[month - 1]
    }
}
