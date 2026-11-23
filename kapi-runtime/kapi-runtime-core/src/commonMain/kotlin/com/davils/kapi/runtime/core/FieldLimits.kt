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

package com.davils.kapi.runtime.core

import com.davils.kapi.runtime.core.internal.bounded
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The limits every parser of an HTTP field value of the runtime keeps before it does any work.
 *
 * A field value longer than [maxFieldLength] is refused before it is read, a list with more than [maxMembers]
 * members, an element with more than [maxParameters] parameters and a `Range` with more than [maxRanges] ranges are
 * refused as a whole: nothing is truncated silently. Every limit has a hard ceiling that no configuration can raise.
 * The limits are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class FieldLimits internal constructor(builder: FieldLimitsBuilder) : Validatable {
    /**
     * The most characters of one field value, at most [HARD_FIELD_LENGTH].
     *
     * @since 1.0.0
     */
    public val maxFieldLength: Int = builder.maxFieldLength

    /**
     * The most members of a list or a dictionary, at most [HARD_MEMBERS].
     *
     * @since 1.0.0
     */
    public val maxMembers: Int = builder.maxMembers

    /**
     * The most parameters of one member, at most [HARD_PARAMETERS].
     *
     * @since 1.0.0
     */
    public val maxParameters: Int = builder.maxParameters

    /**
     * The most ranges of one `Range` field, at most [HARD_RANGES].
     *
     * @since 1.0.0
     */
    public val maxRanges: Int = builder.maxRanges

    override fun ValidationScope.validate() {
        bounded(::maxFieldLength, HARD_FIELD_LENGTH.toLong())
        bounded(::maxMembers, HARD_MEMBERS.toLong())
        bounded(::maxParameters, HARD_PARAMETERS.toLong())
        bounded(::maxRanges, HARD_RANGES.toLong())
    }

    override fun toString(): String =
        "FieldLimits(fieldLength=$maxFieldLength, members=$maxMembers, parameters=$maxParameters, ranges=$maxRanges)"

    /**
     * The default limits and the hard ceilings.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The hard ceiling of [maxFieldLength].
         *
         * @since 1.0.0
         */
        public const val HARD_FIELD_LENGTH: Int = 65_536

        /**
         * The hard ceiling of [maxMembers].
         *
         * @since 1.0.0
         */
        public const val HARD_MEMBERS: Int = 1_024

        /**
         * The hard ceiling of [maxParameters].
         *
         * @since 1.0.0
         */
        public const val HARD_PARAMETERS: Int = 256

        /**
         * The hard ceiling of [maxRanges].
         *
         * @since 1.0.0
         */
        public const val HARD_RANGES: Int = 256

        /**
         * The default limits: 8192 characters, 64 members, 32 parameters and 16 ranges.
         *
         * @since 1.0.0
         */
        public val Default: FieldLimits = FieldLimitsBuilder().build()
    }
}
