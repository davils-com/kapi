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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures [FieldLimits]; every limit starts at its default.
 *
 * A builder is not thread-safe; the limits it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class FieldLimitsBuilder internal constructor() : ValidatingBuilder<FieldLimits>() {
    override val subject: String get() = "field limits"

    /**
     * The most characters of one field value.
     *
     * @since 1.0.0
     */
    public var maxFieldLength: Int = DEFAULT_FIELD_LENGTH

    /**
     * The most members of a list or a dictionary.
     *
     * @since 1.0.0
     */
    public var maxMembers: Int = DEFAULT_MEMBERS

    /**
     * The most parameters of one member.
     *
     * @since 1.0.0
     */
    public var maxParameters: Int = DEFAULT_PARAMETERS

    /**
     * The most ranges of one `Range` field.
     *
     * @since 1.0.0
     */
    public var maxRanges: Int = DEFAULT_RANGES

    override fun assemble(): FieldLimits = FieldLimits(this)

    private companion object {
        const val DEFAULT_FIELD_LENGTH: Int = 8_192
        const val DEFAULT_MEMBERS: Int = 64
        const val DEFAULT_PARAMETERS: Int = 32
        const val DEFAULT_RANGES: Int = 16
    }
}

/**
 * Builds the limits of the field parsers of the runtime.
 *
 * @param configure changes the limits from their defaults.
 * @return the limits.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit is not positive or lies above its
 * hard ceiling.
 * @since 1.0.0
 */
public fun fieldLimits(configure: FieldLimitsBuilder.() -> Unit = {}): FieldLimits {
    val builder = FieldLimitsBuilder()
    builder.configure()
    return builder.build()
}
