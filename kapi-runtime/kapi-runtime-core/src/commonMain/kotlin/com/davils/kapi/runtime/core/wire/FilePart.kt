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

package com.davils.kapi.runtime.core.wire

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.Serializable

private const val NO_NAME: String = "unnamed"

private const val SLASH: Char = '/'

private const val BACKSLASH: Char = '\\'

private val reserved: Set<String> = setOf("", ".", "..")

/**
 * A file sent as a part of `multipart/form-data` or as a body of its own, with the name and the media type the sender
 * gave it.
 *
 * The [filename] is what the sender wrote and may be a path that climbs out of a directory: store a file under
 * [safeFilename], or better under a name of your own. Inside a JSON or CBOR document a file is written as Base64 by
 * [FilePartSerializer], which carries only its content. A file part is immutable; [toString] shows its size and
 * never its content.
 *
 * @property content the bytes of the file.
 * @property filename the file name the sender gave, or `null`.
 * @property contentType the media type the sender gave, or `null`.
 * @param headers the other header fields of the part.
 * @since 1.0.0
 */
@Serializable(with = FilePartSerializer::class)
public class FilePart(
    public val content: ByteString,
    public val filename: String? = null,
    public val contentType: String? = null,
    headers: Map<String, List<String>> = emptyMap(),
) {
    /**
     * The other header fields of the part, copied when the part was made.
     *
     * @since 1.0.0
     */
    public val headers: Map<String, List<String>> = headers.toMap()

    /**
     * The last segment of [filename] after every slash and backslash, without control characters and surrounding
     * blanks, or `null` if nothing usable is left, such as for `..`.
     *
     * @since 1.0.0
     */
    public val safeFilename: String?
        get() {
            val name = filename ?: return null
            val last = name.substringAfterLast(SLASH).substringAfterLast(BACKSLASH)
            val clean = last.filterNot(Char::isISOControl).trim()
            return clean.takeIf { candidate -> candidate !in reserved }
        }

    private val identity: List<Any?> get() = listOf(content, filename, contentType, headers)

    override fun equals(other: Any?): Boolean = other is FilePart && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String {
        val name = filename?.let(SafeText::text) ?: NO_NAME
        val type = contentType?.let(SafeText::text) ?: NO_NAME
        return "FilePart(${content.size} bytes, filename=$name, contentType=$type)"
    }
}
