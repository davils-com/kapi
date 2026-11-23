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

import com.davils.kapi.runtime.core.codec.multipart.Boundary
import com.davils.kapi.runtime.core.codec.multipart.MultipartDecoder
import com.davils.kapi.runtime.core.codec.multipart.MultipartEvent
import com.davils.kapi.runtime.core.codec.multipart.MultipartFailure
import com.davils.kapi.runtime.core.codec.multipart.MultipartLimits
import com.davils.kapi.runtime.server.internal.BodyRead
import com.davils.kapi.runtime.server.internal.BodySource
import com.davils.kapi.runtime.server.internal.MultipartRefusals
import com.davils.kore.core.outcome.Outcome

/**
 * An upload of `multipart/form-data` of RFC 7578 that a handler reads as the events of the bounded
 * [MultipartDecoder] of the runtime core, as they arrive, so the server never holds a whole upload in memory.
 *
 * The upload keeps the [MultipartLimits] of the server, its upload limit and, for a compressed body, the limits of
 * inflation; a body that breaks them or the grammar of RFC 2046 makes [read] throw [BodyRefusedException], which the
 * server answers with 413 (Content Too Large) or 400 (Bad Request). Each part is announced with its header fields,
 * whose file names a handler checks with
 * [ContentDisposition.safeFilename][com.davils.kapi.runtime.core.header.ContentDisposition.safeFilename] before it
 * stores anything. An upload is read once.
 *
 * @property boundary the boundary of the body.
 * @since 1.0.0
 */
public class MultipartUpload internal constructor(
    public val boundary: Boundary,
    private val limits: MultipartLimits,
    private val source: BodySource,
    private val limit: Long,
) {
    /**
     * Reads the upload and hands every event to [handle] as it is read.
     *
     * @param handle takes an event; it may suspend to hold back a fast client.
     * @throws BodyRefusedException if the body breaks a limit or its format.
     * @throws IllegalStateException if the body was read before.
     * @since 1.0.0
     */
    public suspend fun read(handle: suspend (MultipartEvent) -> Unit) {
        val decoder = MultipartDecoder(boundary, limits)
        val read = source.stream(minOf(a = limit, b = limits.maxBodyBytes)) { chunk ->
            delivered(decoder.feed(chunk), handle)
        }
        if (read is BodyRead.Refused) throw BodyRefusedException(read.refusal.status, read.refusal.code)
        delivered(decoder.finish(), handle)
    }

    override fun toString(): String = "MultipartUpload($boundary)"

    private suspend fun delivered(
        outcome: Outcome<List<MultipartEvent>, MultipartFailure>,
        handle: suspend (MultipartEvent) -> Unit,
    ) {
        when (outcome) {
            is Outcome.Failure -> throw BodyRefusedException(
                status = MultipartRefusals.statusOf(outcome.failure),
                code = MultipartRefusals.codeOf(outcome.failure),
            )

            is Outcome.Success -> outcome.value.forEach { event -> handle(event) }
        }
    }
}
