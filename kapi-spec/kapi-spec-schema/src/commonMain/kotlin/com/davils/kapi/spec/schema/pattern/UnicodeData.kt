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

private const val CATEGORY_WIDTH: Int = 1
private const val SCRIPT_WIDTH: Int = 2
private const val LAST_ASCII: Int = 0x7F
private const val WHITE_SPACE: String =
    "\u0009\u000D  \u0085\u0085            " +
        "　　"
private const val HEX_DIGITS: String = "09AFaf０９ＡＦａｆ"
private const val ASCII_HEX_DIGITS: String = "09AFaf"

internal object UnicodeData {
    val CATEGORY_CODES: List<String> = listOf(
        "Cc", "Cf", "Cn", "Co", "Cs", "Ll", "Lm", "Lo", "Lt", "Lu", "Mc", "Me", "Mn", "Nd", "Nl",
        "No", "Pc", "Pd", "Pe", "Pf", "Pi", "Po", "Ps", "Sc", "Sk", "Sm", "So", "Zl", "Zp", "Zs",
    )

    private val categories: UnicodeRuns by lazy { UnicodeRuns.valued(GENERAL_CATEGORY_RUNS, CATEGORY_WIDTH) }
    private val scripts: UnicodeRuns by lazy { UnicodeRuns.valued(SCRIPT_RUNS, SCRIPT_WIDTH) }
    private val scriptNames: List<List<String>> by lazy {
        SCRIPT_NAMES.split(' ').map { names -> names.split(',') }
    }

    fun categoryOf(codePoint: Int): String = CATEGORY_CODES[categories.valueOf(codePoint)]

    fun categorySet(codes: Set<String>): CodePointSet {
        val indices = codes.map { code -> CATEGORY_CODES.indexOf(code) }
        return categories.setOf { value -> value in indices }
    }

    fun scriptOf(codePoint: Int): String = scriptNames[scripts.valueOf(codePoint)].first()

    fun scriptIndexOf(name: String): Int? {
        val index = scriptNames.indexOfFirst { names -> name in names }
        return index.takeIf { it >= 0 }
    }

    fun scriptSet(index: Int): CodePointSet = scripts.setOf { value -> value == index }

    fun binarySet(property: BinaryProperty): CodePointSet = when (property) {
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

    fun pairs(encoded: String): CodePointSet {
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
