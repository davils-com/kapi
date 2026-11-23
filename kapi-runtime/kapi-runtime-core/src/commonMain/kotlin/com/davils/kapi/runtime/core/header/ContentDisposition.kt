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

import com.davils.kapi.runtime.core.internal.ExtValue
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.FieldScanner
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val FILENAME: String = "filename"

private const val EXTENDED_FILENAME: String = "filename*"

private const val NAME: String = "name"

private const val SEMICOLON: Char = ';'

private const val ASSIGN: Char = '='

private const val SLASH: Char = '/'

private const val BACKSLASH: Char = '\\'

private const val REPLACEMENT: Char = '_'

private const val MAX_LENGTH: Int = 8_192

private const val MAX_PARAMETERS: Int = 32

private val reserved: Set<String> = setOf("", ".", "..")

/**
 * The value of `Content-Disposition` of RFC 6266, with the `form-data` disposition of RFC 7578 §4.2 and the
 * `filename*` parameter of RFC 8187.
 *
 * A file name may be any Unicode text. [toString] writes a name that is printable ASCII as `filename="…"` alone and
 * every other name as an ASCII fallback in `filename` followed by the exact name in `filename*`, as RFC 6266 §4.3
 * recommends; other parameters must be visible ASCII. A name that holds a control character, such as a line break, is
 * refused when the value is made, so no writer can inject a field. [filename] prefers `filename*` over `filename`.
 * [safeFilename] is the name to store a file under, without any path. A value is immutable.
 *
 * @param type the disposition type, such as `attachment`, `inline` or `form-data`, a token compared without case.
 * @param parameters the parameters in order, each name once, `filename*` given decoded.
 * @throws IllegalArgumentException if [type] or a name is no token, a name repeats, a file name holds a control
 * character or another value is not visible ASCII.
 * @since 1.0.0
 */
public class ContentDisposition(type: String, parameters: List<Pair<String, String>> = emptyList()) {
    /**
     * The disposition type in lower case.
     *
     * @since 1.0.0
     */
    public val type: String = type.lowercase()

    /**
     * The parameters in order, with their names in lower case.
     *
     * @since 1.0.0
     */
    public val parameters: List<Pair<String, String>> = parameters.map { (name, value) -> name.lowercase() to value }

    init {
        val names = this.parameters.map { (name, _) -> name }
        require(FieldGrammar.isToken(type) && names.all(FieldGrammar::isToken)) { "A disposition is made of tokens" }
        require(names.toSet().size == names.size) { "A parameter of a disposition is named once" }
        require(this.parameters.all { (name, value) -> isWritable(name, value) }) {
            "A parameter of a disposition holds a character a field cannot carry"
        }
    }

    /**
     * The file name: `filename*` if present, else `filename`, else `null`.
     *
     * @since 1.0.0
     */
    public val filename: String? get() = parameter(EXTENDED_FILENAME) ?: parameter(FILENAME)

    /**
     * The name of a field of `multipart/form-data`, or `null`.
     *
     * @since 1.0.0
     */
    public val name: String? get() = parameter(NAME)

    /**
     * The last segment of [filename] after every slash and backslash, without control characters and surrounding
     * blanks, or `null` if nothing usable is left, such as for `..`.
     *
     * @since 1.0.0
     */
    public val safeFilename: String?
        get() {
            val last = filename?.substringAfterLast(SLASH)?.substringAfterLast(BACKSLASH)
            val clean = last?.filterNot(Char::isISOControl)?.trim()
            return clean?.takeIf { candidate -> candidate !in reserved }
        }

    /**
     * Returns the value of the parameter [name].
     *
     * @param name the name, compared without case.
     * @return the value, or `null` if there is no such parameter.
     * @since 1.0.0
     */
    public fun parameter(name: String): String? {
        val key = name.lowercase()
        return parameters.firstOrNull { (candidate, _) -> candidate == key }?.second
    }

    override fun equals(other: Any?): Boolean =
        other is ContentDisposition && type == other.type && parameters == other.parameters

    override fun hashCode(): Int = listOf(type, parameters).hashCode()

    override fun toString(): String {
        val written = parameters.flatMap { (name, value) -> written(name, value) }
        return (listOf(type) + written).joinToString("; ")
    }

