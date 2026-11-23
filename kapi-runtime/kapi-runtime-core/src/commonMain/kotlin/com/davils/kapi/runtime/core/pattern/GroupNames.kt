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

package com.davils.kapi.runtime.core.pattern

import com.davils.kore.core.annotation.DavilsInternal

private const val ZERO_WIDTH_NON_JOINER: Int = 0x200C
private const val ZERO_WIDTH_JOINER: Int = 0x200D
private val startCategories: Set<String> = setOf("Lu", "Ll", "Lt", "Lm", "Lo", "Nl")
private val partCategories: Set<String> = startCategories + setOf("Mn", "Mc", "Nd", "Pc")

internal fun readGroupName(text: String, start: Int): Scanned<String>? {
    val name = StringBuilder()
    var index = start
    var isValid = true
    while (isValid && index < text.length && text[index] != '>') {
        val character = readNameCharacter(text, index)
        isValid = character != null && isNameCharacter(character.value, isFirst = name.isEmpty())
        character?.let { name.appendPoint(it.value) }
        index = character?.end ?: index
    }
    val isClosed = isValid && index < text.length && name.isNotEmpty()
    return Scanned(name.toString(), index + 1).takeIf { isClosed }
}

private fun readNameCharacter(text: String, index: Int): Scanned<Int>? {
    val isEscape = text[index] == '\\'
    val isUnicodeEscape = isEscape && text.getOrNull(index + 1) == 'u'
    if (isEscape) return readUnicodeEscape(text, index + 1).takeIf { isUnicodeEscape }
    val codePoint = codePointAt(text, index)
    return Scanned(codePoint, end = index + charCount(codePoint))
}

private fun isNameCharacter(codePoint: Int, isFirst: Boolean): Boolean {
    val isSpecial = codePoint == '$'.code || codePoint == '_'.code
    val isJoiner = !isFirst && (codePoint == ZERO_WIDTH_NON_JOINER || codePoint == ZERO_WIDTH_JOINER)
    val categories = startCategories.takeIf { isFirst } ?: partCategories
    return isSpecial || isJoiner || UnicodeData.categoryOf(codePoint) in categories
}
