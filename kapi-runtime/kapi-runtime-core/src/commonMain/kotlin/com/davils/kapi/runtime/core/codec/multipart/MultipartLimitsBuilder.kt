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

package com.davils.kapi.runtime.core.codec.multipart

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures [MultipartLimits]; every limit starts at its default.
 *
 * A builder is not thread-safe; the limits it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class MultipartLimitsBuilder internal constructor() : ValidatingBuilder<MultipartLimits>() {
    override val subject: String get() = "multipart limits"

    /**
     * The most parts of a body.
     *
     * @since 1.0.0
     */
    public var maxParts: Int = DEFAULT_PARTS

    /**
     * The most bytes of the header block of one part.
     *
     * @since 1.0.0
     */
    public var maxHeaderBytes: Int = DEFAULT_HEADER_BYTES

    /**
     * The most header fields of one part.
     *
     * @since 1.0.0
     */
    public var maxHeaders: Int = DEFAULT_HEADERS

    /**
     * The most bytes of the content of one part.
     *
     * @since 1.0.0
     */
    public var maxPartBytes: Long = DEFAULT_PART_BYTES

    /**
     * The most bytes of the whole body.
     *
     * @since 1.0.0
     */
    public var maxBodyBytes: Long = DEFAULT_BODY_BYTES

    /**
     * The most bytes before the first boundary.
     *
     * @since 1.0.0
     */
    public var maxPreambleBytes: Int = DEFAULT_PREAMBLE_BYTES

    override fun assemble(): MultipartLimits = MultipartLimits(this)

    private companion object {
        const val DEFAULT_PARTS: Int = 100
        const val DEFAULT_HEADER_BYTES: Int = 8 * 1_024
        const val DEFAULT_HEADERS: Int = 16
        const val DEFAULT_PART_BYTES: Long = 16L * 1_024 * 1_024
        const val DEFAULT_BODY_BYTES: Long = 64L * 1_024 * 1_024
        const val DEFAULT_PREAMBLE_BYTES: Int = 1_024
    }
}

/**
 * Builds the limits of a reader of multipart bodies.
 *
 * @param configure changes the limits from their defaults.
 * @return the limits.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit is not positive or lies above its
 * hard ceiling.
 * @since 1.0.0
 */
public fun multipartLimits(configure: MultipartLimitsBuilder.() -> Unit = {}): MultipartLimits {
    val builder = MultipartLimitsBuilder()
    builder.configure()
    return builder.build()
}
