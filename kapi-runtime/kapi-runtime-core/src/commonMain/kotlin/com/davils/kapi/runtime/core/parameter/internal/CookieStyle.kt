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

package com.davils.kapi.runtime.core.parameter.internal

import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.PercentCoding
import com.davils.kapi.runtime.core.parameter.ParameterEntry
import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterReading
import com.davils.kapi.runtime.core.parameter.ParameterShape
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.core.parameter.ParameterValue
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap

private const val PAIR_SEPARATOR: String = "; "

private const val SEMICOLON: Char = ';'

private const val ASSIGN: Char = '='

private const val QUOTE: Char = '"'

private const val COMMA: Char = ','

private const val NOT_COOKIE_OCTETS: String = "\";\\"

internal object CookieStyle {
    fun text(format: ParameterFormat, value: ParameterValue): String? {
        val entries = EntryStyle.entries(format, value).map { entry -> written(format, entry) }
        val isSafe = entries.all { entry -> FieldGrammar.isToken(entry.name) && entry.value.all(::isCookieOctet) }
        return entries.joinToString(PAIR_SEPARATOR) { entry -> entry.name + ASSIGN + entry.value }.takeIf { isSafe }
    }

    fun read(format: ParameterFormat, header: String?, shape: ParameterShape, names: Set<String>): ParameterReading {
        if (header == null) return ParameterReading.Absent
        val decoded = pairs(header)?.let { found -> decoded(format, found) }
        val reading = decoded?.let { entries ->
            EntryStyle.read(format = format, entries = entries, shape = shape, names = names)
        }
        return reading ?: ParameterReading.Malformed
    }

    private fun written(format: ParameterFormat, entry: ParameterEntry): ParameterEntry {
        if (format.style == ParameterStyle.Cookie) return entry
        val value = PercentCoding.encode(entry.value, isKept = ::isFormKept)
        return ParameterEntry(entry.name, value)
    }

    private fun isFormKept(char: Char): Boolean = UriEncoding.isUnreserved(char) || char == COMMA

    private fun isCookieOctet(char: Char): Boolean = FieldGrammar.isVisible(char) && char !in NOT_COOKIE_OCTETS

    private fun pairs(header: String): List<Pair<String, String>>? {
        val parts = header.split(SEMICOLON, limit = Delimited.MAX_PARTS + 1)
        if (parts.size > Delimited.MAX_PARTS) return null
        return parts.mapNotNull(::pairOf)
    }

    private fun pairOf(part: String): Pair<String, String>? {
        val trimmed = part.trim()
        val name = trimmed.substringBefore(ASSIGN).trim()
        val value = trimmed.substringAfter(ASSIGN, "").trim()
        val unquoted = value.removeSurrounding(QUOTE.toString())
        return (name to unquoted).takeIf { ASSIGN in trimmed && name.isNotEmpty() }
    }

    private fun decoded(format: ParameterFormat, pairs: List<Pair<String, String>>): Map<String, List<String>>? {
        val isRaw = format.style == ParameterStyle.Cookie
        val grouped = OrderedMap<String, MutableList<String>>()
        var isValid = true
        pairs.forEach { (name, value) ->
            val text = value.takeIf { isRaw } ?: PercentCoding.decode(value)
            isValid = isValid && text != null
            text?.let { found -> grouped.getOrPut(name) { ArrayList() } += found }
        }
        return grouped.entries.toMap().takeIf { isValid }
    }
}
