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
import com.davils.kapi.runtime.core.conditional.EntityTag
import com.davils.kapi.runtime.core.conditional.IfRange
import com.davils.kapi.runtime.core.conditional.RepresentationState
import com.davils.kapi.runtime.core.fieldLimits
import com.davils.kapi.runtime.core.negotiation.MediaType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString

private const val LENGTH: Long = 10_000L

private const val SMALL: Long = 50L

private const val LAST_BYTE: Long = 9_999L

private const val TOO_MANY: Int = 17

private const val FIRST_HALF: Long = 499L

private const val SECOND_HALF: Long = 999L

private const val SUFFIX: Long = 9_500L

private const val ETAG: String = "\"v1\""

private fun request(text: String): RangeRequest = RangeRequest.parseOrNull(text).shouldNotBeNull()

private fun resolved(text: String, length: Long = LENGTH): RangeResolution = request(text).resolve(length)

private fun partial(vararg ranges: ResolvedRange, length: Long = LENGTH): RangeResolution =
    RangeResolution.Partial(ranges.toList(), length)

class RangeRequestTest : FunSpec({
    context("the examples of RFC 9110 §14.1.2") {
        test("resolve against a representation of 10000 bytes") {
            resolved("bytes=0-499") shouldBe partial(ResolvedRange(0L, FIRST_HALF))
            resolved("bytes=500-999") shouldBe partial(ResolvedRange(FIRST_HALF + 1, SECOND_HALF))
            resolved("bytes=-500") shouldBe partial(ResolvedRange(SUFFIX, LAST_BYTE))
            resolved("bytes=9500-") shouldBe partial(ResolvedRange(SUFFIX, LAST_BYTE))
            resolved("bytes=0-0,-1") shouldBe partial(ResolvedRange(0L, 0L), ResolvedRange(LAST_BYTE, LAST_BYTE))
        }

        test("write the content ranges of RFC 9110 §14.4") {
            ContentRange.Satisfied(ResolvedRange(42L, 1_233L), 1_234L).toString() shouldBe "bytes 42-1233/1234"
            ContentRange.Satisfied(ResolvedRange(42L, 1_233L)).toString() shouldBe "bytes 42-1233/*"
            ContentRange.Unsatisfied(1_234L).toString() shouldBe "bytes */1234"
            listOf("bytes 42-1233/1234", "bytes 42-1233/*", "bytes */1234").forEach { text ->
                ContentRange.parseOrNull(text).toString() shouldBe text
            }
        }
    }

    context("resolve") {
        test("clamps the last byte, drops ranges outside and answers 416 when none is left") {
            resolved("bytes=40-100", SMALL) shouldBe partial(ResolvedRange(40L, SMALL - 1), length = SMALL)
            resolved("bytes=40-, 60-70", SMALL) shouldBe partial(ResolvedRange(40L, SMALL - 1), length = SMALL)
            resolved("bytes=-100", SMALL) shouldBe partial(ResolvedRange(0L, SMALL - 1), length = SMALL)
            resolved("bytes=60-70", SMALL) shouldBe RangeResolution.Unsatisfiable(SMALL)
            resolved("bytes=-0", SMALL) shouldBe RangeResolution.Unsatisfiable(SMALL)
            resolved("bytes=0-", 0L) shouldBe RangeResolution.Unsatisfiable(0L)
            RangeResolution.Unsatisfiable(SMALL).contentRange.toString() shouldBe "bytes */50"
        }

        test("coalesces ranges that overlap or touch into ascending order") {
            resolved("bytes=20-30, 0-10, 5-19") shouldBe partial(ResolvedRange(0L, 30L))
            resolved("bytes=40-45, 0-10") shouldBe partial(ResolvedRange(40L, 45L), ResolvedRange(0L, 10L))
            resolved("bytes=0-1, 0-1, 0-1") shouldBe partial(ResolvedRange(0L, 1L))
        }

        test("sends the whole representation when If-Range does not match") {
            val state = RepresentationState(EntityTag("v1"))
            val single = request("bytes=0-1")

            single.resolve(LENGTH, IfRange.parseOrNull(ETAG), state).shouldBeInstanceOf<RangeResolution.Partial>()
            single.resolve(LENGTH, IfRange.parseOrNull("\"v2\""), state) shouldBe RangeResolution.Full
            single.resolve(LENGTH, IfRange.parseOrNull("W/$ETAG"), state) shouldBe RangeResolution.Full
        }

        test("names the content range of a single range") {
            val single = resolved("bytes=0-499").shouldBeInstanceOf<RangeResolution.Partial>()
            val multiple = resolved("bytes=0-1,5-6").shouldBeInstanceOf<RangeResolution.Partial>()

            single.contentRange.toString() shouldBe "bytes 0-499/10000"
            multiple.contentRange.shouldBeNull()
            shouldThrow<IllegalArgumentException> { request("bytes=0-1").resolve(-1L) }
        }
    }

    context("parsing") {
        test("refuses another unit, malformed ranges and numbers that do not fit") {
            val refused = listOf(
                "", "bytes", "bytes=", "items=0-1", "bytes=a-b", "bytes=500-100", "bytes=-", "bytes=1", "bytes=1-2-3",
                "bytes=0-1;x", "bytes=1234567890123456789-", "bytes = 0-1", "bytes=0-1\r\nX: y",
            )
            refused.forEach { text -> RangeRequest.parseOrNull(text).shouldBeNull() }
        }

        test("reads the unit without case and blanks around commas") {
            request("BYTES=0-1 ,\t2-3,,") shouldBe
                RangeRequest(listOf(ByteRangeSpec.FromTo(0L, 1L), ByteRangeSpec.FromTo(2L, 3L)))
            request("bytes=0-1,-5,7-").toString() shouldBe "bytes=0-1,-5,7-"
        }

        test("reads as many ranges as the limit allows and refuses one more") {
            val ranges = List(TOO_MANY) { index -> "$index-$index" }

            RangeRequest.parseOrNull("bytes=" + ranges.dropLast(1).joinToString(",")).shouldNotBeNull()
            RangeRequest.parseOrNull("bytes=" + ranges.joinToString(",")).shouldBeNull()
            RangeRequest.parseOrNull("bytes=0-1,2-3", fieldLimits { maxRanges = 1 }).shouldBeNull()
        }

        test("refuses a content range that is malformed or outside its length") {
            listOf("bytes 5-1/10", "bytes 0-10/10", "items 0-1/2", "bytes 0-1", "bytes */*", "bytes -1/2").forEach { text ->
                ContentRange.parseOrNull(text).shouldBeNull()
            }
            shouldThrow<IllegalArgumentException> { ContentRange.Satisfied(ResolvedRange(0L, SMALL), SMALL) }
            shouldThrow<IllegalArgumentException> { ContentRange.Unsatisfied(-1L) }
        }
    }

    context("the types") {
        test("refuse ranges that run backwards") {
            shouldThrow<IllegalArgumentException> { ByteRangeSpec.FromTo(SMALL, 1L) }
            shouldThrow<IllegalArgumentException> { ByteRangeSpec.Suffix(-1L) }
            shouldThrow<IllegalArgumentException> { ResolvedRange(SMALL, 1L) }
            shouldThrow<IllegalArgumentException> { RangeRequest(emptyList()) }
        }

        test("are equal by their values") {
            ResolvedRange(0L, 1L).length shouldBe 2L
            ResolvedRange(0L, 1L).hashCode() shouldBe ResolvedRange(0L, 1L).hashCode()
            ByteRangeSpec.FromTo(0L).hashCode() shouldBe ByteRangeSpec.FromTo(0L).hashCode()
            ByteRangeSpec.Suffix(1L).hashCode() shouldBe ByteRangeSpec.Suffix(1L).hashCode()
            request("bytes=0-1").hashCode() shouldBe request("bytes=0-1").hashCode()
            partial(ResolvedRange(0L, 1L)).hashCode() shouldBe partial(ResolvedRange(0L, 1L)).hashCode()
            RangeResolution.Unsatisfiable(1L).hashCode() shouldBe RangeResolution.Unsatisfiable(1L).hashCode()
            ContentRange.Unsatisfied(1L).hashCode() shouldBe ContentRange.Unsatisfied(1L).hashCode()
            RangeResolution.Full.toString() shouldBe "Full"
            partial(ResolvedRange(0L, 1L)).toString() shouldBe "Partial([0-1] of 10000)"
            RangeResolution.Unsatisfiable(1L).toString() shouldBe "Unsatisfiable(1)"
        }
    }

    context("multipart/byteranges") {
        test("writes every part with its type and range") {
            val boundary = Boundary("THIS_STRING_SEPARATES")
            val body = MultipartByteRanges.encode(
                boundary = boundary,
                contentType = MediaType.parse("application/pdf"),
                completeLength = LENGTH,
                parts = listOf(
                    ResolvedRange(0L, 1L) to "ab".encodeToByteString(),
                    ResolvedRange(LAST_BYTE, LAST_BYTE) to "z".encodeToByteString(),
                ),
            )

            MultipartByteRanges.contentType(boundary).toString() shouldBe
                "multipart/byteranges; boundary=THIS_STRING_SEPARATES"
            body.decodeToString() shouldBe "--THIS_STRING_SEPARATES\r\nContent-Type: application/pdf\r\n" +
                "Content-Range: bytes 0-1/10000\r\n\r\nab\r\n--THIS_STRING_SEPARATES\r\n" +
                "Content-Type: application/pdf\r\nContent-Range: bytes 9999-9999/10000\r\n\r\nz\r\n" +
                "--THIS_STRING_SEPARATES--\r\n"
        }

        test("refuses parts that do not fit their ranges") {
            val type = MediaType.OctetStream
            val boundary = Boundary.random()

            shouldThrow<IllegalArgumentException> {
                MultipartByteRanges.encode(boundary, type, LENGTH, emptyList())
            }
            shouldThrow<IllegalArgumentException> {
                MultipartByteRanges.encode(boundary, type, LENGTH, listOf(ResolvedRange(0L, 1L) to ByteString()))
            }
            shouldThrow<IllegalArgumentException> {
                MultipartByteRanges.encode(boundary, type, 1L, listOf(ResolvedRange(0L, 1L) to "ab".encodeToByteString()))
            }
        }
    }
})
