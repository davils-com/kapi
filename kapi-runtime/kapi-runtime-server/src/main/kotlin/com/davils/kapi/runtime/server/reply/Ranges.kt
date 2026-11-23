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

package com.davils.kapi.runtime.server.reply

import com.davils.kapi.runtime.core.codec.multipart.Boundary
import com.davils.kapi.runtime.core.conditional.IfRange
import com.davils.kapi.runtime.core.conditional.RepresentationState
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.range.MultipartByteRanges
import com.davils.kapi.runtime.core.range.RangeRequest
import com.davils.kapi.runtime.core.range.RangeResolution
import com.davils.kapi.runtime.core.range.ResolvedRange
import com.davils.kapi.runtime.core.response.HttpStatus
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.request.Request
import kotlinx.io.bytestring.ByteString

private const val GET: String = "GET"

private const val RANGE: String = "range"

private const val IF_RANGE: String = "if-range"

private const val ACCEPT_RANGES: String = "Accept-Ranges"

private const val CONTENT_RANGE: String = "Content-Range"

private const val BYTES: String = "bytes"

/**
 * Answers a `GET` of [content] with the ranges of RFC 9110 §14 the request asks for.
 *
 * A request without `Range`, with a `Range` that is malformed, of another unit or beyond the field limits of the
 * server, with an `If-Range` that does not match [state] or is malformed, and every method but `GET` get the whole
 * content with 200 (OK). Ranges inside the content are answered with 206 (Partial Content): one range with
 * `Content-Range`, several as `multipart/byteranges` with a random boundary, overlapping ranges coalesced so no
 * client can make the server send the same bytes again and again. A `Range` of which no range lies inside the content
 * is answered with a problem with status 416 (Range Not Satisfiable) and `Content-Range: bytes &#42;/length`. Every
 * answer but the problem carries `Accept-Ranges: bytes` and the validators of [state].
 *
 * @receiver the request.
 * @param content the whole representation.
 * @param mediaType the media type of the representation.
 * @param state the validators of the representation, which `If-Range` is compared with.
 * @return the reply.
 * @since 1.0.0
 */
public fun Request.ranged(
    content: ByteString,
    mediaType: MediaType,
    state: RepresentationState = RepresentationState(),
): Reply {
    val headers = listOf(ACCEPT_RANGES to BYTES) + validatorsOf(state)
    val full = Reply(status = HttpStatus.OK, headers = headers, body = ReplyBody.Bytes(mediaType, content))
    val resolution = resolutionOf(this, content.size.toLong(), state)
    return when (resolution) {
        null, RangeResolution.Full -> full

        is RangeResolution.Partial -> partial(
            resolution = resolution,
            content = content,
            mediaType = mediaType,
            headers = headers,
        )

        is RangeResolution.Unsatisfiable -> context.problems.problem(
            status = HttpStatus.RANGE_NOT_SATISFIABLE,
            code = ServerCodes.RANGE_NOT_SATISFIABLE,
            headers = listOf(CONTENT_RANGE to resolution.contentRange.toString()),
        )
    }
}

private fun resolutionOf(request: Request, length: Long, state: RepresentationState): RangeResolution? {
    val text = joined(request, RANGE)?.takeIf { request.method == GET }
    val range = text?.let { value -> RangeRequest.parseOrNull(value, request.context.limits.fields) }
    val ifRangeText = request.headers[IF_RANGE]
    val ifRange = ifRangeText?.let { value -> IfRange.parseOrNull(value, request.context.now()) }
    val isIfRangeBroken = ifRangeText != null && ifRange == null
    return range?.takeUnless { isIfRangeBroken }?.resolve(completeLength = length, ifRange = ifRange, state = state)
}

private fun partial(
    resolution: RangeResolution.Partial,
    content: ByteString,
    mediaType: MediaType,
    headers: List<Pair<String, String>>,
): Reply {
    val single = resolution.contentRange
    if (single != null) {
        val body = ReplyBody.Bytes(mediaType, slice(content, resolution.ranges.single()))
        return Reply(HttpStatus.PARTIAL_CONTENT, headers + (CONTENT_RANGE to single.toString()), body)
    }
    val boundary = Boundary.random()
    val encoded = MultipartByteRanges.encode(
        boundary = boundary,
        contentType = mediaType,
        completeLength = resolution.completeLength,
        parts = resolution.ranges.map { range -> range to slice(content, range) },
    )
    val body = ReplyBody.Bytes(MultipartByteRanges.contentType(boundary), encoded)
    return Reply(HttpStatus.PARTIAL_CONTENT, headers, body)
}

private fun slice(content: ByteString, range: ResolvedRange): ByteString =
    content.substring(startIndex = range.first.toInt(), endIndex = (range.last + 1).toInt())
