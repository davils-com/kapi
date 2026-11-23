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

package com.davils.kapi.runtime.server

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.codec.multipart.MultipartLimits
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures [ServerLimits]; every limit starts at its default.
 *
 * A builder is not thread-safe; the limits it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ServerLimitsBuilder internal constructor() : ValidatingBuilder<ServerLimits>() {
    override val subject: String get() = "server limits"

    /**
     * See [ServerLimits.maxRequestBytes].
     *
     * @since 1.0.0
     */
    public var maxRequestBytes: Long = DEFAULT_REQUEST_BYTES

    /**
     * See [ServerLimits.maxUploadBytes].
     *
     * @since 1.0.0
     */
    public var maxUploadBytes: Long = DEFAULT_UPLOAD_BYTES

    /**
     * See [ServerLimits.maxDecompressedBytes].
     *
     * @since 1.0.0
     */
    public var maxDecompressedBytes: Long = DEFAULT_DECOMPRESSED_BYTES

    /**
     * See [ServerLimits.maxQueryLength].
     *
     * @since 1.0.0
     */
    public var maxQueryLength: Int = DEFAULT_QUERY_LENGTH

    /**
     * See [ServerLimits.maxQueryParameters].
     *
     * @since 1.0.0
     */
    public var maxQueryParameters: Int = DEFAULT_QUERY_PARAMETERS

    /**
     * See [ServerLimits.maxHeaders].
     *
     * @since 1.0.0
     */
    public var maxHeaders: Int = DEFAULT_HEADERS

    /**
     * See [ServerLimits.maxHeaderBytes].
     *
     * @since 1.0.0
     */
    public var maxHeaderBytes: Int = DEFAULT_HEADER_BYTES

    /**
     * See [ServerLimits.multipart].
     *
     * @since 1.0.0
     */
    public var multipart: MultipartLimits = MultipartLimits.Default

    /**
     * See [ServerLimits.fields].
     *
     * @since 1.0.0
     */
    public var fields: FieldLimits = FieldLimits.Default

    override fun assemble(): ServerLimits = ServerLimits(this)

    private companion object {
        const val DEFAULT_REQUEST_BYTES: Long = 4L * 1_024 * 1_024
        const val DEFAULT_UPLOAD_BYTES: Long = 64L * 1_024 * 1_024
        const val DEFAULT_DECOMPRESSED_BYTES: Long = 16L * 1_024 * 1_024
        const val DEFAULT_QUERY_LENGTH: Int = 8_192
        const val DEFAULT_QUERY_PARAMETERS: Int = 256
        const val DEFAULT_HEADERS: Int = 100
        const val DEFAULT_HEADER_BYTES: Int = 32 * 1_024
    }
}
