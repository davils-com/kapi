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

package com.davils.kapi.spec.schema.format

import com.davils.kapi.runtime.core.pattern.UnicodeData
import com.davils.kapi.runtime.core.pattern.appendPoint
import com.davils.kore.core.annotation.DavilsInternal

private const val MAXIMUM_LABEL_LENGTH: Int = 63
private const val MAXIMUM_NAME_LENGTH: Int = 253
private const val MAXIMUM_INPUT_LENGTH: Int = MAXIMUM_NAME_LENGTH * 2
private const val HYPHEN_POSITION: Int = 2
private const val ACE_PREFIX: String = "xn--"
private const val LAST_ASCII: Int = 0x7F
private const val FULLWIDTH_START: Int = 0xFF01
private const val FULLWIDTH_END: Int = 0xFF5E
private const val FULLWIDTH_OFFSET: Int = 0xFEE0
private const val IDEOGRAPHIC_FULL_STOP: Int = 0x3002
private const val HALFWIDTH_IDEOGRAPHIC_FULL_STOP: Int = 0xFF61
private const val LAST_BMP: Int = 0xFFFF
private const val IGNORED: String = "00AD 034F 180B 180C 180D 180F 200B 2060 2064 FE00 FE01 FE02 FE03 FE04 FE05 " +
    "FE06 FE07 FE08 FE09 FE0A FE0B FE0C FE0D FE0E FE0F FEFF"
private const val HEX_RADIX: Int = 16
private const val LENGTH_REASON: String = "a hostname has between 1 and 253 characters"

internal object HostnameFormats {
    private val ignored: List<Int> by lazy { IGNORED.split(' ').map { it.toInt(HEX_RADIX) } }
    private val separators: Set<Int> = setOf('.'.code, IDEOGRAPHIC_FULL_STOP, HALFWIDTH_IDEOGRAPHIC_FULL_STOP)

    fun hostname(value: String): String? {
        val isAscii = value.all { it.code <= LAST_ASCII }
        if (!isAscii) return "a hostname consists of ASCII letters, digits, hyphens and dots"
        return nameReason(value.split('.'))
    }

    fun idnHostname(value: String): String? {
        val isBounded = value.length <= MAXIMUM_INPUT_LENGTH
        if (!isBounded) return LENGTH_REASON
        return nameReason(mapped(value).split('.'))
    }

    private fun mapped(value: String): String {
        val text = StringBuilder()
        codePoints(value).forEach { point ->
            val isFullwidth = point in FULLWIDTH_START..FULLWIDTH_END
            val isSeparator = point in separators
            val lower = lowercase(point)
            val mappedPoint = (point - FULLWIDTH_OFFSET).takeIf { isFullwidth } ?: '.'.code.takeIf { isSeparator }
            if (point !in ignored) text.appendPoint(mappedPoint ?: lower)
        }
        return text.toString()
    }

    private fun lowercase(point: Int): Int {
        val isUpper = point in 0..LAST_BMP && UnicodeData.categoryOf(point) in setOf("Lu", "Lt")
        val lowerChar = point.toChar().lowercaseChar()
        val lower = lowerChar.code.takeIf { isUpper }
        return lower?.takeIf { UnicodeData.categoryOf(it) == "Ll" } ?: point
    }

    private fun nameReason(labels: List<String>): String? {
        val length = labels.sumOf(::aceLength) + labels.size - 1
        val isInRange = length in 1..MAXIMUM_NAME_LENGTH
        if (!isInRange) return LENGTH_REASON
        val labelReason = labels.firstNotNullOfOrNull { label -> emptyReason(label) ?: labelReason(label) }
        return labelReason ?: BidiRule.domainReason(labels.map(::unicodeForm))
    }

    private fun emptyReason(label: String): String? =
        "a hostname has no empty label, also not before a final dot".takeIf { label.isEmpty() }

    private fun labelReason(label: String): String? {
        val isAscii = label.all { it.code <= LAST_ASCII }
        if (isAscii) return asciiLabelReason(label)
        val points = codePoints(label)
        val isShort = aceLength(label) <= MAXIMUM_LABEL_LENGTH
        val lengthReason = "a label has at most 63 characters in its ASCII form".takeUnless { isShort }
        return lengthReason ?: hyphenReason(points) ?: IdnaRules.labelReason(points)
    }

    private fun asciiLabelReason(label: String): String? {
        val isLdh = label.all { isLetter(it) || isDigit(it) || it == '-' }
        val isShort = label.length <= MAXIMUM_LABEL_LENGTH
        if (!isLdh || !isShort) return "a label has at most 63 ASCII letters, digits and hyphens"
        return hyphenReason(codePoints(label)) ?: aceReason(label)
    }

    private fun hyphenReason(points: IntArray): String? {
        val hasEdgeHyphen = points.firstOrNull() == '-'.code || points.lastOrNull() == '-'.code
        val hasReservedHyphens =
            points.getOrNull(HYPHEN_POSITION) == '-'.code && points.getOrNull(HYPHEN_POSITION + 1) == '-'.code
        if (hasEdgeHyphen) return "a label neither starts nor ends with a hyphen"
        val isReservedValid = !hasReservedHyphens || isAce(points)
        return "only an A-label starting with xn-- has hyphens in its third and fourth place".takeUnless {
            isReservedValid
        }
    }

    private fun isAce(points: IntArray): Boolean {
        val prefix = points.take(ACE_PREFIX.length)
        val text = prefix.joinToString("") { it.toChar().toString() }
        return text.equals(ACE_PREFIX, ignoreCase = true)
    }

    private fun aceReason(label: String): String? {
        val isAce = label.startsWith(ACE_PREFIX, ignoreCase = true)
        if (!isAce) return null
        val encoded = label.substring(ACE_PREFIX.length)
        val decoded = Punycode.decode(encoded) ?: IntArray(0)
        val isUnicode = decoded.any { it > LAST_ASCII }
        val isCanonical = Punycode.encode(decoded).equals(encoded, ignoreCase = true)
        val isLabelValid = hyphenReason(decoded) == null && IdnaRules.labelReason(decoded) == null
        val isValid = isUnicode && isCanonical && isLabelValid
        return "an A-label is the Punycode of a valid U-label".takeUnless { isValid }
    }

    private fun unicodeForm(label: String): IntArray {
        val isAce = label.startsWith(ACE_PREFIX, ignoreCase = true)
        val decoded = label.takeIf { isAce }?.let { Punycode.decode(it.substring(ACE_PREFIX.length)) }
        return decoded ?: codePoints(label)
    }

    private fun aceLength(label: String): Int {
        val isAscii = label.all { it.code <= LAST_ASCII }
        if (isAscii) return label.length
        return ACE_PREFIX.length + Punycode.encode(codePoints(label)).length
    }
}
