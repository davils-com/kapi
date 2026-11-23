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

package com.davils.kapi.runtime.testing.internal

import com.davils.kore.core.failure.contained
import io.ktor.http.decodeURLPart

internal object PathTemplates {
    fun match(template: String, path: String): PathMatch? {
        val expected = template.split('/')
        val actual = path.split('/')
        val values = mutableListOf<Pair<String, String>>()
        val isSameLength = expected.size == actual.size
        val isMatching = isSameLength &&
            expected.indices.all { index ->
                segment(template = expected[index], actual = actual[index], values = values)
            }
        return PathMatch(
            template = template,
            parameters = values,
            literals = literalsOf(template),
        ).takeIf { isMatching }
    }

    fun isTemplate(template: String): Boolean {
        val parts = template.split('/').flatMap(::partsOf)
        val isBalanced = template.count { char -> char == '{' } == template.count { char -> char == '}' }
        return template.startsWith('/') && isBalanced && parts.none { part -> part.isName && part.text.isEmpty() }
    }

    private fun segment(template: String, actual: String, values: MutableList<Pair<String, String>>): Boolean {
        val parts = partsOf(template)
        var position = 0
        val isMatching = parts.indices.all { index ->
            val part = parts[index]
            val end = endOf(parts = parts, index = index, actual = actual, position = position)
            val text = actual.substring(startIndex = position, endIndex = end.coerceAtLeast(position))
            val isFitting = (part.isName && end > position) || (!part.isName && text == part.text)
            if (part.isName && isFitting) values += part.text to decoded(text)
            position = end
            isFitting
        }
        return isMatching && position == actual.length
    }

    private fun endOf(parts: List<Part>, index: Int, actual: String, position: Int): Int {
        val part = parts[index]
        if (!part.isName) return minOf(a = actual.length, b = position + part.text.length)
        val next = parts.getOrNull(index + 1)?.takeUnless(Part::isName)
        return next?.let { literal -> actual.indexOf(literal.text, position) } ?: actual.length
    }

    private fun partsOf(segment: String): List<Part> {
        val parts = mutableListOf<Part>()
        var rest = segment
        while (rest.isNotEmpty()) {
            val part = nextPart(rest)
            parts += part
            rest = rest.substring(startIndex = part.length)
        }
        return parts
    }

    private fun nextPart(rest: String): Part {
        val open = rest.indexOf('{')
        val close = rest.indexOf(char = '}', startIndex = maxOf(a = open, b = 0))
        if (open == 0 && close > 0) {
            return Part(
                isName = true,
                text = rest.substring(startIndex = 1, endIndex = close),
                length = close + 1,
            )
        }
        val end = open.takeIf { found -> found > 0 && close > open } ?: rest.length
        return Part(isName = false, text = rest.substring(startIndex = 0, endIndex = end), length = end)
    }

    private fun literalsOf(template: String): Int {
        val parts = template.split('/').flatMap(::partsOf)
        return parts.filterNot(Part::isName).sumOf { part -> part.text.length }
    }

    private fun decoded(text: String): String = contained { text.decodeURLPart() }.getOrNull() ?: text

    private data class Part(val isName: Boolean, val text: String, val length: Int)
}
