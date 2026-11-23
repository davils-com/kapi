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

package com.davils.kapi.runtime.core.parameter.internal

import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterShape
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.core.parameter.ParameterValue

private const val LABEL: String = "."

private const val MATRIX: String = ";"

private const val ASSIGN: String = "="

internal object PathStyle {
    fun segment(format: ParameterFormat, value: ParameterValue): String {
        val parts = Delimited.parts(value, format.isExploded, UriEncoding::encode)
        return when (format.style) {
            ParameterStyle.Label -> LABEL + parts.joinToString(labelSeparator(format))

            ParameterStyle.Matrix -> matrix(format, value, parts)

            ParameterStyle.Simple,
            ParameterStyle.Form,
            ParameterStyle.SpaceDelimited,
            ParameterStyle.PipeDelimited,
            ParameterStyle.DeepObject,
            ParameterStyle.Cookie,
            -> parts.joinToString(Delimited.COMMA)
        }
    }

    fun read(format: ParameterFormat, text: String, shape: ParameterShape): ParameterValue? = when (format.style) {
        ParameterStyle.Label -> label(format, text, shape)

        ParameterStyle.Matrix -> matrix(format, text, shape)

        ParameterStyle.Simple,
        ParameterStyle.Form,
        ParameterStyle.SpaceDelimited,
        ParameterStyle.PipeDelimited,
        ParameterStyle.DeepObject,
        ParameterStyle.Cookie,
        -> Delimited.value(text = text, shape = shape, separator = Delimited.COMMA, isExploded = format.isExploded)
    }

    private fun labelSeparator(format: ParameterFormat): String = when (format.isExploded) {
        true -> LABEL
        false -> Delimited.COMMA
    }

    private fun matrix(format: ParameterFormat, value: ParameterValue, parts: List<String>): String {
        val name = UriEncoding.encode(format.name)
        val isSplit = format.isExploded && value !is ParameterValue.Text
        if (!isSplit) return named(name, parts)
        val prefix = splitPrefix(name, value)
        return parts.joinToString("") { part -> MATRIX + prefix + part }
    }

    private fun named(name: String, parts: List<String>): String {
        val text = parts.joinToString(Delimited.COMMA)
        if (text.isEmpty()) return MATRIX + name
        return MATRIX + name + ASSIGN + text
    }

    private fun splitPrefix(name: String, value: ParameterValue): String {
        if (value is ParameterValue.Fields) return ""
        return name + ASSIGN
    }

    private fun label(format: ParameterFormat, text: String, shape: ParameterShape): ParameterValue? {
        if (!text.startsWith(LABEL)) return null
        return Delimited.value(
            text = text.substring(1),
            shape = shape,
            separator = labelSeparator(format),
            isExploded = format.isExploded,
        )
    }

    private fun matrix(format: ParameterFormat, text: String, shape: ParameterShape): ParameterValue? {
        val isSplit = format.isExploded && shape != ParameterShape.Text
        if (isSplit) return split(format, text, shape)
        val rest = rest(format, text)
        return rest?.let { value ->
            Delimited.value(text = value, shape = shape, separator = Delimited.COMMA, isExploded = false)
        }
    }

    private fun rest(format: ParameterFormat, text: String): String? {
        val head = MATRIX + format.name
        if (text == head) return ""
        val assigned = text.takeIf { candidate -> candidate.startsWith(head + ASSIGN) }
        return assigned?.substring(head.length + 1)
    }

    private fun split(format: ParameterFormat, text: String, shape: ParameterShape): ParameterValue? {
        val parts = text.takeIf { candidate -> candidate.startsWith(MATRIX) }?.let { found ->
            Delimited.split(found.substring(1), MATRIX)
        }
        return parts?.let { found -> splitParts(format, found, shape) }
    }

    private fun splitParts(format: ParameterFormat, parts: List<String>, shape: ParameterShape): ParameterValue? {
        if (shape == ParameterShape.Fields) return Delimited.fields(parts, isExploded = true)
        return items(format, parts)
    }

    private fun items(format: ParameterFormat, parts: List<String>): ParameterValue.Items? {
        val prefix = format.name + ASSIGN
        if (parts.any { part -> !part.startsWith(prefix) }) return null
        return ParameterValue.Items(parts.map { part -> part.removePrefix(prefix) })
    }
}
