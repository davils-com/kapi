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

package com.davils.kapi.runtime.core.range

import com.davils.kapi.runtime.core.codec.multipart.Boundary
import com.davils.kapi.runtime.core.negotiation.MediaType
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.ByteStringBuilder

private const val LINE: String = "\r\n"

private const val DASHES: String = "--"

/**
 * Writes the body of a 206 (Partial Content) response with several ranges as `multipart/byteranges` of RFC 9110
 * §14.6.
 *
 * Every part carries the `Content-Type` of the representation and its `Content-Range`. The writer keeps no state and
 * is safe to call from any thread; it holds the whole body in memory, so a server bounds the ranges it sends with
 * [com.davils.kapi.runtime.core.FieldLimits.maxRanges].
 *
 * @since 1.0.0
 */
public object MultipartByteRanges {
    /**
     * Returns the `Content-Type` of the body: `multipart/byteranges` with [boundary].
     *
     * @param boundary the boundary of the parts.
     * @return the media type.
     * @since 1.0.0
     */
    public fun contentType(boundary: Boundary): MediaType =
        MediaType.MultipartByteRanges.withParameter("boundary", boundary.value)

    /**
     * Writes the body of the parts.
     *
     * @param boundary the boundary, such as [Boundary.random].
     * @param contentType the media type of the representation.
     * @param completeLength the length of the whole representation.
     * @param parts the ranges with their bytes, in the order to send them.
     * @return the body.
     * @throws IllegalArgumentException if a part is empty, a range lies outside [completeLength] or the bytes of a
     * part are not as long as its range.
     * @since 1.0.0
     */
    public fun encode(
        boundary: Boundary,
        contentType: MediaType,
        completeLength: Long,
        parts: List<Pair<ResolvedRange, ByteString>>,
    ): ByteString {
        require(parts.isNotEmpty()) { "A multipart/byteranges body has at least one part" }
        val out = ByteStringBuilder()
        parts.forEach { (range, bytes) ->
            require(bytes.size.toLong() == range.length) { "The bytes of a part are as long as its range" }
            val contentRange = ContentRange.Satisfied(range, completeLength)
            val head = "$DASHES$boundary${LINE}Content-Type: $contentType${LINE}Content-Range: $contentRange$LINE$LINE"
            out.append(head.encodeToByteArray())
            out.append(bytes.toByteArray())
            out.append(LINE.encodeToByteArray())
        }
        out.append("$DASHES$boundary$DASHES$LINE".encodeToByteArray())
        return out.toByteString()
    }
}
