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

package com.davils.kapi.runtime.core.framing

import com.davils.kapi.runtime.core.internal.bounded
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The limits the readers of framed streams keep: JSON Lines, JSON text sequences and server-sent events.
 *
 * A record or a line longer than [maxRecordBytes] and an event whose data grows beyond [maxEventBytes] fail the
 * stream instead of being cut; a reader never buffers more than these limits. Every limit has a hard ceiling. The
 * limits are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class FramingLimits internal constructor(builder: FramingLimitsBuilder) : Validatable {
    /**
     * The most bytes of one record of JSON Lines or of a JSON text sequence, and of one line of an event stream, at
     * most [HARD_BYTES].
     *
     * @since 1.0.0
     */
    public val maxRecordBytes: Int = builder.maxRecordBytes

    /**
     * The most bytes the data of one server-sent event may gather, at most [HARD_BYTES].
     *
     * @since 1.0.0
     */
    public val maxEventBytes: Int = builder.maxEventBytes

    override fun ValidationScope.validate() {
        bounded(::maxRecordBytes, HARD_BYTES.toLong())
        bounded(::maxEventBytes, HARD_BYTES.toLong())
    }

    override fun toString(): String = "FramingLimits(record=$maxRecordBytes, event=$maxEventBytes)"

    /**
     * The default limits and the hard ceiling.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The hard ceiling of every limit, 64 MiB.
         *
         * @since 1.0.0
         */
        public const val HARD_BYTES: Int = 64 * 1_024 * 1_024

        /**
         * The default limits: 1 MiB per record or line and 1 MiB of data per event.
         *
         * @since 1.0.0
         */
        public val Default: FramingLimits = FramingLimitsBuilder().build()
    }
}
