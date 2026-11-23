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

import com.davils.kapi.spec.schema.pattern.CodePointSet
import com.davils.kapi.spec.schema.pattern.UnicodeData

private const val HEX_RADIX: Int = 16
private const val PVALID_EXCEPTIONS: String = "00DF 03C2 06FD 06FE 0F0B 3007"
private const val DISALLOWED: String =
    "0640 07FA 302E-302F 3031-3035 303B 1100-11FF A960-A97F D7B0-D7FF 20D0-20FF 1D100-1D24F 00AA 00BA " +
        "2100-214F 2460-24FF 3300-33FF FB00-FDFF FE00-FE0F FE10-FE1F FE30-FE4F FE70-FEFF FF00-FFEF 1D400-1D7FF"
private const val VIRAMAS: String =
    "094D 09CD 0A4D 0ACD 0B4D 0BCD 0C4D 0CCD 0D3B-0D3C 0D4D 0DCA 0E3A 0EBA 0F84 1039-103A 1714-1715 1734 17D2 " +
        "1A60 1B44 1BAA-1BAB 1BF2-1BF3 2D7F A806 A82C A8C4 A953 A9C0 AAF6 ABED 10A3F 11046 11070 1107F 110B9 " +
        "11133-11134 111C0 11235 112EA 1134D 113CE-113D0 11442 114C2 115BF 1163F 116B6 1172B 11839 1193D-1193E " +
        "119E0 11A34 11A47 11A99 11C3F 11D44-11D45 11D97 11F41-11F42"
private const val RIGHT_JOINING: String =
    "0622-0625 0627 0629 062F-0632 0648 0671-0673 0675-0677 0688-0699 06C0 06C3-06CB 06CD 06CF 06D2-06D3 06D5 " +
        "06EE-06EF 0759-075B 076B-076C 0771 0773-0774 0778-0779 08AA-08AC 08AE 08B1-08B2 08B9"
private const val NON_JOINING: String = "0621 0674"
private const val ARABIC_INDIC_DIGITS: String = "0660-0669"
private const val EXTENDED_ARABIC_INDIC_DIGITS: String = "06F0-06F9"
private const val ZERO_WIDTH_NON_JOINER: Int = 0x200C
private const val ZERO_WIDTH_JOINER: Int = 0x200D
private const val MIDDLE_DOT: Int = 0x00B7
private const val GREEK_KERAIA: Int = 0x0375
private const val HEBREW_GERESH: Int = 0x05F3
private const val HEBREW_GERSHAYIM: Int = 0x05F4
private const val KATAKANA_MIDDLE_DOT: Int = 0x30FB
private const val LAST_ASCII: Int = 0x7F
private const val CONTEXT_REASON: String = "a character of the label is not allowed where it stands, by RFC 5892"
private const val DISALLOWED_REASON: String = "a character of the label is not allowed in IDNA2008"

internal object IdnaRules {
    private val pvalidExceptions: CodePointSet by lazy { hexRanges(PVALID_EXCEPTIONS) }
    private val disallowed: CodePointSet by lazy { hexRanges(DISALLOWED) }
    private val viramas: CodePointSet by lazy { hexRanges(VIRAMAS) }
    private val rightJoining: CodePointSet by lazy { hexRanges(RIGHT_JOINING) }
    private val nonJoining: CodePointSet by lazy { hexRanges(NON_JOINING) }
    private val arabicIndicDigits: CodePointSet by lazy { hexRanges(ARABIC_INDIC_DIGITS) }
    private val extendedDigits: CodePointSet by lazy { hexRanges(EXTENDED_ARABIC_INDIC_DIGITS) }
    private val letterDigits: Set<String> = setOf("Ll", "Lo", "Lm", "Mn", "Mc", "Nd")
    private val marks: Set<String> = setOf("Mn", "Mc", "Me")
    private val transparent: Set<String> = setOf("Mn", "Me", "Cf")
    private val kanaAndHan: Set<String> = setOf("Hiragana", "Katakana", "Han")

    fun labelReason(points: IntArray): String? {
        val isStartingWithMark = points.isNotEmpty() && UnicodeData.categoryOf(points[0]) in marks
        if (isStartingWithMark) return "a label cannot start with a combining mark"
        return points.indices.firstNotNullOfOrNull { index -> pointReason(points, index) }
    }

