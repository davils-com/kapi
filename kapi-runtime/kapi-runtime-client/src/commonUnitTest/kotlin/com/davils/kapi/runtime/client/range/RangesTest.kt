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

import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.core.codec.multipart.Boundary
import com.davils.kapi.runtime.core.conditional.EntityTag
import com.davils.kapi.runtime.core.conditional.IfRange
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.parameter.ParameterEntry
import com.davils.kapi.runtime.core.range.ByteRangeSpec
import com.davils.kapi.runtime.core.range.ContentRange
import com.davils.kapi.runtime.core.range.MultipartByteRanges
import com.davils.kapi.runtime.core.range.RangeRequest
import com.davils.kapi.runtime.core.range.ResolvedRange
import com.davils.kapi.runtime.core.response.headersOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString

private const val PARTIAL: Int = 206

private const val FIRST: Long = 10

private const val LAST: Long = 14

private const val LENGTH: Long = 100

private const val CONTENT_RANGE: String = "Content-Range"

private const val OK: Int = 200

private const val NOT_FOUND: Int = 404

private const val NOT_SATISFIABLE: Int = 416

private const val OUTCOMES: Int = 5

private const val HELLO: String = "hello"

private const val MULTIPART: String = "multipart/byteranges; boundary=x"

private const val SECOND_FIRST: Long = 5

private const val CONTENT_TYPE: String = "Content-Type"

private const val CRLF: String = "\r\n"

private fun wire(vararg lines: String): String = lines.joinToString(separator = "") { line -> line + CRLF }

private val encodedParts: ByteString = MultipartByteRanges.encode(
    boundary = Boundary("x"),
    contentType = MediaType.TextPlain,
    completeLength = LENGTH,
    parts = listOf(
        ResolvedRange(first = 0, last = 1) to "ab".encodeToByteString(),
        ResolvedRange(first = SECOND_FIRST, last = SECOND_FIRST + 1) to "fg".encodeToByteString(),
    ),
)

private val PARTS: String = encodedParts.decodeToString()

private fun answered(status: Int, content: String, vararg headers: Pair<String, String>): Exchange.Answered =
    Exchange.Answered(
        status = status,
        headers = headersOf(headers.associate { (name, value) -> name to listOf(value) }),
        content = content.encodeToByteString(),
    )

class RangesTest : FunSpec({
    context("the range of a call") {
        test("asks for the byte ranges, and only while the representation is the one it names") {
            val call = apiCall(operation = "download", method = "GET") {
                val specs = listOf(ByteRangeSpec.FromTo(first = 0, last = FIRST - 1), ByteRangeSpec.Suffix(FIRST))
                range(RangeRequest(specs))
                ifRange(IfRange.Tag(EntityTag("v1")))
            }

            call.headers shouldBe listOf(
                ParameterEntry(name = "Range", value = "bytes=0-9,-10"),
                ParameterEntry(name = "If-Range", value = "\"v1\""),
            )
        }
    }

    context("how a call for a range ended") {
        test("is the part of the representation the server sent, when it is as long as it says") {
            val part = answered(PARTIAL, HELLO, CONTENT_RANGE to "bytes $FIRST-$LAST/$LENGTH").ranged()
            val short = answered(PARTIAL, "hell", CONTENT_RANGE to "bytes $FIRST-$LAST/$LENGTH").ranged()
            val bare = answered(PARTIAL, HELLO).ranged()

            val partial = part.shouldBeInstanceOf<RangeOutcome.Partial>()
            partial.range.completeLength shouldBe LENGTH
            partial.content shouldBe HELLO.encodeToByteString()
            partial.toString() shouldBe "Partial(bytes 10-14/100, 5 bytes)"
            short.shouldBeInstanceOf<RangeOutcome.Malformed>()
            bare.shouldBeInstanceOf<RangeOutcome.Malformed>()
        }

        test("of several ranges is read into its parts, each exactly as long as its range") {
            val ranges = answered(PARTIAL, PARTS, CONTENT_TYPE to MULTIPART).ranged()

            val parts = ranges.shouldBeInstanceOf<RangeOutcome.Ranges>().parts

            parts.map { part -> part.content.decodeToString() } shouldBe listOf("ab", "fg")
            parts.map { part -> part.range.range.first } shouldBe listOf(0L, SECOND_FIRST)
            ranges.toString() shouldContain "2 parts"
        }

        test("of several ranges is malformed when a part names no range, the wrong length or no boundary") {
            val unnamed = wire("--x", "Content-Type: text/plain", "", "ab", "--x--")
            val long = wire("--x", "Content-Range: bytes 0-0/100", "", "ab", "--x--")
            val bodies = listOf(
                unnamed to MULTIPART,
                long to MULTIPART,
                "" to MULTIPART,
                PARTS to "multipart/byteranges",
            )

            val outcomes = bodies.map { (body, type) -> answered(PARTIAL, body, CONTENT_TYPE to type).ranged() }

            outcomes.all { outcome -> outcome is RangeOutcome.Malformed } shouldBe true
        }

        test("is several ranges, the whole representation, none or another answer") {
            val multipart = answered(PARTIAL, PARTS, CONTENT_TYPE to MULTIPART).ranged()
            val full = answered(OK, "all").ranged()
            val none = answered(NOT_SATISFIABLE, "", CONTENT_RANGE to "bytes */$LENGTH").ranged()
            val unnamed = answered(NOT_SATISFIABLE, "").ranged()
            val other = answered(NOT_FOUND, "").ranged()
            val unsent = Exchange.Failed(CallFailure.TimedOut).ranged()

            multipart.shouldBeInstanceOf<RangeOutcome.Ranges>()
            full shouldBe RangeOutcome.Full(answered(OK, "all"))
            none shouldBe RangeOutcome.Unsatisfiable(LENGTH)
            unnamed shouldBe RangeOutcome.Unsatisfiable(null)
            other shouldBe RangeOutcome.Other(answered(NOT_FOUND, ""))
            unsent shouldBe RangeOutcome.Other(Exchange.Failed(CallFailure.TimedOut))
            none.toString() shouldBe "Unsatisfiable(100)"
            unnamed.toString() shouldBe "Unsatisfiable(unknown)"
            listOf(multipart, full, none, other).map { outcome -> outcome.toString().isNotEmpty() } shouldBe
                listOf(true, true, true, true)
            listOf(multipart, full, none, other, unsent).map { outcome -> outcome.hashCode() }.size shouldBe OUTCOMES
            multipart shouldBe answered(PARTIAL, PARTS, CONTENT_TYPE to MULTIPART).ranged()
            RangeOutcome.Malformed(answered(PARTIAL, "")) shouldBe RangeOutcome.Malformed(answered(PARTIAL, ""))
            RangeOutcome.Malformed(answered(PARTIAL, "")).hashCode() shouldBe answered(PARTIAL, "").hashCode()
            val malformed = RangeOutcome.Malformed(answered(PARTIAL, "")).toString()
            val single = ContentRange.Satisfied(ResolvedRange(first = 0, last = 0), completeLength = 1)

            malformed.startsWith("Malformed(") shouldBe true
            RangeOutcome.Partial(single, "x".encodeToByteString()).hashCode() shouldBe single.hashCode()
        }
    }
})
