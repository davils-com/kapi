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

package com.davils.kapi.runtime.core.header

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.internal.ExtValue
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.FieldLists
import com.davils.kapi.runtime.core.internal.FieldScanner
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.uri.UriReference

private const val OPEN: Char = '<'

private const val CLOSE: Char = '>'

private const val SEMICOLON: Char = ';'

private const val ASSIGN: Char = '='

/**
 * Reads and writes `Link` fields of RFC 8288, such as the links to the next and the last page of a paged result.
 *
 * The reader keeps the [FieldLimits] it is handed: the length of the value, the number of links and the number of
 * parameters of a link. Parameters may be written without a value and with blanks around `=`; a parameter that is
 * named again within one link is ignored, as RFC 8288 §3.3 demands for `rel`. A value is refused as a whole when a
 * link is malformed. The functions keep no state and are safe to call from any thread.
 *
 * @since 1.0.0
 */
public object Links {
    /**
     * The relation of the next page.
     *
     * @since 1.0.0
     */
    public const val NEXT: String = "next"

    /**
     * The relation of the previous page.
     *
     * @since 1.0.0
     */
    public const val PREV: String = "prev"

    /**
     * The relation of the first page.
     *
     * @since 1.0.0
     */
    public const val FIRST: String = "first"

    /**
     * The relation of the last page.
     *
     * @since 1.0.0
     */
    public const val LAST: String = "last"

    /**
     * Reads the value of a `Link` field.
     *
     * @param text the field value, the lines of a repeated field joined with commas.
     * @param limits the limits to keep.
     * @return the links in order, or `null` if a link is malformed or the value exceeds a limit.
     * @since 1.0.0
     */
    public fun parseOrNull(text: String, limits: FieldLimits = FieldLimits.Default): List<LinkValue>? =
        FieldLists.read(text, limits.maxFieldLength, limits.maxMembers) { scanner -> link(scanner, limits) }

    /**
     * Writes [links] as the value of a `Link` field.
     *
     * @param links the links in order.
     * @return the field value.
     * @since 1.0.0
     */
    public fun format(links: List<LinkValue>): String = links.joinToString(", ")

    /**
     * Returns the first link with the relation [relation], such as [NEXT].
     *
     * @param links the links.
     * @param relation the relation type, compared without case.
     * @return the link, or `null` if no link has the relation.
     * @since 1.0.0
     */
    public fun find(links: List<LinkValue>, relation: String): LinkValue? {
        val key = relation.lowercase()
        return links.firstOrNull { link -> key in link.relations }
    }

    private fun link(scanner: FieldScanner, limits: FieldLimits): LinkValue? {
        val isOpen = scanner.consume(OPEN)
        val target = scanner.span { char -> char != CLOSE && FieldGrammar.isVisible(char) }
        val isClosed = isOpen && target != null && scanner.consume(CLOSE)
        val parameters = parameters(scanner, limits).takeIf { isClosed }
        val isTarget = target?.let(UriReference::parseOrNull) != null
        if (parameters == null || target == null || !isTarget) return null
        return LinkValue(target, parameters)
    }

    private fun parameters(scanner: FieldScanner, limits: FieldLimits): List<Pair<String, String>>? {
        val parameters = ArrayList<Pair<String, String>>()
        var isValid = true
        while (isValid && startsParameter(scanner)) {
            val parameter = parameter(scanner)
            val isNew = parameter != null && parameters.none { (name, _) -> name == parameter.first }
            parameter?.takeIf { isNew }?.let(parameters::add)
            isValid = parameter != null && parameters.size <= limits.maxParameters
        }
        return parameters.takeIf { isValid }
    }

    private fun startsParameter(scanner: FieldScanner): Boolean {
        val mark = scanner.position
        scanner.skipWhitespace()
        val isParameter = scanner.consume(SEMICOLON)
        if (!isParameter) scanner.reset(mark)
        scanner.skipWhitespace()
        return isParameter
    }

    private fun parameter(scanner: FieldScanner): Pair<String, String>? {
        val name = scanner.token()?.lowercase() ?: return null
        val mark = scanner.position
        scanner.skipWhitespace()
        val hasValue = scanner.consume(ASSIGN)
        if (!hasValue) scanner.reset(mark)
        val value = valueOf(scanner, hasValue)?.let { text -> decoded(name, text) }
        return value?.let { found -> name to found }
    }

    private fun valueOf(scanner: FieldScanner, hasValue: Boolean): String? {
        if (!hasValue) return ""
        scanner.skipWhitespace()
        return scanner.tokenOrQuoted()
    }

    private fun decoded(name: String, text: String): String? {
        if (LinkValue.isExtended(name)) return ExtValue.decode(text)
        return text.takeIf(FieldGrammar::isSafeValue)
    }
}
