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

package com.davils.kapi.runtime.client.range

import com.davils.kapi.runtime.client.call.CallBuilder
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.core.codec.multipart.MultipartFormData
import com.davils.kapi.runtime.core.conditional.IfRange
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.range.ContentRange
import com.davils.kapi.runtime.core.range.RangeRequest
import com.davils.kapi.runtime.core.response.isSuccess
import com.davils.kore.core.outcome.Outcome
import kotlinx.io.bytestring.ByteString

private const val PARTIAL: Int = 206

private const val NOT_SATISFIABLE: Int = 416

private const val CONTENT_RANGE: String = "content-range"

/**
 * Asks for the byte ranges of [request] with `Range` of RFC 9110 §14.2, such as to resume a download.
 *
 * @receiver the call.
 * @param request the ranges.
 * @since 1.0.0
 */
public fun CallBuilder.range(request: RangeRequest) {
    header(name = "Range", value = request.toString())
}

/**
 * Sends `If-Range` of RFC 9110 §13.1.5, so the server sends the ranges only while the representation is the one of
 * [condition], and else the whole representation.
 *
 * @receiver the call.
 * @param condition the entity tag or the time of the representation the client holds part of.
 * @since 1.0.0
 */
public fun CallBuilder.ifRange(condition: IfRange) {
    header(name = "If-Range", value = condition.toString())
}

/**
 * Reads how a call for a range ended.
 *
 * A `multipart/byteranges` answer is read into its parts within the default multipart limits of the runtime core.
 *
 * @receiver how the call ended.
 * @return the outcome by the status, the media type and the `Content-Range` field of the answer.
 * @since 1.0.0
 */
public fun Exchange.ranged(): RangeOutcome {
    val answered = this as? Exchange.Answered ?: return RangeOutcome.Other(this)
    val range = answered.headers[CONTENT_RANGE]?.let(ContentRange::parseOrNull)
    val partial = partialOf(answered, range).takeIf { answered.status == PARTIAL }
    val length = (range as? ContentRange.Unsatisfied)?.completeLength
    val unsatisfiable = RangeOutcome.Unsatisfiable(length).takeIf { answered.status == NOT_SATISFIABLE }
    val full = RangeOutcome.Full(answered).takeIf { answered.isSuccess }
    return partial ?: unsatisfiable ?: full ?: RangeOutcome.Other(answered)
}

private fun partialOf(answered: Exchange.Answered, range: ContentRange?): RangeOutcome {
    val type = answered.contentType
    val isMultipart = type?.essence == MediaType.MultipartByteRanges.essence
    if (isMultipart) return rangesOf(answered, type)
    val partial = partOf(range, answered.content)
    return partial ?: RangeOutcome.Malformed(answered)
}

private fun rangesOf(answered: Exchange.Answered, type: MediaType?): RangeOutcome {
    val boundary = type?.let(MultipartFormData::boundaryOf)
    val decoded = boundary?.let { found -> MultipartFormData.decode(answered.content, found) }
    val parts = (decoded as? Outcome.Success)?.value.orEmpty()
    val partials = parts.mapNotNull { part ->
        val range = part.header(CONTENT_RANGE)?.let(ContentRange::parseOrNull)
        partOf(range, part.content)
    }
    val isWhole = partials.isNotEmpty() && partials.size == parts.size
    return RangeOutcome.Ranges(answered, partials).takeIf { isWhole } ?: RangeOutcome.Malformed(answered)
}

private fun partOf(range: ContentRange?, content: ByteString): RangeOutcome.Partial? {
    val size = content.size.toLong()
    val satisfied = (range as? ContentRange.Satisfied)?.takeIf { found -> found.range.length == size }
    return satisfied?.let { found -> RangeOutcome.Partial(found, content) }
}
