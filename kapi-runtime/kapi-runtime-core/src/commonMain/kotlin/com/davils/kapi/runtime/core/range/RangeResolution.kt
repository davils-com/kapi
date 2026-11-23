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

/**
 * What a server sends for a request with a `Range` field, by RFC 9110 §14.
 *
 * @since 1.0.0
 */
public sealed interface RangeResolution {
    /**
     * Ignore the range and send the whole representation with 200 (OK).
     *
     * @since 1.0.0
     */
    public object Full : RangeResolution {
        override fun toString(): String = "Full"
    }

    /**
     * Send [ranges] with 206 (Partial Content): one range with `Content-Range`, several as `multipart/byteranges`.
     *
     * @param ranges the ranges in the order to send them.
     * @property completeLength the length of the whole representation.
     * @since 1.0.0
     */
    public class Partial(ranges: List<ResolvedRange>, public val completeLength: Long) : RangeResolution {
        /**
         * The ranges in the order to send them.
         *
         * @since 1.0.0
         */
        public val ranges: List<ResolvedRange> = ranges.toList()

        /**
         * The `Content-Range` of a response with a single range, or `null` when several are sent.
         *
         * @since 1.0.0
         */
        public val contentRange: ContentRange?
            get() = ranges.singleOrNull()?.let { range -> ContentRange.Satisfied(range, completeLength) }

        override fun equals(other: Any?): Boolean =
            other is Partial && ranges == other.ranges && completeLength == other.completeLength

        override fun hashCode(): Int = listOf(ranges, completeLength).hashCode()

        override fun toString(): String = "Partial($ranges of $completeLength)"
    }

    /**
     * Answer with 416 (Range Not Satisfiable) and [contentRange], since no range lies inside the representation.
     *
     * @property completeLength the length of the whole representation.
     * @since 1.0.0
     */
    public class Unsatisfiable(public val completeLength: Long) : RangeResolution {
        /**
         * The `Content-Range` of the answer, such as `bytes *&#47;1234`.
         *
         * @since 1.0.0
         */
        public val contentRange: ContentRange get() = ContentRange.Unsatisfied(completeLength)

        override fun equals(other: Any?): Boolean = other is Unsatisfiable && completeLength == other.completeLength

        override fun hashCode(): Int = completeLength.hashCode()

        override fun toString(): String = "Unsatisfiable($completeLength)"
    }
}
