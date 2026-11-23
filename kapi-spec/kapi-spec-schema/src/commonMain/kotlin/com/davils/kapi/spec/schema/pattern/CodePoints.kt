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

private const val SUPPLEMENTARY_START: Int = 0x10000
private const val SURROGATE_BITS: Int = 10
private const val SURROGATE_MASK: Int = 0x3FF
private const val HIGH_SURROGATE_START: Int = 0xD800
private const val LOW_SURROGATE_START: Int = 0xDC00
private const val LOW_SURROGATE_END: Int = 0xDFFF
private const val HEX_RADIX: Int = 16
private const val DECIMAL_LETTER_OFFSET: Int = 10
private const val FOUR_DIGITS: Int = 4
private const val MAXIMUM_BRACED_DIGITS: Int = 8

internal fun codePointAt(text: CharSequence, index: Int): Int {
    val high = text[index]
    val hasPair = high.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()
    if (!hasPair) return high.code
    return combine(high = high.code, low = text[index + 1].code)
}

internal fun charCount(codePoint: Int): Int = 2.takeIf { codePoint >= SUPPLEMENTARY_START } ?: 1

internal fun combine(high: Int, low: Int): Int {
    val highBits = high - HIGH_SURROGATE_START shl SURROGATE_BITS
    return SUPPLEMENTARY_START + highBits + (low - LOW_SURROGATE_START)
}

internal fun StringBuilder.appendCodePoint(codePoint: Int): StringBuilder {
    val isSupplementary = codePoint >= SUPPLEMENTARY_START
    if (!isSupplementary) return append(codePoint.toChar())
    val offset = codePoint - SUPPLEMENTARY_START
    append((HIGH_SURROGATE_START + (offset shr SURROGATE_BITS)).toChar())
    return append((LOW_SURROGATE_START + (offset and SURROGATE_MASK)).toChar())
}

internal fun isHighSurrogate(value: Int): Boolean = value in HIGH_SURROGATE_START until LOW_SURROGATE_START

internal fun isLowSurrogate(value: Int): Boolean = value in LOW_SURROGATE_START..LOW_SURROGATE_END

internal fun hexValue(char: Char): Int {
    val decimal = (char - '0').takeIf { char in '0'..'9' }
    val lower = (char - 'a' + DECIMAL_LETTER_OFFSET).takeIf { char in 'a'..'f' }
    val upper = (char - 'A' + DECIMAL_LETTER_OFFSET).takeIf { char in 'A'..'F' }
    return decimal ?: lower ?: upper ?: -1
}

internal fun isAsciiDigit(char: Char): Boolean = char in '0'..'9'

internal fun isAsciiLetter(char: Char): Boolean = char in 'a'..'z' || char in 'A'..'Z'

internal fun readHex(text: String, start: Int, count: Int): Int? {
    val isLongEnough = start + count <= text.length
    if (!isLongEnough) return null
    var value = 0
    var isHex = true
    repeat(count) { offset ->
        val digit = hexValue(text[start + offset])
        isHex = isHex && digit >= 0
        value = value * HEX_RADIX + digit.coerceAtLeast(0)
    }
    return value.takeIf { isHex }
}

internal fun readUnicodeEscape(text: String, start: Int): Scanned<Int>? {
    val isBraced = start + 1 < text.length && text[start + 1] == '{'
    if (isBraced) return readBracedEscape(text, start + 2)
    return readFourDigitEscape(text, start)
}

private fun readFourDigitEscape(text: String, start: Int): Scanned<Int>? {
    val lead = readHex(text, start = start + 1, count = FOUR_DIGITS) ?: return null
    val afterLead = start + 1 + FOUR_DIGITS
    val trail = readTrail(text, afterLead).takeIf { isHighSurrogate(lead) }
    val combined = trail?.let { low -> Scanned(combine(high = lead, low = low), end = afterLead + 2 + FOUR_DIGITS) }
    return combined ?: Scanned(lead, end = afterLead)
}

private fun readTrail(text: String, start: Int): Int? {
    val hasEscape = text.startsWith("\\u", start)
    val trail = readHex(text, start = start + 2, count = FOUR_DIGITS).takeIf { hasEscape }
    return trail?.takeIf(::isLowSurrogate)
}

private fun readBracedEscape(text: String, start: Int): Scanned<Int>? {
    var index = start
    while (index < text.length && text[index] == '0') index++
    val significant = index
    while (index < text.length && hexValue(text[index]) >= 0) index++
    val digits = index - significant
    val isClosed = index < text.length && text[index] == '}' && index > start
    val isBounded = isClosed && digits <= MAXIMUM_BRACED_DIGITS
    val value = readHex(text, start = significant, count = digits).takeIf { isBounded }
    val codePoint = value?.takeIf { it <= LAST_CODE_POINT }
    return codePoint?.let { Scanned(it, end = index + 1) }
}
