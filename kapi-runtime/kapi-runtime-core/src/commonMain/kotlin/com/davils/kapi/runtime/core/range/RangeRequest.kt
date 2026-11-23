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

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.conditional.IfRange
import com.davils.kapi.runtime.core.conditional.RepresentationState
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.FieldLists
import com.davils.kapi.runtime.core.internal.FieldScanner
import com.davils.kapi.runtime.core.range.internal.RangeNumbers

private const val UNIT: String = "bytes"

private const val DASH: Char = '-'

/**
 * The value of a `Range` field of RFC 9110 §14.2 for the unit `bytes`, such as `bytes=0-499, -500`.
 *
 * [resolve] decides what a server sends for a representation of a known length: the ranges that lie inside it,
 * coalesced into ascending order when any two overlap or touch, as RFC 9110 §14.2 allows against requests that would
 * make a server send the same bytes again and again. A range request is immutable.
 *
 * @param specs the ranges in the order the client asked for them.
 * @throws IllegalArgumentException if [specs] is empty.
 * @since 1.0.0
 */
public class RangeRequest(specs: List<ByteRangeSpec>) {
    /**
     * The ranges in the order the client asked for them.
     *
     * @since 1.0.0
     */
    public val specs: List<ByteRangeSpec> = specs.toList()

    init {
        require(specs.isNotEmpty()) { "A range request names at least one range" }
    }

    /**
     * Decides what to send for a representation of [completeLength] bytes by RFC 9110 §14.
     *
     * @param completeLength the length of the representation.
     * @param ifRange the `If-Range` of the request, or `null` if it has none.
     * @param state the current representation, which [ifRange] is evaluated against.
     * @return [RangeResolution.Full] if [ifRange] does not match, [RangeResolution.Partial] with the ranges that lie
     * inside the representation, or [RangeResolution.Unsatisfiable] if none does.
     * @throws IllegalArgumentException if [completeLength] is negative.
     * @since 1.0.0
     */
    public fun resolve(
        completeLength: Long,
        ifRange: IfRange? = null,
        state: RepresentationState = RepresentationState(),
    ): RangeResolution {
        require(completeLength >= 0) { "A representation has no negative length" }
        if (ifRange?.matches(state) == false) return RangeResolution.Full
        val ranges = specs.mapNotNull { spec -> inside(spec, completeLength) }
        val partial = RangeResolution.Partial(coalesced(ranges), completeLength)
        return partial.takeIf { ranges.isNotEmpty() } ?: RangeResolution.Unsatisfiable(completeLength)
    }

    override fun equals(other: Any?): Boolean = other is RangeRequest && specs == other.specs

    override fun hashCode(): Int = specs.hashCode()

    override fun toString(): String = "$UNIT=${specs.joinToString(",")}"

    private fun inside(spec: ByteRangeSpec, length: Long): ResolvedRange? = when (spec) {
        is ByteRangeSpec.FromTo -> spec.takeIf { range -> range.first < length }?.let { range ->
            ResolvedRange(range.first, minOf(range.last ?: (length - 1), length - 1))
        }
        is ByteRangeSpec.Suffix -> spec.takeIf { range -> range.length > 0 && length > 0 }?.let { range ->
            ResolvedRange(maxOf(0, length - range.length), length - 1)
        }
    }

    private fun coalesced(ranges: List<ResolvedRange>): List<ResolvedRange> {
        val sorted = ranges.sortedBy(ResolvedRange::first)
        val isOverlapping = sorted.zipWithNext().any { (left, right) -> right.first <= left.last + 1 }
        if (!isOverlapping) return ranges
        val merged = ArrayList<ResolvedRange>()
        sorted.forEach { range ->
            val previous = merged.lastOrNull()?.takeIf { last -> range.first <= last.last + 1 }
            if (previous == null) merged += range
            previous?.let { found -> merged[merged.lastIndex] = ResolvedRange(found.first, maxOf(found.last, range.last)) }
        }
        return merged
    }

    /**
     * Reads `Range`.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads the value of a `Range` field.
         *
         * The unit is compared without case; blanks around the commas and empty members are allowed.
         *
         * @param text the field value, which may come from an attacker.
         * @param limits the limits: the length of the value and [FieldLimits.maxRanges] ranges at most.
         * @return the request, or `null` if [text] has another unit, is malformed, names a range whose last byte lies
         * before its first, has more than 18 digits in a number or exceeds a limit; a server then ignores the field
         * and sends the whole representation.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String, limits: FieldLimits = FieldLimits.Default): RangeRequest? {
            val unit = text.substringBefore('=', "")
            val isBytes = unit.equals(UNIT, ignoreCase = true) && text.length <= limits.maxFieldLength
            val set = text.takeIf { isBytes }?.substring(unit.length + 1) ?: return null
            val specs = FieldLists.read(FieldScanner(set), limits.maxRanges, ::spec)
            return specs?.takeIf(List<ByteRangeSpec>::isNotEmpty)?.let(::RangeRequest)
        }

        private fun spec(scanner: FieldScanner): ByteRangeSpec? {
            val first = scanner.span(FieldGrammar::isDigit)
            val hasDash = scanner.consume(DASH)
            val last = scanner.span(FieldGrammar::isDigit)
            if (!hasDash) return null
            val suffix = last?.let(RangeNumbers::parse)?.let(ByteRangeSpec::Suffix)
            return first?.let { start -> fromTo(start, last) } ?: suffix.takeIf { first == null }
        }

        private fun fromTo(first: String, last: String?): ByteRangeSpec? {
            val start = RangeNumbers.parse(first)
            val end = last?.let(RangeNumbers::parse)
            val isValid = start != null && (last == null || (end != null && end >= start))
            if (!isValid) return null
            return ByteRangeSpec.FromTo(start, end)
        }
    }
}
