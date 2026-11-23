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

import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.core.range.ContentRange
import kotlinx.io.bytestring.ByteString

private const val NONE: String = "unknown"

/**
 * How a call for a range of RFC 9110 §14 ended.
 *
 * @since 1.0.0
 */
public sealed interface RangeOutcome {
    /**
     * `206 Partial Content` with one range, whose content is exactly as long as its `Content-Range` says.
     *
     * @property range the range the content covers.
     * @property content the bytes of the range.
     * @since 1.0.0
     */
    public class Partial(public val range: ContentRange.Satisfied, public val content: ByteString) : RangeOutcome {
        override fun equals(other: Any?): Boolean = other is Partial && range == other.range && content == other.content

        override fun hashCode(): Int = range.hashCode()

        override fun toString(): String = "Partial($range, ${content.size} bytes)"
    }

    /**
     * `206 Partial Content` with several ranges as `multipart/byteranges`, to read with the multipart decoder of the
     * runtime core.
     *
     * @property answered the answer.
     * @since 1.0.0
     */
    public class Ranges(public val answered: Exchange.Answered) : RangeOutcome {
        override fun equals(other: Any?): Boolean = other is Ranges && answered == other.answered

        override fun hashCode(): Int = answered.hashCode()

        override fun toString(): String = "Ranges($answered)"
    }

    /**
     * A successful answer with the whole representation: the server ignored the range or `If-Range` did not match.
     *
     * @property answered the answer.
     * @since 1.0.0
     */
    public class Full(public val answered: Exchange.Answered) : RangeOutcome {
        override fun equals(other: Any?): Boolean = other is Full && answered == other.answered

        override fun hashCode(): Int = answered.hashCode()

        override fun toString(): String = "Full($answered)"
    }

    /**
     * `416 Range Not Satisfiable`: no range lies inside the representation.
     *
     * @property completeLength the length of the representation the server named, or `null`.
     * @since 1.0.0
     */
    public class Unsatisfiable(public val completeLength: Long?) : RangeOutcome {
        override fun equals(other: Any?): Boolean = other is Unsatisfiable && completeLength == other.completeLength

        override fun hashCode(): Int = completeLength.hashCode()

        override fun toString(): String = "Unsatisfiable(${completeLength ?: NONE})"
    }

    /**
     * `206 Partial Content` whose `Content-Range` is missing, malformed or does not match the length of the content;
     * the content is not trusted.
     *
     * @property answered the answer.
     * @since 1.0.0
     */
    public class Malformed(public val answered: Exchange.Answered) : RangeOutcome {
        override fun equals(other: Any?): Boolean = other is Malformed && answered == other.answered

        override fun hashCode(): Int = answered.hashCode()

        override fun toString(): String = "Malformed($answered)"
    }

    /**
     * Another status or no answer at all.
     *
     * @property exchange how the call ended.
     * @since 1.0.0
     */
    public class Other(public val exchange: Exchange) : RangeOutcome {
        override fun equals(other: Any?): Boolean = other is Other && exchange == other.exchange

        override fun hashCode(): Int = exchange.hashCode()

        override fun toString(): String = "Other($exchange)"
    }
}
