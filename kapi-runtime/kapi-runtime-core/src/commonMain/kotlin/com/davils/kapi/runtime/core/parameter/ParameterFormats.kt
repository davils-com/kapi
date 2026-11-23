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

package com.davils.kapi.runtime.core.parameter

import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.parameter.internal.CookieStyle
import com.davils.kapi.runtime.core.parameter.internal.Delimited
import com.davils.kapi.runtime.core.parameter.internal.EntryStyle
import com.davils.kapi.runtime.core.parameter.internal.PathStyle
import com.davils.kapi.runtime.core.parameter.internal.UriEncoding
import com.davils.kapi.runtime.core.parameter.internal.toReading

private val dotSegments: Set<String> = setOf(".", "..")

private const val QUERY_SEPARATOR: String = "&"

private const val ASSIGN: String = "="

/**
 * Writes [value] as one segment of a path in the style of this format, every character that is not unreserved by
 * RFC 3986 percent-encoded.
 *
 * @param value the value.
 * @return the segment, or `null` if it would be `.` or `..`, which would change the path it is put into.
 * @since 1.0.0
 */
public fun ParameterFormat.pathSegmentOrNull(value: ParameterValue): String? {
    val segment = PathStyle.segment(this, value)
    return segment.takeUnless { text -> text in dotSegments }
}

/**
 * Writes [value] as the names and values of a query or a form in the style of this format, not yet encoded.
 *
 * @param value the value.
 * @return the entries in order; none for an empty list or object.
 * @since 1.0.0
 */
public fun ParameterFormat.entries(value: ParameterValue): List<ParameterEntry> = EntryStyle.entries(this, value)

/**
 * Writes [value] as the names and values of a query, percent-encoded for the query component of RFC 3986.
 *
 * Every character but the unreserved ones is percent-encoded; with [ParameterFormat.isReservedAllowed] the reserved
 * characters and percent-encoded octets stay as they are, except `#`, `&`, `=` and `+`, which would end or change
 * the value.
 *
 * @param value the value.
 * @return the encoded entries in order.
 * @since 1.0.0
 */
public fun ParameterFormat.queryEntries(value: ParameterValue): List<ParameterEntry> = entries(value).map { entry ->
    ParameterEntry(UriEncoding.encode(entry.name), UriEncoding.encodeQuery(entry.value, isReservedAllowed))
}

/**
 * Writes [value] as text to put into a query, such as `color=blue&color=black`, encoded like [queryEntries].
 *
 * @param value the value.
 * @return the query text without a leading `?`; empty for an empty list or object.
 * @since 1.0.0
 */
public fun ParameterFormat.queryText(value: ParameterValue): String =
    queryEntries(value).joinToString(QUERY_SEPARATOR) { entry -> entry.name + ASSIGN + entry.value }

/**
 * Writes [value] as the value of a header field in the simple style.
 *
 * @param value the value.
 * @return the field value, or `null` if a part holds a character a field value cannot carry, such as a line break
 * that would inject another field.
 * @since 1.0.0
 */
public fun ParameterFormat.headerTextOrNull(value: ParameterValue): String? {
    val parts = Delimited.parts(value, isExploded)
    val text = parts.joinToString(Delimited.COMMA)
    return text.takeIf(FieldGrammar::isSafeValue)
}

/**
 * Writes [value] as the pairs of a `Cookie` field, such as `color=blue; size=2`.
 *
 * In the [ParameterStyle.Cookie] style of OpenAPI 3.2 the values are written as they are and must already consist of
 * the cookie octets of RFC 6265; in every other style they are percent-encoded like a form.
 *
 * @param value the value.
 * @return the cookie pairs, or `null` if a name is no token or a value holds a character a cookie cannot carry, such
 * as a semicolon, a quote, a space or a line break.
 * @since 1.0.0
 */
public fun ParameterFormat.cookieTextOrNull(value: ParameterValue): String? = CookieStyle.text(this, value)

/**
 * Reads the segment of a path as the value of this format.
 *
 * @param text the segment, already percent-decoded, or `null` if the path has none.
 * @param shape the shape of the schema.
 * @return the value, [ParameterReading.Absent] for `null` or [ParameterReading.Malformed]; a value with more than 1024
 * parts is malformed.
 * @since 1.0.0
 */
public fun ParameterFormat.readPath(text: String?, shape: ParameterShape): ParameterReading {
    if (text == null) return ParameterReading.Absent
    return PathStyle.read(this, text, shape).toReading()
}

/**
 * Reads the decoded entries of a query or a form as the value of this format.
 *
 * @param entries the names with every value they have, already percent-decoded.
 * @param shape the shape of the schema.
 * @param fields the names of the properties of an exploded object in the form style.
 * @return the value, [ParameterReading.Absent] or [ParameterReading.Malformed]; a value with more than 1024 parts is
 * malformed.
 * @since 1.0.0
 */
public fun ParameterFormat.readEntries(
    entries: Map<String, List<String>>,
    shape: ParameterShape,
    fields: Set<String> = emptySet(),
): ParameterReading = EntryStyle.read(format = this, entries = entries, shape = shape, names = fields)

/**
 * Reads the value of a header field in the simple style, dropping the blanks around every part.
 *
 * @param text the field value, or `null` if the request has none.
 * @param shape the shape of the schema.
 * @return the value, [ParameterReading.Absent] or [ParameterReading.Malformed].
 * @since 1.0.0
 */
public fun ParameterFormat.readHeader(text: String?, shape: ParameterShape): ParameterReading {
    if (text == null) return ParameterReading.Absent
    val parts = Delimited.split(text, Delimited.COMMA)
    val trimmed = parts?.joinToString(Delimited.COMMA) { part -> part.trim() }
    val value = trimmed?.let { joined ->
        Delimited.value(text = joined, shape = shape, separator = Delimited.COMMA, isExploded = isExploded)
    }
    return value.toReading()
}

/**
 * Reads the value of this format from a `Cookie` field.
 *
 * The pairs are separated by semicolons; a pair without `=` belongs to no parameter and is skipped, and a value in
 * quotes loses them. In every style but [ParameterStyle.Cookie] the values are percent-decoded, and a value that is
 * no percent-encoded UTF-8 is malformed.
 *
 * @param header the value of the `Cookie` field, or `null` if the request has none.
 * @param shape the shape of the schema.
 * @param fields the names of the properties of an exploded object.
 * @return the value, [ParameterReading.Absent] or [ParameterReading.Malformed]; a field with more than 1024 pairs is
 * malformed.
 * @since 1.0.0
 */
public fun ParameterFormat.readCookie(
    header: String?,
    shape: ParameterShape,
    fields: Set<String> = emptySet(),
): ParameterReading = CookieStyle.read(format = this, header = header, shape = shape, names = fields)
