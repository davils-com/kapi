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

package com.davils.kapi.runtime.client.call

import com.davils.kapi.runtime.core.codec.multipart.Boundary
import com.davils.kapi.runtime.core.codec.multipart.MultipartFormData
import com.davils.kapi.runtime.core.codec.multipart.MultipartPart
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.wire.FilePart
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.failure.contained
import kotlinx.io.bytestring.ByteString

private const val CONTENT_TYPE: String = "Content-Type"

private const val MULTIPART: String = "multipart"

/**
 * Adds the parts of a `multipart/form-data` body of RFC 7578, encoded by the multipart encoder of the runtime core.
 *
 * A file is named by its safe file name, the last segment of its name without control characters, so a client never
 * sends a path. A builder is not thread-safe.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class MultipartBuilder internal constructor() {
    private val parts: MutableList<MultipartPart> = mutableListOf()

    internal var refusal: CallFailure? = null
        private set

    /**
     * Adds the text field [name] with [value].
     *
     * @param name the name of the part.
     * @param value the text.
     * @since 1.0.0
     */
    public fun text(name: String, value: String) {
        if (isName(name)) parts += MultipartPart.field(name, value)
    }

    /**
     * Adds the text field [name] once for every value of [values].
     *
     * @param name the name of the part.
     * @param values the texts.
     * @since 1.0.0
     */
    public fun texts(name: String, values: List<String>) {
        values.forEach { value -> text(name, value) }
    }

    /**
     * Adds [file] as the part [name]: with its own media type, else [contentType], else `application/octet-stream`.
     *
     * @param name the name of the part.
     * @param file the file.
     * @param contentType the media type the document declares, or `null`.
     * @since 1.0.0
     */
    public fun file(name: String, file: FilePart, contentType: MediaType? = null) {
        val own = file.contentType?.let(MediaType::parseOrNull)
        val type = own ?: contentType ?: MediaType.OctetStream
        val filename = file.safeFilename
        if (!isName(name)) return
        val part = filename?.let { safe ->
            MultipartPart.file(name = name, filename = safe, content = file.content, contentType = type)
        }
        parts += part ?: unnamed(name = name, contentType = type, content = file.content)
    }

    /**
     * Adds every file of [files] as the part [name].
     *
     * @param name the name of the parts.
     * @param files the files.
     * @param contentType the media type the document declares, or `null`.
     * @since 1.0.0
     */
    public fun files(name: String, files: List<FilePart>, contentType: MediaType? = null) {
        files.forEach { file -> file(name, file, contentType) }
    }

    /**
     * Adds [content] of the media type [contentType] as the part [name], such as an encoded JSON object.
     *
     * @param name the name of the part.
     * @param contentType the media type.
     * @param content the content.
     * @since 1.0.0
     */
    public fun part(name: String, contentType: MediaType, content: ByteString) {
        if (isName(name)) parts += unnamed(name = name, contentType = contentType, content = content)
    }

    internal fun build(boundary: Boundary): Body {
        val encoded = contained { MultipartFormData.encode(parts.toList(), boundary) }.getOrNull()
        if (encoded == null) refusal = refusal ?: CallFailure.ParameterRefused(MULTIPART)
        return Body(MultipartFormData.contentType(boundary), encoded ?: ByteString())
    }

    private fun isName(name: String): Boolean {
        val isSafe = name.none(Char::isISOControl)
        if (!isSafe) refusal = refusal ?: CallFailure.ParameterRefused(name)
        return isSafe
    }

    private fun unnamed(name: String, contentType: MediaType, content: ByteString): MultipartPart {
        val disposition = MultipartPart.field(name, "").headers.first()
        return MultipartPart(headers = listOf(disposition, CONTENT_TYPE to contentType.toString()), content = content)
    }
}
