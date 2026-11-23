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

package com.davils.kapi.spec.schema.format

private const val DATE_LENGTH: Int = 10
private const val FIELDS: Int = 3
private const val YEAR_DIGITS: Int = 4
private const val MONTH_START: Int = 5
private const val DAY_START: Int = 8
private const val MINUTE_START: Int = 3
private const val SECOND_START: Int = 6
private const val CLOCK_LENGTH: Int = 8
private const val OFFSET_LENGTH: Int = 6
private const val MONTHS: Int = 12
private const val LAST_HOUR: Int = 23
private const val LAST_MINUTE: Int = 59
private const val LEAP_SECOND: Int = 60
private const val MINUTES_PER_HOUR: Int = 60
private const val MINUTES_PER_DAY: Int = 24 * 60
private const val LAST_MINUTE_OF_DAY: Int = MINUTES_PER_DAY - 1
private const val FEBRUARY: Int = 2
private const val LEAP_DAYS: Int = 29
private const val LEAP_CYCLE: Int = 4
private const val CENTURY: Int = 100
private const val LEAP_CENTURY: Int = 400
private const val DAYS_PER_MONTH: String = "312831303130313130313031"
private const val DURATION_DATES: String = " D M MD Y YM YMD "
private const val DURATION_TIMES: String = " H HM HMS M MS S "
private const val DURATION_DESIGNATORS: String = "YMWDHS"

internal object DateTimeFormats {
    fun date(value: String): String? {
        val year = number(value, start = 0, count = YEAR_DIGITS)
        val month = number(value, start = MONTH_START, count = 2)
        val day = number(value, start = DAY_START, count = 2)
        val hasDashes = value.length == DATE_LENGTH && value[YEAR_DIGITS] == '-' && value[MONTH_START + 2] == '-'
        val fields = listOfNotNull(year, month, day).takeIf { hasDashes && it.size == FIELDS }
        val (yearValue, monthValue, dayValue) = fields ?: return "a date has the form YYYY-MM-DD in ASCII digits"
        return monthReason(monthValue) ?: dayReason(yearValue, month = monthValue, day = dayValue)
    }

    fun time(value: String): String? {
        val clock = clockReason(value) ?: fractionReason(value)
        return clock ?: offsetReason(value) ?: leapSecondReason(value)
    }

    fun dateTime(value: String): String? {
        val separator = value.getOrNull(DATE_LENGTH)
        val hasSeparator = separator == 'T' || separator == 't'
        if (!hasSeparator) return "a date-time joins its date and its time with T"
        return date(value.substring(startIndex = 0, endIndex = DATE_LENGTH)) ?: time(value.substring(DATE_LENGTH + 1))
    }

    fun duration(value: String): String? {
        val parts = durationParts(value) ?: return "a duration has the form PnYnMnD, PTnHnMnS or PnW in ASCII digits"
        val (date, time) = parts
        val isWeek = date == "W" && time == null
        val isDatePart = date.isEmpty() || " $date " in DURATION_DATES
        val isTimePart = time == null || (time.isNotEmpty() && " $time " in DURATION_TIMES)
        val hasElement = date.isNotEmpty() || !time.isNullOrEmpty()
        val isCombined = isDatePart && isTimePart && hasElement
        return "the elements of the duration are missing or out of order".takeUnless { isWeek || isCombined }
    }

    private fun durationParts(value: String): Pair<String, String?>? {
        if (!value.startsWith('P')) return null
        val date = StringBuilder()
        var time: StringBuilder? = null
        var index = 1
        var isValid = true
        while (isValid && index < value.length) {
            val isSeparator = value[index] == 'T' && time == null
            if (isSeparator) {
                time = StringBuilder()
                index++
            }
            if (!isSeparator) {
                var end = index
                while (end < value.length && isDigit(value[end])) end++
                val designator = value.getOrNull(end) ?: ' '
                isValid = end > index && designator in DURATION_DESIGNATORS
                (time ?: date).append(designator)
                index = end + 1
            }
        }
        return (date.toString() to time?.toString()).takeIf { isValid }
    }

    private fun monthReason(month: Int): String? =
        "the month of a date lies between 01 and 12".takeUnless { month in 1..MONTHS }

    private fun dayReason(year: Int, month: Int, day: Int): String? {
        val isLeapYear = year % LEAP_CYCLE == 0 && (year % CENTURY != 0 || year % LEAP_CENTURY == 0)
        val usual = number(DAYS_PER_MONTH, start = (month - 1) * 2, count = 2) ?: 0
        val days = LEAP_DAYS.takeIf { isLeapYear && month == FEBRUARY } ?: usual
        return "the day does not exist in its month".takeUnless { day in 1..days }
    }

    private fun clockReason(value: String): String? {
        val hour = number(value, start = 0, count = 2)
        val minute = number(value, start = MINUTE_START, count = 2)
        val second = number(value, start = SECOND_START, count = 2)
        val hasColons = value.getOrNull(2) == ':' && value.getOrNull(MINUTE_START + 2) == ':'
        val fields = listOfNotNull(hour, minute, second).takeIf { hasColons && it.size == FIELDS }
        val (hourValue, minuteValue, secondValue) = fields
            ?: return "a time has the form HH:MM:SS followed by an offset, in ASCII digits"
        val isInRange = hourValue <= LAST_HOUR && minuteValue <= LAST_MINUTE && secondValue <= LEAP_SECOND
        return "the hour, minute or second of the time is out of range".takeUnless { isInRange }
    }

    private fun fractionReason(value: String): String? =
        "a fraction of a second needs at least one digit".takeIf { offsetStart(value) == null }

    private fun offsetStart(value: String): Int? {
        val hasFraction = value.getOrNull(CLOCK_LENGTH) == '.'
        if (!hasFraction) return CLOCK_LENGTH
        var index = CLOCK_LENGTH + 1
        while (index < value.length && isDigit(value[index])) index++
        return index.takeIf { it > CLOCK_LENGTH + 1 }
    }

    private fun offsetReason(value: String): String? {
        val offset = value.substring(offsetStart(value) ?: value.length)
        val isZulu = offset == "Z" || offset == "z"
        val isOffset = offsetMinutes(offset) != null
        val isValid = isZulu || isOffset
        return "a time ends in Z or in an offset such as +01:00".takeUnless { isValid }
    }

    private fun offsetMinutes(offset: String): Int? {
        val sign = offset.firstOrNull()
        val hours = number(offset, start = 1, count = 2)
        val minutes = number(offset, start = 4, count = 2)
        val isShaped = offset.length == OFFSET_LENGTH && (sign == '+' || sign == '-') && offset[MINUTE_START] == ':'
        val isInRange = hours != null && minutes != null && hours <= LAST_HOUR && minutes <= LAST_MINUTE
        val total = ((hours ?: 0) * MINUTES_PER_HOUR + (minutes ?: 0)).takeIf { isShaped && isInRange }
        val factor = (-1).takeIf { sign == '-' } ?: 1
        return total?.times(factor)
    }

    private fun leapSecondReason(value: String): String? {
        val isLeapSecond = number(value, start = SECOND_START, count = 2) == LEAP_SECOND
        if (!isLeapSecond) return null
        val hour = number(value, start = 0, count = 2) ?: 0
        val minute = number(value, start = MINUTE_START, count = 2) ?: 0
        val local = hour * MINUTES_PER_HOUR + minute
        val offset = value.substring(offsetStart(value) ?: value.length)
        val universal = (local - (offsetMinutes(offset) ?: 0) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return "a leap second falls on 23:59:60 in UTC".takeUnless { universal == LAST_MINUTE_OF_DAY }
    }
}
