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

package com.davils.kapi.spec.asyncapi.validation

private const val OPEN: Char = '{'

private const val CLOSE: Char = '}'

internal object Templates {
    fun names(text: String): List<String>? {
        val opens = text.indices.filter { index -> text[index] == OPEN }
        val closes = text.indices.filter { index -> text[index] == CLOSE }
        val pairs = opens.zip(closes)
        val following = opens.drop(1)
        val isNested = following.zip(closes).any { (open, close) -> open < close }
        val isOrdered = pairs.all { (open, close) -> open < close }
        val isPaired = opens.size == closes.size && isOrdered && !isNested
        if (!isPaired) return null
        val names = pairs.map { (open, close) -> text.substring(startIndex = open + 1, endIndex = close) }
        return names.takeIf { names.none(String::isEmpty) }
    }
}
