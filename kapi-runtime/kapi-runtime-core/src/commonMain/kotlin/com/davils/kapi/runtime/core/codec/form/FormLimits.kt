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

package com.davils.kapi.runtime.core.codec.form

import com.davils.kapi.runtime.core.internal.bounded
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The limits a reader of `application/x-www-form-urlencoded` keeps before it decodes a field.
 *
 * A body longer than [maxLength] is refused before it is read, and so is a body with more than [maxFields] fields or a
 * field whose name or value is longer than [maxFieldLength]; nothing is truncated silently. Every limit has a hard
 * ceiling. The limits are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class FormLimits internal constructor(builder: FormLimitsBuilder) : Validatable {
    /**
     * The most characters of a whole body, at most [HARD_LENGTH].
     *
     * @since 1.0.0
     */
    public val maxLength: Int = builder.maxLength

    /**
     * The most fields of a body, at most [HARD_FIELDS].
     *
     * @since 1.0.0
     */
    public val maxFields: Int = builder.maxFields

    /**
     * The most characters of the encoded name or value of one field, at most [HARD_LENGTH].
     *
     * @since 1.0.0
     */
    public val maxFieldLength: Int = builder.maxFieldLength

    override fun ValidationScope.validate() {
        bounded(::maxLength, HARD_LENGTH.toLong())
        bounded(::maxFields, HARD_FIELDS.toLong())
        bounded(::maxFieldLength, HARD_LENGTH.toLong())
    }

    override fun toString(): String = "FormLimits(length=$maxLength, fields=$maxFields, fieldLength=$maxFieldLength)"

    /**
     * The default limits and the hard ceilings.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The hard ceiling of [maxLength] and [maxFieldLength], 64 MiB.
         *
         * @since 1.0.0
         */
        public const val HARD_LENGTH: Int = 64 * 1_024 * 1_024

        /**
         * The hard ceiling of [maxFields].
         *
         * @since 1.0.0
         */
        public const val HARD_FIELDS: Int = 100_000

        /**
         * The default limits: 1 MiB, 1000 fields and 64 KiB per name or value.
         *
         * @since 1.0.0
         */
        public val Default: FormLimits = FormLimitsBuilder().build()
    }
}
