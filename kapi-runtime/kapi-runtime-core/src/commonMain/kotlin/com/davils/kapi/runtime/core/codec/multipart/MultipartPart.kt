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

package com.davils.kapi.runtime.core.codec.multipart

import com.davils.kapi.runtime.core.header.ContentDisposition
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val CONTENT_DISPOSITION: String = "Content-Disposition"

private const val CONTENT_TYPE: String = "Content-Type"

private const val QUOTE: String = "\""

private const val ESCAPED_QUOTE: String = "%22"

private const val NONE: String = "none"

/**
 * One part of a multipart body: its header fields and its content.
 *
 * Header names are tokens and header values hold no control character, so a part can never inject a header field or
 * a boundary into the body it is written into; values may hold Unicode text, which a body carries as UTF-8, as
 * `multipart/form-data` of RFC 7578 does. [disposition], [name], [filename] and [contentType] read the fields of a
 * part of a form. A part is immutable; [toString] shows its size and its names escaped, never its content.
 *
 * @param headers the header fields in order.
 * @property content the content of the part.
 * @throws IllegalArgumentException if a header name is no token or a value holds a control character, such as a line
 * break.
 * @since 1.0.0
 */
public class MultipartPart(headers: List<Pair<String, String>>, public val content: ByteString) {
    /**
     * The header fields in order.
     *
     * @since 1.0.0
     */
    public val headers: List<Pair<String, String>> = headers.toList()

    init {
        require(headers.all { (name, value) -> FieldGrammar.isToken(name) && value.none(FieldGrammar::isControl) }) {
            "A header field of a part has a token as its name and no control character in its value"
        }
    }

    /**
     * The `Content-Disposition` of the part, or `null` if it has none or it is malformed.
     *
     * @since 1.0.0
     */
    public val disposition: ContentDisposition? get() = header(
        CONTENT_DISPOSITION,
    )?.let(ContentDisposition::parseOrNull)

    /**
     * The name of the field of a form the part belongs to, or `null`.
     *
     * @since 1.0.0
     */
    public val name: String? get() = disposition?.name

    /**
     * The file name the sender gave, or `null`; see [ContentDisposition.safeFilename] before storing a file.
     *
     * @since 1.0.0
     */
    public val filename: String? get() = disposition?.filename

    /**
     * The `Content-Type` of the part, or `null` if it has none or it is malformed; a part of a form without one is
     * `text/plain` by RFC 7578 §4.4.
     *
     * @since 1.0.0
     */
    public val contentType: MediaType? get() = header(CONTENT_TYPE)?.let(MediaType::parseOrNull)

    /**
     * Returns the first value of the header field [name].
     *
     * @param name the name, compared without case.
     * @return the value, or `null` if the part has no such field.
     * @since 1.0.0
     */
    public fun header(name: String): String? =
        headers.firstOrNull { (candidate, _) -> candidate.equals(name, ignoreCase = true) }?.second

    override fun equals(other: Any?): Boolean =
        other is MultipartPart && headers == other.headers && content == other.content

    override fun hashCode(): Int = listOf(headers, content).hashCode()

    override fun toString(): String {
        val shownName = name?.let(SafeText::text) ?: NONE
        val shownFile = filename?.let(SafeText::text) ?: NONE
        return "MultipartPart(name=$shownName, filename=$shownFile, ${content.size} bytes)"
    }

    /**
     * Makes the parts of a `multipart/form-data` form of RFC 7578.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns a text field of a form.
         *
         * @param name the name of the field, without control characters.
         * @param value the value, written as UTF-8.
         * @return the part.
         * @throws IllegalArgumentException if [name] holds a control character.
         * @since 1.0.0
         */
        public fun field(name: String, value: String): MultipartPart =
            MultipartPart(listOf(CONTENT_DISPOSITION to disposition(name, null)), value.encodeToByteString())

        /**
         * Returns a file of a form.
         *
         * @param name the name of the field, without control characters.
         * @param filename the file name, without control characters.
         * @param content the content of the file.
         * @param contentType the media type of the file.
         * @return the part.
         * @throws IllegalArgumentException if [name] or [filename] holds a control character.
         * @since 1.0.0
         */
        public fun file(
            name: String,
            filename: String,
            content: ByteString,
            contentType: MediaType = MediaType.OctetStream,
        ): MultipartPart = MultipartPart(
            listOf(CONTENT_DISPOSITION to disposition(name, filename), CONTENT_TYPE to contentType.toString()),
            content,
        )

        private fun disposition(name: String, filename: String?): String {
            val file = filename?.let { found -> "; filename=\"${escaped(found)}\"" }.orEmpty()
            return "form-data; name=\"${escaped(name)}\"$file"
        }

        private fun escaped(text: String): String {
            require(text.none(FieldGrammar::isControl)) { "A name of a form holds no control character" }
            return text.replace(oldValue = QUOTE, newValue = ESCAPED_QUOTE)
        }
    }
}
