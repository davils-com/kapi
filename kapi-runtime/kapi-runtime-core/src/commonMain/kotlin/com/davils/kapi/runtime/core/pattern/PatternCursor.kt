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

private const val QUANTIFIER_STARTS: String = "*+?{"

internal class PatternCursor(val source: String) {
    var index: Int = 0

    var unsupported: PatternIssue? = null
        private set

    val isAtEnd: Boolean get() = index >= source.length

    val isAtQuantifier: Boolean get() = !isAtEnd && source[index] in QUANTIFIER_STARTS

    val current: Char get() = source[index]

    fun isAt(char: Char): Boolean = !isAtEnd && source[index] == char

    fun accept(token: String): Boolean {
        val isMatch = source.startsWith(token, index)
        if (isMatch) index += token.length
        return isMatch
    }

    fun digits(): String {
        val start = index
        while (!isAtEnd && isAsciiDigit(source[index])) index++
        return source.substring(startIndex = start, endIndex = index)
    }

    fun codePoint(): Int {
        val codePoint = codePointAt(source, index)
        index += charCount(codePoint)
        return codePoint
    }

    fun record(reason: PatternRefusal, offset: Int, detail: String) {
        unsupported = unsupported ?: PatternIssue(reason, offset, detail)
    }

    fun fail(offset: Int, detail: String): Nothing =
        throw PatternFailure(PatternIssue(PatternRefusal.Syntax, offset, detail))
}
