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

package com.davils.kapi.runtime.core.pattern

import com.davils.kore.core.annotation.DavilsInternal

private const val CATEGORY_WIDTH: Int = 1
private const val SCRIPT_WIDTH: Int = 2
private const val LAST_ASCII: Int = 0x7F
private const val WHITE_SPACE: String =
    "\u0009\u000D  \u0085\u0085            " +
        "　　"
private const val HEX_DIGITS: String = "09AFaf０９ＡＦａｆ"
private const val ASCII_HEX_DIGITS: String = "09AFaf"

/**
 * The properties of the code points of Unicode 16.0 that the pattern engine and the formats of `kapi-spec-schema`
 * need, read from compact tables on first use.
 *
 * Internal to kapi. It is safe to use from any thread.
 *
 * @since 1.0.0
 */
@DavilsInternal
public object UnicodeData {
    internal val CATEGORY_CODES: List<String> = listOf(
        "Cc", "Cf", "Cn", "Co", "Cs", "Ll", "Lm", "Lo", "Lt", "Lu", "Mc", "Me", "Mn", "Nd", "Nl",
        "No", "Pc", "Pd", "Pe", "Pf", "Pi", "Po", "Ps", "Sc", "Sk", "Sm", "So", "Zl", "Zp", "Zs",
    )

    internal val BIDI_CLASSES: List<String> = listOf(
        "", "L", "R", "AL", "EN", "ES", "ET", "AN", "CS", "NSM", "BN", "B", "S",
        "WS", "ON", "LRE", "LRO", "RLE", "RLO", "PDF", "LRI", "RLI", "FSI", "PDI",
    )

    private val categories: UnicodeRuns by lazy { UnicodeRuns.valued(GENERAL_CATEGORY_RUNS, CATEGORY_WIDTH) }
    private val bidiClasses: UnicodeRuns by lazy { UnicodeRuns.valued(BIDI_CLASS_RUNS, CATEGORY_WIDTH) }
    private val scripts: UnicodeRuns by lazy { UnicodeRuns.valued(SCRIPT_RUNS, SCRIPT_WIDTH) }
    private val scriptNames: List<List<String>> by lazy {
        SCRIPT_NAMES.split(' ').map { names -> names.split(',') }
    }

    /**
     * Returns the general category of [codePoint] as its two-letter code, such as `Lu` or `Nd`.
     *
     * @param codePoint the code point, from 0 to [LAST_CODE_POINT].
     * @return the code of the category; `Cn` for an unassigned code point.
     * @since 1.0.0
     */
    public fun categoryOf(codePoint: Int): String = CATEGORY_CODES[categories.valueOf(codePoint)]

    /**
     * Returns the bidirectional class of [codePoint], such as `L`, `R` or `AN`.
     *
     * @param codePoint the code point, from 0 to [LAST_CODE_POINT].
     * @return the short name of the class.
     * @since 1.0.0
     */
    public fun bidiClassOf(codePoint: Int): String = BIDI_CLASSES[bidiClasses.valueOf(codePoint)]

    internal fun categorySet(codes: Set<String>): CodePointSet {
        val indices = codes.map { code -> CATEGORY_CODES.indexOf(code) }
        return categories.setOf { value -> value in indices }
    }

    /**
     * Returns the script of [codePoint] by its long name, such as `Latin` or `Greek`.
     *
     * @param codePoint the code point, from 0 to [LAST_CODE_POINT].
     * @return the long name of the script; `Unknown` for a code point of no script.
     * @since 1.0.0
     */
    public fun scriptOf(codePoint: Int): String = scriptNames[scripts.valueOf(codePoint)].first()

    internal fun scriptIndexOf(name: String): Int? {
        val index = scriptNames.indexOfFirst { names -> name in names }
        return index.takeIf { it >= 0 }
    }

    internal fun scriptSet(index: Int): CodePointSet = scripts.setOf { value -> value == index }

    internal fun binarySet(property: BinaryProperty): CodePointSet = when (property) {
        BinaryProperty.ANY -> CodePointSet.All
        BinaryProperty.ASCII -> CodePointSet.of(first = 0, last = LAST_ASCII)
        BinaryProperty.ASSIGNED -> categorySet(setOf("Cn")).complement()
        BinaryProperty.ASCII_HEX_DIGIT -> pairs(ASCII_HEX_DIGITS)
        BinaryProperty.HEX_DIGIT -> pairs(HEX_DIGITS)
        BinaryProperty.WHITE_SPACE -> pairs(WHITE_SPACE)
        BinaryProperty.ALPHABETIC -> inside(ALPHABETIC_RUNS)
        BinaryProperty.LOWERCASE -> inside(LOWERCASE_RUNS)
        BinaryProperty.UPPERCASE -> inside(UPPERCASE_RUNS)
        BinaryProperty.IDEOGRAPHIC -> inside(IDEOGRAPHIC_RUNS)
    }

    internal fun pairs(encoded: String): CodePointSet {
        val ranges = CodePointSet.Ranges()
        var index = 0
        while (index + 1 < encoded.length) {
            ranges.add(first = encoded[index].code, last = encoded[index + 1].code)
            index += 2
        }
        return ranges.toSet()
    }

    private fun inside(encoded: String): CodePointSet = UnicodeRuns.binary(encoded).setOf { value -> value == 1 }
}
