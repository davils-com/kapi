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

private const val LINE_TERMINATORS: String = "\n\n\r\r  "
private const val ECMA_WHITE_SPACE: String = "\u0009\u000D    ﻿﻿  "
private const val DIGITS: String = "09"
private const val WORD: String = "09AZ__az"

internal object CharacterSets {
    val digits: CodePointSet = UnicodeData.pairs(DIGITS)

    val word: CodePointSet = UnicodeData.pairs(WORD)

    val lineTerminators: CodePointSet = UnicodeData.pairs(LINE_TERMINATORS)

    val dot: CodePointSet = lineTerminators.complement()

    val space: CodePointSet by lazy {
        val separators = UnicodeData.categorySet(setOf("Zs"))
        separators.union(UnicodeData.pairs(ECMA_WHITE_SPACE))
    }

    fun isWord(codePoint: Int): Boolean = codePoint in word
}
