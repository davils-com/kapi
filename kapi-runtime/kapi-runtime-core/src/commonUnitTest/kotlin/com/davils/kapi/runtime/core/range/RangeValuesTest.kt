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

import com.davils.kapi.runtime.core.fieldLimits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val LENGTH: Long = 100L

private const val FIRST: Long = 10L

private const val LAST: Long = 20L

private const val FAR: Long = 50L

private const val LONGEST_CONTENT_RANGE: Int = 128

private const val OVERFLOWING: String = "99999999999999999999"

private val range: ResolvedRange = ResolvedRange(first = FIRST, last = LAST)

private val refusedContentRanges: List<String> = listOf(
    "bytes 1-/10",
    "bytes 5-1/10",
    "bytes 1-x/10",
    "bytes 0-1/x",
    "bytes 0-10/10",
    "bytes */x",
    "bytes 0-1/" + "1".repeat(LONGEST_CONTENT_RANGE),
)

private val refusedRequests: List<String> = listOf(
    "bytes=-$OVERFLOWING",
    "bytes=$OVERFLOWING-",
    "bytes=1-$OVERFLOWING",
    "bytes=5-1",
)

private fun request(text: String): RangeRequest = RangeRequest.parseOrNull(text).shouldNotBeNull()

class RangeValuesTest : FunSpec({
    context("Content-Range") {
        refusedContentRanges.forEach { text ->
            test("refuses '${text.take(LONGEST_CONTENT_RANGE / 2)}'") {
                ContentRange.parseOrNull(text).shouldBeNull()
            }
        }

        test("reads a range of an unknown length") {
            ContentRange.parseOrNull("bytes 0-1/*") shouldBe ContentRange.Satisfied(ResolvedRange(first = 0L, last = 1L))
        }

        test("is equal by its range and its length") {
            val satisfied = ContentRange.Satisfied(range, LENGTH)

            satisfied shouldBe ContentRange.Satisfied(range, LENGTH)
            satisfied.hashCode() shouldBe ContentRange.Satisfied(range, LENGTH).hashCode()
            satisfied shouldNotBe ContentRange.Satisfied(range)
            satisfied shouldNotBe ContentRange.Satisfied(ResolvedRange(first = 0L, last = LAST), LENGTH)
            satisfied.equals(range) shouldBe false
            ContentRange.Unsatisfied(LENGTH) shouldBe ContentRange.Unsatisfied(LENGTH)
            ContentRange.Unsatisfied(LENGTH) shouldNotBe ContentRange.Unsatisfied(FAR)
            ContentRange.Unsatisfied(LENGTH).equals(satisfied) shouldBe false
        }
    }

    context("Range") {
        refusedRequests.forEach { text ->
            test("refuses '$text'") {
                RangeRequest.parseOrNull(text).shouldBeNull()
            }
        }

        test("refuses a field above the limit of its length") {
            RangeRequest.parseOrNull("bytes=0-1", fieldLimits { maxFieldLength = 8 }).shouldBeNull()
        }

        test("leaves a range apart that does not touch the merged ranges") {
            val resolution = request("bytes=0-1,1-2,10-12").resolve(LENGTH)

            resolution shouldBe RangeResolution.Partial(
                listOf(ResolvedRange(first = 0L, last = 2L), ResolvedRange(first = FIRST, last = 12L)),
                LENGTH,
            )
        }

        test("cannot satisfy a suffix of nothing or of an empty representation") {
            request("bytes=-0").resolve(LENGTH) shouldBe RangeResolution.Unsatisfiable(LENGTH)
            request("bytes=-5").resolve(0L) shouldBe RangeResolution.Unsatisfiable(0L)
        }

        test("is equal by its ranges") {
            val ranges = request("bytes=1-2")

            ranges.equals(range) shouldBe false
            ranges shouldNotBe request("bytes=1-3")
        }
    }

    context("the ranges") {
        test("run forward from byte 0 on") {
            shouldThrow<IllegalArgumentException> { ByteRangeSpec.FromTo(first = -1L) }
            shouldThrow<IllegalArgumentException> { ByteRangeSpec.FromTo(first = LAST, last = FIRST) }
            shouldThrow<IllegalArgumentException> { ResolvedRange(first = -1L, last = FIRST) }
            shouldThrow<IllegalArgumentException> { ResolvedRange(first = LAST, last = FIRST) }
        }

        test("are equal by their bounds") {
            val fromTo = ByteRangeSpec.FromTo(first = FIRST, last = LAST)

            fromTo shouldNotBe ByteRangeSpec.FromTo(first = 0L, last = LAST)
            fromTo shouldNotBe ByteRangeSpec.FromTo(first = FIRST)
            fromTo.equals(ByteRangeSpec.Suffix(FIRST)) shouldBe false
            ByteRangeSpec.Suffix(FIRST) shouldBe ByteRangeSpec.Suffix(FIRST)
            ByteRangeSpec.Suffix(FIRST) shouldNotBe ByteRangeSpec.Suffix(LAST)
            ByteRangeSpec.Suffix(FIRST).equals(fromTo) shouldBe false
            range shouldNotBe ResolvedRange(first = 0L, last = LAST)
            range shouldNotBe ResolvedRange(first = FIRST, last = FAR)
            range.equals(fromTo) shouldBe false
        }

        test("resolve to results that are equal by their ranges") {
            val partial = RangeResolution.Partial(listOf(range), LENGTH)

            partial shouldNotBe RangeResolution.Partial(listOf(range), FAR)
            partial shouldNotBe RangeResolution.Partial(emptyList(), LENGTH)
            partial.equals(RangeResolution.Full) shouldBe false
            RangeResolution.Unsatisfiable(LENGTH) shouldNotBe RangeResolution.Unsatisfiable(FAR)
            RangeResolution.Unsatisfiable(LENGTH).equals(partial) shouldBe false
        }
    }
})
