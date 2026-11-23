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

package com.davils.kapi.runtime.core.negotiation

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.FieldLists
import com.davils.kapi.runtime.core.internal.FieldScanner
import com.davils.kapi.runtime.core.internal.MediaTypeParts
import com.davils.kapi.runtime.core.internal.MediaTypeReader

private const val WEIGHT: String = "q"

private const val WILDCARD: String = "*"

private const val LANGUAGE_SEPARATOR: Char = '-'

private const val MOST_SUBTAG_LENGTH: Int = 8

/**
 * Reads the fields of proactive negotiation of RFC 9110 §12.5: `Accept`, `Accept-Encoding` and `Accept-Language`.
 *
 * Every reader keeps the [FieldLimits] it is handed: a value longer than [FieldLimits.maxFieldLength], with more than
 * [FieldLimits.maxMembers] members or with more than [FieldLimits.maxParameters] parameters on a member is refused as
 * a whole, and so is a value with a malformed member or a malformed quality. Empty members, such as the one between
 * two adjacent commas, are skipped as RFC 9110 §5.6.1 demands. A refused value reads as `null`, which a server
 * should treat like an absent field. The readers keep no state and are safe to call from any thread.
 *
 * @since 1.0.0
 */
public object AcceptFields {
    /**
     * Reads the value of an `Accept` field.
     *
     * The parameters before the weight belong to the media range; the extension parameters after it are ignored.
     *
     * @param text the field value, the lines of a repeated field joined with commas.
     * @param limits the limits to keep.
     * @return the weighted media ranges in the order they were written, an empty list for an empty value, or `null`
     * if the value is malformed or exceeds a limit.
     * @since 1.0.0
     */
    public fun parseAccept(text: String, limits: FieldLimits = FieldLimits.Default): List<Weighted<MediaRange>>? =
        FieldLists.read(
            text = text,
            maxLength = limits.maxFieldLength,
            maxMembers = limits.maxMembers,
        ) { scanner -> mediaRange(scanner, limits) }

    /**
     * Reads the value of an `Accept-Encoding` field.
     *
     * @param text the field value.
     * @param limits the limits to keep.
     * @return the weighted content codings in lower case, `*` and `identity` included, an empty list for an empty
     * value, which accepts only `identity`, or `null` if the value is malformed or exceeds a limit.
     * @since 1.0.0
     */
    public fun parseAcceptEncoding(text: String, limits: FieldLimits = FieldLimits.Default): List<Weighted<String>>? =
        FieldLists.read(text = text, maxLength = limits.maxFieldLength, maxMembers = limits.maxMembers) { scanner ->
            weighted(scanner, limits, scanner.token()?.lowercase())
        }

    /**
     * Reads the value of an `Accept-Language` field.
     *
     * A language range of RFC 4647 §2.1 is `*` or up to eight letters followed by subtags of up to eight letters
     * and digits.
     *
     * @param text the field value.
     * @param limits the limits to keep.
     * @return the weighted language ranges in lower case, an empty list for an empty value, or `null` if the value is
     * malformed or exceeds a limit.
     * @since 1.0.0
     */
    public fun parseAcceptLanguage(text: String, limits: FieldLimits = FieldLimits.Default): List<Weighted<String>>? =
        FieldLists.read(text = text, maxLength = limits.maxFieldLength, maxMembers = limits.maxMembers) { scanner ->
            val range = scanner.token()?.takeIf(::isLanguageRange)
            weighted(scanner, limits, range?.lowercase())
        }

    internal fun isLanguageRange(text: String): Boolean {
        if (text == WILDCARD) return true
        val subtags = text.split(LANGUAGE_SEPARATOR)
        val first = subtags.first()
        val isPrimary = first.length in 1..MOST_SUBTAG_LENGTH && first.all(FieldGrammar::isAlpha)
        return isPrimary && subtags.drop(1).all(::isSubtag)
    }

    private fun isSubtag(subtag: String): Boolean =
        subtag.length in 1..MOST_SUBTAG_LENGTH && subtag.all(FieldGrammar::isAlphaNumeric)

    private fun mediaRange(scanner: FieldScanner, limits: FieldLimits): Weighted<MediaRange>? {
        val parts = MediaTypeReader.read(scanner, limits.maxParameters) ?: return null
        val weightAt = parts.parameters.indexOfFirst { (name, _) -> name == WEIGHT }
        val own = parts.parameters.takeIf { weightAt < 0 } ?: parts.parameters.take(weightAt)
        val quality = parts.parameters.getOrNull(weightAt)?.let { (_, value) -> Quality.parseOrNull(value) }
        val range = MediaRange.rangeOf(parts.copyWith(own))
        val isWeightValid = weightAt < 0 || quality != null
        return range?.takeIf { isWeightValid }?.let { found -> Weighted(found, quality ?: Quality.Highest) }
    }

    private fun <T : Any> weighted(scanner: FieldScanner, limits: FieldLimits, value: T?): Weighted<T>? {
        val parameters = value?.let { MediaTypeReader.parameters(scanner, limits.maxParameters) } ?: return null
        val weight = parameters.singleOrNull()?.takeIf { (name, _) -> name == WEIGHT }
        val quality = weight?.let { (_, text) -> Quality.parseOrNull(text) }
        val isValid = parameters.isEmpty() || quality != null
        return Weighted(value, quality ?: Quality.Highest).takeIf { isValid }
    }

    private fun MediaTypeParts.copyWith(parameters: List<Pair<String, String>>): MediaTypeParts =
        MediaTypeParts(type, subtype, parameters)
}
