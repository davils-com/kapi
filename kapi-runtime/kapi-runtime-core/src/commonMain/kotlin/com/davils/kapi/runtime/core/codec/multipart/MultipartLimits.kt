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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.core.codec.multipart

import com.davils.kapi.runtime.core.bounded
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The limits a reader of a multipart body keeps, such as the upload of a `multipart/form-data` form.
 *
 * The reader fails as soon as a limit is passed, before it buffers more: more than [maxParts] parts, a header block
 * of a part longer than [maxHeaderBytes] or with more than [maxHeaders] fields, a part larger than [maxPartBytes], a
 * body larger than [maxBodyBytes] or a preamble longer than [maxPreambleBytes]. Every limit has a hard ceiling. The
 * limits are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class MultipartLimits internal constructor(builder: MultipartLimitsBuilder) : Validatable {
    /**
     * The most parts of a body, at most [HARD_PARTS].
     *
     * @since 1.0.0
     */
    public val maxParts: Int = builder.maxParts

    /**
     * The most bytes of the header block of one part, at most [HARD_HEADER_BYTES].
     *
     * @since 1.0.0
     */
    public val maxHeaderBytes: Int = builder.maxHeaderBytes

    /**
     * The most header fields of one part, at most [HARD_HEADERS].
     *
     * @since 1.0.0
     */
    public val maxHeaders: Int = builder.maxHeaders

    /**
     * The most bytes of the content of one part, at most [HARD_PART_BYTES].
     *
     * @since 1.0.0
     */
    public val maxPartBytes: Long = builder.maxPartBytes

    /**
     * The most bytes of the whole body, at most [HARD_BODY_BYTES].
     *
     * @since 1.0.0
     */
    public val maxBodyBytes: Long = builder.maxBodyBytes

    /**
     * The most bytes before the first boundary, at most [HARD_HEADER_BYTES].
     *
     * @since 1.0.0
     */
    public val maxPreambleBytes: Int = builder.maxPreambleBytes

    override fun ValidationScope.validate() {
        bounded(::maxParts, HARD_PARTS.toLong())
        bounded(::maxHeaderBytes, HARD_HEADER_BYTES.toLong())
        bounded(::maxHeaders, HARD_HEADERS.toLong())
        bounded(::maxPartBytes, HARD_PART_BYTES)
        bounded(::maxBodyBytes, HARD_BODY_BYTES)
        bounded(::maxPreambleBytes, HARD_HEADER_BYTES.toLong())
    }

    override fun toString(): String = "MultipartLimits(parts=$maxParts, headerBytes=$maxHeaderBytes, " +
        "headers=$maxHeaders, partBytes=$maxPartBytes, bodyBytes=$maxBodyBytes, preambleBytes=$maxPreambleBytes)"

    /**
     * The default limits and the hard ceilings.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The hard ceiling of [maxParts].
         *
         * @since 1.0.0
         */
        public const val HARD_PARTS: Int = 10_000

        /**
         * The hard ceiling of [maxHeaderBytes] and [maxPreambleBytes], 64 KiB.
         *
         * @since 1.0.0
         */
        public const val HARD_HEADER_BYTES: Int = 64 * 1_024

        /**
         * The hard ceiling of [maxHeaders].
         *
         * @since 1.0.0
         */
        public const val HARD_HEADERS: Int = 256

        /**
         * The hard ceiling of [maxPartBytes], 64 GiB.
         *
         * @since 1.0.0
         */
        public const val HARD_PART_BYTES: Long = 64L * 1_024 * 1_024 * 1_024

        /**
         * The hard ceiling of [maxBodyBytes], 64 GiB.
         *
         * @since 1.0.0
         */
        public const val HARD_BODY_BYTES: Long = 64L * 1_024 * 1_024 * 1_024

        /**
         * The default limits: 100 parts, 8 KiB and 16 fields of headers per part, 16 MiB per part, 64 MiB per body
         * and 1 KiB of preamble.
         *
         * @since 1.0.0
         */
        public val Default: MultipartLimits = MultipartLimitsBuilder().build()
    }
}
