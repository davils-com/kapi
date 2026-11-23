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

package com.davils.kapi.spec.openapi.model.path

private const val OPEN: Char = '{'

private const val CLOSE: Char = '}'

internal object Placeholders {
    const val PATH_STOPS: String = "{}/"

    const val SERVER_STOPS: String = "{}"

    fun spans(text: String, stops: String): List<IntRange> {
        val stopCharacters = stops.toCharArray()
        val spans = mutableListOf<IntRange>()
        var open = text.indexOf(OPEN)
        while (open >= 0) {
            val found = text.indexOfAny(stopCharacters, open + 1)
            val stop = found.takeIf { index -> index >= 0 } ?: text.length
            val isClosed = stop > open + 1 && text.getOrNull(stop) == CLOSE
            if (isClosed) spans += open..stop
            open = text.indexOf(OPEN, stop)
        }
        return spans
    }

    fun names(text: String, stops: String): List<String> =
        spans(text, stops).map { span -> text.substring(startIndex = span.first + 1, endIndex = span.last) }

    fun replaced(text: String, stops: String, replacement: String): String {
        val replaced = StringBuilder()
        var start = 0
        spans(text, stops).forEach { span ->
            replaced.append(text, start, span.first)
            replaced.append(replacement)
            start = span.last + 1
        }
        replaced.append(text, start, text.length)
        return replaced.toString()
    }
}