    private fun pointReason(points: IntArray, index: Int): String? {
        val rule = ruleFor(points[index])
        if (rule != null) return CONTEXT_REASON.takeUnless { rule(points, index) }
        return DISALLOWED_REASON.takeUnless { isPermitted(points[index]) }
    }

    private fun ruleFor(point: Int): ((IntArray, Int) -> Boolean)? {
        val single: ((IntArray, Int) -> Boolean)? = mapOf<Int, (IntArray, Int) -> Boolean>(
            ZERO_WIDTH_NON_JOINER to ::isNonJoinerAllowed,
            ZERO_WIDTH_JOINER to ::isAfterVirama,
            MIDDLE_DOT to ::isBetweenLetterL,
            GREEK_KERAIA to ::isBeforeGreek,
            HEBREW_GERESH to ::isAfterHebrew,
            HEBREW_GERSHAYIM to ::isAfterHebrew,
            KATAKANA_MIDDLE_DOT to { points, _ -> hasKanaOrHan(points) },
        )[point]
        val digits = { points: IntArray, _: Int -> hasNoExtendedDigits(points) }.takeIf { point in arabicIndicDigits }
        val extended = { points: IntArray, _: Int -> hasNoArabicIndicDigits(points) }.takeIf { point in extendedDigits }
        return single ?: digits ?: extended
    }

    private fun isPermitted(point: Int): Boolean {
        val isLdh = point <= LAST_ASCII && (isLetter(point.toChar()) || isDigit(point.toChar()) || point == '-'.code)
        val isLetterDigit = point > LAST_ASCII && UnicodeData.categoryOf(point) in letterDigits
        val isAllowed = (isLdh || isLetterDigit) && point !in disallowed
        return point in pvalidExceptions || isAllowed
    }

    private fun isAfterVirama(points: IntArray, index: Int): Boolean = index > 0 && points[index - 1] in viramas

    private fun isNonJoinerAllowed(points: IntArray, index: Int): Boolean {
        if (isAfterVirama(points, index)) return true
        val before = (index - 1 downTo 0).map { points[it] }.firstOrNull { joiningType(it) != 'T' }
        val after = (index + 1 until points.size).map { points[it] }.firstOrNull { joiningType(it) != 'T' }
        val isJoinedBefore = before != null && joiningType(before) in "LD"
        return isJoinedBefore && after != null && joiningType(after) in "RD"
    }

    private fun joiningType(point: Int): Char {
        val category = UnicodeData.categoryOf(point)
        val isDual = UnicodeData.scriptOf(point) == "Arabic" && category == "Lo" && point !in nonJoining
        val type = 'R'.takeIf { point in rightJoining } ?: 'D'.takeIf { isDual }
        return type ?: 'T'.takeIf { category in transparent && point != ZERO_WIDTH_NON_JOINER } ?: 'U'
    }

    private fun isBetweenLetterL(points: IntArray, index: Int): Boolean =
        points.getOrNull(index - 1) == 'l'.code && points.getOrNull(index + 1) == 'l'.code

    private fun isBeforeGreek(points: IntArray, index: Int): Boolean {
        val next = points.getOrNull(index + 1)
        return next != null && UnicodeData.scriptOf(next) == "Greek"
    }

    private fun isAfterHebrew(points: IntArray, index: Int): Boolean {
        val previous = points.getOrNull(index - 1)
        return previous != null && UnicodeData.scriptOf(previous) == "Hebrew"
    }

    private fun hasKanaOrHan(points: IntArray): Boolean =
        points.any { point -> point != KATAKANA_MIDDLE_DOT && UnicodeData.scriptOf(point) in kanaAndHan }

    private fun hasNoExtendedDigits(points: IntArray): Boolean = points.none { it in extendedDigits }

    private fun hasNoArabicIndicDigits(points: IntArray): Boolean = points.none { it in arabicIndicDigits }

    private fun hexRanges(text: String): CodePointSet {
        val ranges = CodePointSet.Ranges()
        text.split(' ').forEach { entry ->
            val first = entry.substringBefore('-').toInt(HEX_RADIX)
            val last = entry.substringAfter('-').toInt(HEX_RADIX)
            ranges.add(first = first, last = last)
        }
        return ranges.toSet()
    }
}