    private fun written(name: String, value: String): List<String> {
        if (name == EXTENDED_FILENAME) return listOf("$name=${ExtValue.encode(value)}")
        val isFallback = name == FILENAME && !value.all(FieldGrammar::isVisibleOrSpace) && !hasExtended()
        return listOf("$name=${FieldGrammar.quote(fallback(value))}") + extendedOf(value, isFallback)
    }

    private fun extendedOf(value: String, isNeeded: Boolean): List<String> {
        if (!isNeeded) return emptyList()
        return listOf("$EXTENDED_FILENAME=${ExtValue.encode(value)}")
    }

    private fun hasExtended(): Boolean = parameter(EXTENDED_FILENAME) != null

    private fun fallback(value: String): String =
        value.map { char -> char.takeIf(FieldGrammar::isVisibleOrSpace) ?: REPLACEMENT }.joinToString("")

    /**
     * Makes and reads dispositions.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns `attachment` with [filename], which asks a browser to save the content.
         *
         * @param filename the file name, any Unicode text without control characters, or `null`.
         * @return the disposition.
         * @throws IllegalArgumentException if [filename] holds a control character.
         * @since 1.0.0
         */
        public fun attachment(filename: String? = null): ContentDisposition = named("attachment", filename)

        /**
         * Returns `inline` with [filename], which asks a browser to show the content.
         *
         * @param filename the file name, any Unicode text without control characters, or `null`.
         * @return the disposition.
         * @throws IllegalArgumentException if [filename] holds a control character.
         * @since 1.0.0
         */
        public fun inline(filename: String? = null): ContentDisposition = named("inline", filename)

        /**
         * Reads the value of `Content-Disposition`.
         *
         * Quoted strings may hold Unicode text, as the headers of `multipart/form-data` do. A `filename*` is decoded
         * from UTF-8 or ISO-8859-1; one in another charset or malformed refuses the value.
         *
         * @param text the field value, which may come from an attacker.
         * @return the disposition, or `null` if [text] is malformed, repeats a parameter, holds a control character,
         * is longer than 8192 characters or has more than 32 parameters.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): ContentDisposition? {
            if (text.length > MAX_LENGTH) return null
            return Reader(FieldScanner(FieldGrammar.trimmed(text))).read()
        }

        private fun named(type: String, filename: String?): ContentDisposition =
            ContentDisposition(type, listOfNotNull(filename?.let { name -> FILENAME to name }))

        private fun isWritable(name: String, value: String): Boolean {
            val isFilename = name == FILENAME || name == EXTENDED_FILENAME || name == NAME
            return value.none(FieldGrammar::isControl) && (isFilename || value.all(FieldGrammar::isVisibleOrSpace))
        }
    }

    private class Reader(private val scanner: FieldScanner) {
        fun read(): ContentDisposition? {
            val type = scanner.token()
            val parameters = type?.let { parameters() }?.takeIf(::isAcceptable)
            if (type == null || parameters == null || !scanner.isAtEnd) return null
            return ContentDisposition(type, parameters)
        }

        private fun isAcceptable(parameters: List<Pair<String, String>>): Boolean {
            val names = parameters.map { (name, _) -> name }
            val isUnique = names.toSet().size == names.size
            return isUnique && parameters.all { (name, value) -> isWritable(name, value) }
        }

        private fun parameters(): List<Pair<String, String>>? {
            val parameters = ArrayList<Pair<String, String>>()
            var isValid = true
            while (isValid && startsParameter()) {
                val parameter = parameter()
                parameter?.let(parameters::add)
                isValid = parameter != null && parameters.size <= MAX_PARAMETERS
            }
            return parameters.takeIf { isValid }
        }

        private fun startsParameter(): Boolean {
            scanner.skipWhitespace()
            val isParameter = scanner.consume(SEMICOLON)
            scanner.skipWhitespace()
            return isParameter
        }

        private fun parameter(): Pair<String, String>? {
            val name = scanner.token()?.lowercase()
            val hasValue = name != null && scanner.consume(ASSIGN)
            val value = valueOf(name, hasValue)?.let { text -> decodedOf(name, text) }
            if (name == null || value == null) return null
            return name to value
        }

        private fun valueOf(name: String?, hasValue: Boolean): String? {
            if (!hasValue) return null
            scanner.skipWhitespace()
            return scanner.tokenOrQuoted(isUnicode = name?.endsWith('*') != true)
        }

        private fun decodedOf(name: String?, text: String): String? {
            if (name?.endsWith('*') != true) return text
            return ExtValue.decode(text)
        }
    }
}
