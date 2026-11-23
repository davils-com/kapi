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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures [FramingLimits]; every limit starts at its default.
 *
 * A builder is not thread-safe; the limits it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class FramingLimitsBuilder internal constructor() : ValidatingBuilder<FramingLimits>() {
    override val subject: String get() = "framing limits"

    /**
     * The most bytes of one record or line.
     *
     * @since 1.0.0
     */
    public var maxRecordBytes: Int = DEFAULT_BYTES

    /**
     * The most bytes of data of one server-sent event.
     *
     * @since 1.0.0
     */
    public var maxEventBytes: Int = DEFAULT_BYTES

    override fun assemble(): FramingLimits = FramingLimits(this)

    private companion object {
        const val DEFAULT_BYTES: Int = 1_024 * 1_024
    }
}

/**
 * Builds the limits of the readers of framed streams.
 *
 * @param configure changes the limits from their defaults.
 * @return the limits.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit is not positive or lies above its
 * hard ceiling.
 * @since 1.0.0
 */
public fun framingLimits(configure: FramingLimitsBuilder.() -> Unit = {}): FramingLimits {
    val builder = FramingLimitsBuilder()
    builder.configure()
    return builder.build()
}
