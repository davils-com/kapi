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

package com.davils.kapi.spec.schema.internal

private const val DATE_LENGTH: Int = 10

private const val YEAR_END: Int = 4

private const val MONTH_START: Int = 5

private const val MONTH_END: Int = 7

private const val DAY_START: Int = 8

private const val MONTHS: Int = 12

private const val FEBRUARY: Int = 2

private const val LEAP_FEBRUARY: Int = 29

private const val HOURS: Int = 23

private const val MINUTES: Int = 59

private const val LEAP_SECOND: Int = 60

private const val MINUTES_PER_HOUR: Int = 60

private const val MINUTES_PER_DAY: Int = 1440

private const val CLOCK_LENGTH: Int = 8

private const val OFFSET_LENGTH: Int = 6

private const val OCTETS: Int = 4

private const val OCTET_DIGITS: Int = 3

private const val OCTET_MAXIMUM: Int = 255

private const val UUID_LENGTH: Int = 36

private const val LOCAL_PART_LENGTH: Int = 64

private const val LABEL_LENGTH: Int = 63

private const val DOMAIN_LENGTH: Int = 253

private const val LEAP_CYCLE: Int = 400

private const val CENTURY: Int = 100

private const val LEAP_YEAR: Int = 4

private val UUID_HYPHENS: Set<Int> = setOf(8, 13, 18, 23)

private val DAYS: IntArray = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

private const val ATOM_SYMBOLS: String = "!#$%&'*+-/=?^_`{|}~"

internal object FormatBridge {
    private val checks: Map<String, (String) -> Boolean> = mapOf(
        "date" to ::isDate,
        "date-time" to ::isDateTime,
        "ipv4" to ::isIpv4,
        "uuid" to ::isUuid,
        "email" to ::isEmail,
    )

    fun check(format: String, value: String): Boolean? = checks[format]?.invoke(value)

    private fun isDate(text: String): Boolean {
        val isShaped = text.length == DATE_LENGTH && text[YEAR_END] == '-' && text[MONTH_END] == '-'
        val year = number(text, 0, YEAR_END).takeIf { isShaped }
        val month = number(text, MONTH_START, MONTH_END) ?: 0
        val day = number(text, DAY_START, DATE_LENGTH) ?: 0
        return year != null && month in 1..MONTHS && day in 1..daysOf(year, month)
    }

    private fun isDateTime(text: String): Boolean {
        val separator = text.getOrNull(DATE_LENGTH)
        val hasSeparator = separator == 'T' || separator == 't'
        return hasSeparator && isDate(text.substring(0, DATE_LENGTH)) && isTime(text.substring(DATE_LENGTH + 1))
    }

    private fun isTime(text: String): Boolean {
        val clock = clockOf(text.take(CLOCK_LENGTH)) ?: return false
        val rest = text.drop(CLOCK_LENGTH)
        val fractionEnd = fractionEnd(rest)
        val zone = rest.substring(fractionEnd.coerceAtLeast(0)).takeIf { fractionEnd >= 0 }
        val offset = zone?.let(::offsetOf)
        val second = number(text, 6, CLOCK_LENGTH) ?: 0
        val minute = (clock - second) / MINUTES_PER_HOUR
        val utcMinute = offset?.let { (minute - it).mod(MINUTES_PER_DAY) }
        val isLeapAllowed = second < LEAP_SECOND || utcMinute == HOURS * MINUTES_PER_HOUR + MINUTES
        return utcMinute != null && isLeapAllowed
    }

    private fun clockOf(text: String): Int? {
        val isShaped = text.length == CLOCK_LENGTH && text[2] == ':' && text[5] == ':'
        val hour = number(text, 0, 2).takeIf { isShaped } ?: return null
        val minute = number(text, 3, 5) ?: -1
        val second = number(text, 6, CLOCK_LENGTH) ?: -1
        val isValid = hour <= HOURS && minute in 0..MINUTES && second in 0..LEAP_SECOND
        return ((hour * MINUTES_PER_HOUR + minute) * MINUTES_PER_HOUR + second).takeIf { isValid }
    }

