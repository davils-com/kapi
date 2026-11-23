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

package com.davils.kapi.runtime.server.request

import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.server.internal.BodyRead
import com.davils.kapi.runtime.server.internal.BodySource
import kotlinx.io.bytestring.ByteString

/**
 * A raw upload, such as the `application/octet-stream` body of a file, that a handler reads in pieces as they arrive
 * instead of as a whole.
 *
 * The upload keeps [ServerLimits.maxUploadBytes][com.davils.kapi.runtime.server.ServerLimits.maxUploadBytes] and, for
 * a compressed body, the limits of inflation; a body that breaks them makes [read] throw [BodyRefusedException], which
 * the server answers with 413 (Content Too Large). An upload is read once.
 *
 * @property contentType the media type of the upload.
 * @property length the length the client declared, or `null` if the body is chunked.
 * @since 1.0.0
 */
public class Upload internal constructor(
    public val contentType: MediaType,
    public val length: Long?,
    private val source: BodySource,
    private val limit: Long,
) {
    /**
     * Reads the upload and hands every piece to [consume] as it arrives.
     *
     * @param consume takes a piece; it may suspend to hold back a fast client.
     * @return the number of bytes read.
     * @throws BodyRefusedException if the upload breaks a limit or its content coding.
     * @throws IllegalStateException if the body was read before.
     * @since 1.0.0
     */
    public suspend fun read(consume: suspend (ByteString) -> Unit): Long =
        when (val read = source.stream(limit, consume)) {
            is BodyRead.Read -> read.value
            is BodyRead.Refused -> throw BodyRefusedException(read.refusal.status, read.refusal.code)
        }

    override fun toString(): String = "Upload($contentType, ${length ?: "unknown"} bytes)"
}
