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

package com.davils.kapi.runtime.server

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.bounded
import com.davils.kapi.runtime.core.codec.multipart.MultipartLimits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The limits a server keeps before it does any work on a request.
 *
 * A request whose header fields are more than [maxHeaders] or longer than [maxHeaderBytes] together is refused with
 * 431 (Request Header Fields Too Large), a query longer than [maxQueryLength] with 414 (URI Too Long), a query with
 * more than [maxQueryParameters] fields with 400 (Bad Request), a body that is read as a whole and is larger than
 * [maxRequestBytes], or that inflates to more than [maxDecompressedBytes], and an upload that streams more than
 * [maxUploadBytes] with 413 (Content Too Large). Uploads of `multipart/form-data` keep [multipart] and the parsers of
 * header fields keep [fields]. Nothing is truncated silently. Every limit has a hard ceiling. The limits are immutable
 * and safe to share between threads.
 *
 * @since 1.0.0
 */
public class ServerLimits internal constructor(builder: ServerLimitsBuilder) : Validatable {
    /**
     * The most bytes of a body read as a whole, and of a compressed body, at most [HARD_REQUEST_BYTES].
     *
     * @since 1.0.0
     */
    public val maxRequestBytes: Long = builder.maxRequestBytes

    /**
     * The most bytes of an upload that is streamed rather than read as a whole, at most [HARD_UPLOAD_BYTES].
     *
     * @since 1.0.0
     */
    public val maxUploadBytes: Long = builder.maxUploadBytes

    /**
     * The most bytes a compressed body may inflate to, at most [HARD_REQUEST_BYTES].
     *
     * @since 1.0.0
     */
    public val maxDecompressedBytes: Long = builder.maxDecompressedBytes

    /**
     * The most characters of the query of a request, at most [HARD_QUERY_LENGTH].
     *
     * @since 1.0.0
     */
    public val maxQueryLength: Int = builder.maxQueryLength

    /**
     * The most fields of the query of a request, at most [HARD_QUERY_PARAMETERS].
     *
     * @since 1.0.0
     */
    public val maxQueryParameters: Int = builder.maxQueryParameters

    /**
     * The most header fields of a request, at most [HARD_HEADERS].
     *
     * @since 1.0.0
     */
    public val maxHeaders: Int = builder.maxHeaders

    /**
     * The most bytes of the names and values of the header fields of a request together, at most
     * [HARD_HEADER_BYTES].
     *
     * @since 1.0.0
     */
    public val maxHeaderBytes: Int = builder.maxHeaderBytes

    /**
     * The limits of a body of `multipart/form-data`.
     *
     * @since 1.0.0
     */
    public val multipart: MultipartLimits = builder.multipart

    /**
     * The limits of the parsers of header fields, such as `Accept`, `Range` and `If-None-Match`.
     *
     * @since 1.0.0
     */
    public val fields: FieldLimits = builder.fields

    /**
     * The most requests one operation serves at once, at most [HARD_CONCURRENT_REQUESTS]; a bulkhead of kontinuity
     * refuses every further request at once with `503 Service Unavailable` and the code
     * [com.davils.kapi.runtime.server.problem.ServerCodes.OVERLOADED], so a slow operation cannot take every worker of
     * the server.
     *
     * @since 1.1.0
     */
    public val maxConcurrentRequests: Int = builder.maxConcurrentRequests

    override fun ValidationScope.validate() {
        bounded(::maxRequestBytes, HARD_REQUEST_BYTES)
        bounded(::maxUploadBytes, HARD_UPLOAD_BYTES)
        bounded(::maxDecompressedBytes, HARD_REQUEST_BYTES)
        bounded(::maxQueryLength, HARD_QUERY_LENGTH.toLong())
        bounded(::maxQueryParameters, HARD_QUERY_PARAMETERS.toLong())
        bounded(::maxHeaders, HARD_HEADERS.toLong())
        bounded(::maxHeaderBytes, HARD_HEADER_BYTES.toLong())
        bounded(::maxConcurrentRequests, HARD_CONCURRENT_REQUESTS.toLong())
    }

    override fun toString(): String = "ServerLimits(requestBytes=$maxRequestBytes, uploadBytes=$maxUploadBytes, " +
        "decompressedBytes=$maxDecompressedBytes, queryLength=$maxQueryLength, queryParameters=$maxQueryParameters, " +
        "headers=$maxHeaders, headerBytes=$maxHeaderBytes)"

    /**
     * The default limits and the hard ceilings.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The hard ceiling of [maxRequestBytes] and [maxDecompressedBytes], 1 GiB.
         *
         * @since 1.0.0
         */
        public const val HARD_REQUEST_BYTES: Long = 1_024L * 1_024 * 1_024

        /**
         * The hard ceiling of [maxUploadBytes], 64 GiB.
         *
         * @since 1.0.0
         */
        public const val HARD_UPLOAD_BYTES: Long = 64L * 1_024 * 1_024 * 1_024

        /**
         * The hard ceiling of [maxQueryLength].
         *
         * @since 1.0.0
         */
        public const val HARD_QUERY_LENGTH: Int = 65_536

        /**
         * The hard ceiling of [maxQueryParameters].
         *
         * @since 1.0.0
         */
        public const val HARD_QUERY_PARAMETERS: Int = 10_000

        /**
         * The hard ceiling of [maxHeaders].
         *
         * @since 1.0.0
         */
        public const val HARD_HEADERS: Int = 1_000

        /**
         * The hard ceiling of [maxHeaderBytes], 1 MiB.
         *
         * @since 1.0.0
         */
        public const val HARD_HEADER_BYTES: Int = 1_024 * 1_024

        /**
         * The hard ceiling of [maxConcurrentRequests], 100,000.
         *
         * @since 1.1.0
         */
        public const val HARD_CONCURRENT_REQUESTS: Int = 100_000

        /**
         * The default limits: 4 MiB per body, 64 MiB per upload, 16 MiB inflated, 8192 characters and 256 fields of
         * query, 100 header fields of 32 KiB together, 512 concurrent requests per operation, and the default limits of
         * multipart bodies and header fields.
         *
         * @since 1.0.0
         */
        public val Default: ServerLimits = ServerLimitsBuilder().build()
    }
}
