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

package com.davils.kapi.runtime.core.codec.multipart

import com.davils.kapi.runtime.core.codec.internal.ByteWindow
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.map
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.ByteStringBuilder

private const val LINE: String = "\r\n"

private const val DASHES: String = "--"

private const val BOUNDARY: String = "boundary"

/**
 * Writes and reads `multipart/form-data` of RFC 7578.
 *
 * [encode] writes every part with its header fields in UTF-8 and refuses a part whose content holds the delimiter of
 * the boundary, which would end it early: with a boundary of [Boundary.random] this never happens by chance. [decode]
 * reads a whole body with a [MultipartDecoder] and its limits; a server that streams an upload uses the decoder
 * itself. The functions keep no state and are safe to call from any thread.
 *
 * @since 1.0.0
 */
public object MultipartFormData {
    /**
     * Returns the `Content-Type` of a body with [boundary].
     *
     * @param boundary the boundary.
     * @return `multipart/form-data` with the boundary.
     * @since 1.0.0
     */
    public fun contentType(boundary: Boundary): MediaType =
        MediaType.MultipartFormData.withParameter(name = BOUNDARY, value = boundary.value)

    /**
     * Returns the boundary [mediaType] names.
     *
     * @param mediaType the `Content-Type` of a received body.
     * @return the boundary, or `null` if the media type names none or a malformed one.
     * @since 1.0.0
     */
    public fun boundaryOf(mediaType: MediaType): Boundary? = mediaType.parameter(BOUNDARY)?.let(Boundary::parseOrNull)

    /**
     * Writes [parts] as a body with [boundary].
     *
     * @param parts the parts in order.
     * @param boundary the boundary, such as [Boundary.random].
     * @return the body.
     * @throws IllegalArgumentException if the content of a part holds the delimiter of [boundary].
     * @since 1.0.0
     */
    public fun encode(parts: List<MultipartPart>, boundary: Boundary = Boundary.random()): ByteString {
        val delimiter = "$LINE$DASHES${boundary.value}".encodeToByteArray()
        val out = ByteStringBuilder()
        parts.forEach { part ->
            val content = part.content.toByteArray()
            val window = ByteWindow()
            window.append(content)
            require(window.indexOf(delimiter) < 0) { "The content of a part holds the delimiter of its boundary" }
            val head = part.headers.joinToString("") { (name, value) -> "$name: $value$LINE" }
            out.append("$DASHES${boundary.value}$LINE$head$LINE".encodeToByteArray())
            out.append(content)
            out.append(LINE.encodeToByteArray())
        }
        out.append("$DASHES${boundary.value}$DASHES$LINE".encodeToByteArray())
        return out.toByteString()
    }

    /**
     * Reads a whole body with [boundary].
     *
     * @param body the body, which may come from an attacker.
     * @param boundary the boundary from the `Content-Type` of the body.
     * @param limits the limits of the body.
     * @return the parts in order, or the reason the body was refused.
     * @since 1.0.0
     */
    public fun decode(
        body: ByteString,
        boundary: Boundary,
        limits: MultipartLimits = MultipartLimits.Default,
    ): Outcome<List<MultipartPart>, MultipartFailure> {
        val decoder = MultipartDecoder(boundary, limits)
        val events = decoder.feed(body)
        return when (val ended = decoder.finish()) {
            is Outcome.Failure -> Outcome.Failure(ended.failure)
            is Outcome.Success -> events.map(::partsOf)
        }
    }

    private fun partsOf(events: List<MultipartEvent>): List<MultipartPart> {
        val parts = ArrayList<MultipartPart>()
        var headers: List<Pair<String, String>> = emptyList()
        var content = ByteStringBuilder()
        events.forEach { event ->
            (event as? MultipartEvent.Started)?.let { started -> headers = started.part.headers }
            (event as? MultipartEvent.Content)?.let { piece -> content.append(piece.bytes.toByteArray()) }
            if (event == MultipartEvent.Ended) {
                parts += MultipartPart(headers, content.toByteString())
                content = ByteStringBuilder()
            }
        }
        return parts
    }
}