    private fun fractionEnd(text: String): Int {
        if (!text.startsWith('.')) return 0
        val digits = text.drop(1).takeWhile { it in '0'..'9' }
        return (digits.length + 1).takeIf { digits.isNotEmpty() } ?: -1
    }

    private fun offsetOf(text: String): Int? {
        if (text == "Z" || text == "z") return 0
        val sign = mapOf('+' to 1, '-' to -1)[text.firstOrNull()]
        val isShaped = text.length == OFFSET_LENGTH && text[3] == ':' && sign != null
        val hour = number(text, 1, 3).takeIf { isShaped }
        val minute = number(text, 4, OFFSET_LENGTH) ?: -1
        val isValid = hour != null && hour <= HOURS && minute in 0..MINUTES && sign != null
        return hour?.let { (it * MINUTES_PER_HOUR + minute) * (sign ?: 1) }?.takeIf { isValid }
    }

    private fun isIpv4(text: String): Boolean {
        val octets = text.split('.')
        return octets.size == OCTETS && octets.all(::isOctet)
    }

    private fun isOctet(text: String): Boolean {
        val isShaped = text.length in 1..OCTET_DIGITS && text.all { it in '0'..'9' }
        val hasLeadingZero = text.length > 1 && text.startsWith('0')
        val value = text.takeIf { isShaped && !hasLeadingZero }?.toInt()
        return value != null && value <= OCTET_MAXIMUM
    }

    private fun isUuid(text: String): Boolean {
        val isShaped = text.length == UUID_LENGTH
        return isShaped && text.indices.all { index -> isUuidCharacter(text[index], index in UUID_HYPHENS) }
    }

    private fun isUuidCharacter(character: Char, isHyphen: Boolean): Boolean {
        val isHex = character in '0'..'9' || character in 'a'..'f' || character in 'A'..'F'
        return (isHyphen && character == '-') || (!isHyphen && isHex)
    }

    private fun isEmail(text: String): Boolean {
        val at = text.lastIndexOf('@')
        val local = text.substring(0, at.coerceAtLeast(0))
        val domain = text.substring(at + 1)
        return at > 0 && isLocalPart(local) && isDomain(domain)
    }

    private fun isLocalPart(text: String): Boolean {
        val atoms = text.split('.')
        val isAtomic = atoms.all { atom -> atom.isNotEmpty() && atom.all(::isAtomCharacter) }
        return text.length <= LOCAL_PART_LENGTH && isAtomic
    }

    private fun isAtomCharacter(character: Char): Boolean =
        character in 'a'..'z' || character in 'A'..'Z' || character in '0'..'9' || character in ATOM_SYMBOLS

    private fun isDomain(text: String): Boolean {
        val isLiteral = text.startsWith('[') && text.endsWith(']')
        if (isLiteral) return isIpv4(text.substring(1, text.length - 1))
        val labels = text.split('.')
        return text.length <= DOMAIN_LENGTH && labels.all(::isLabel)
    }

    private fun isLabel(text: String): Boolean {
        val isShaped = text.length in 1..LABEL_LENGTH && !text.startsWith('-') && !text.endsWith('-')
        return isShaped && text.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '-' }
    }

    private fun number(text: String, start: Int, end: Int): Int? {
        val part = text.substring(start.coerceAtMost(text.length), end.coerceAtMost(text.length))
        val isDigits = part.length == end - start && part.all { it in '0'..'9' }
        return part.takeIf { isDigits }?.toInt()
    }

    private fun daysOf(year: Int, month: Int): Int {
        val isLeap = year % LEAP_CYCLE == 0 || (year % LEAP_YEAR == 0 && year % CENTURY != 0)
        val leapDays = LEAP_FEBRUARY.takeIf { isLeap && month == FEBRUARY }
        return leapDays ?: DAYS[month - 1]
    }
}
