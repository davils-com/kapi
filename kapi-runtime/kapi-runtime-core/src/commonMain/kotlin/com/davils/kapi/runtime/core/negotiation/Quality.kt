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

package com.davils.kapi.runtime.core.negotiation

import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.jvm.JvmInline

private const val SCALE: Int = 1_000

private const val MOST_DECIMALS: Int = 3

private const val DECIMAL_BASE: Int = 10

private const val POINT: Char = '.'

/**
 * A quality value of RFC 9110 §12.4.2, the weight a client gives a member of `Accept`, `Accept-Encoding` or
 * `Accept-Language`, kept exactly as thousandths from `0` to `1000`.
 *
 * A quality of zero marks what is not acceptable. Its [toString] is the shortest qvalue that writes it, such as `1`,
 * `0.5` or `0.125`.
 *
 * @property thousandths the quality in thousandths, from `0` to `1000`.
 * @since 1.0.0
 */
@JvmInline
public value class Quality(public val thousandths: Int) : Comparable<Quality> {
    init {
        require(thousandths in 0..SCALE) { "A quality lies between 0 and 1000 thousandths, but was $thousandths" }
    }

    /**
     * Whether this quality marks something as acceptable, which every quality but zero does.
     *
     * @since 1.0.0
     */
    public val isAcceptable: Boolean get() = thousandths > 0

    override fun compareTo(other: Quality): Int = thousandths.compareTo(other.thousandths)

    override fun toString(): String {
        if (thousandths == SCALE || thousandths == 0) return (thousandths / SCALE).toString()
        val digits = (thousandths + SCALE).toString().drop(1)
        return "0." + digits.trimEnd('0')
    }

    /**
     * The qualities RFC 9110 names and the reading of a qvalue.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The quality `1`, what a member without a weight has.
         *
         * @since 1.0.0
         */
        public val Highest: Quality = Quality(SCALE)

        /**
         * The quality `0`, which marks something as not acceptable.
         *
         * @since 1.0.0
         */
        public val NotAcceptable: Quality = Quality(0)

        /**
         * Reads [text] as a qvalue: `0` or `1`, followed by a point and at most three digits, and never above `1`.
         *
         * @param text the text, which may come from an attacker.
         * @return the quality, or `null` if [text] is no qvalue.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Quality? {
            val whole = text.firstOrNull()?.takeIf { char -> char == '0' || char == '1' }
            val fraction = text.drop(1)
            val decimals = fraction.drop(1)
            val isFraction = fraction.isEmpty() || (fraction.first() == POINT && decimals.length <= MOST_DECIMALS)
            val isValid = whole != null && isFraction && decimals.all(FieldGrammar::isDigit)
            if (!isValid) return null
            val padded = decimals.padEnd(MOST_DECIMALS, '0')
            val fractionValue = padded.fold(0) { sum, digit -> sum * DECIMAL_BASE + digit.digitToInt() }
            val value = text.first().digitToInt() * SCALE + fractionValue
            return value.takeIf { number -> number <= SCALE }?.let(::Quality)
        }

        /**
         * Reads [text] as a qvalue like [parseOrNull].
         *
         * @param text the text.
         * @return the quality.
         * @throws IllegalArgumentException if [text] is no qvalue.
         * @since 1.0.0
         */
        public fun parse(text: String): Quality =
            requireNotNull(parseOrNull(text)) { "${SafeText.quoted(text)} is no quality such as 0.5" }
    }
}
