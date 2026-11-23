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

/**
 * Why a reader of a multipart body failed; a reader that failed stays failed.
 *
 * @since 1.0.0
 */
public enum class MultipartFailure {
    /**
     * The body has more parts than [MultipartLimits.maxParts].
     *
     * @since 1.0.0
     */
    TooManyParts,

    /**
     * The header block of a part is longer than [MultipartLimits.maxHeaderBytes].
     *
     * @since 1.0.0
     */
    HeadersTooLong,

    /**
     * A part has more header fields than [MultipartLimits.maxHeaders].
     *
     * @since 1.0.0
     */
    TooManyHeaders,

    /**
     * A header field of a part is malformed: no name, a name that is no token, a folded line, a control character or
     * octets that are no UTF-8.
     *
     * @since 1.0.0
     */
    MalformedHeader,

    /**
     * The content of a part is larger than [MultipartLimits.maxPartBytes].
     *
     * @since 1.0.0
     */
    PartTooLarge,

    /**
     * The body is larger than [MultipartLimits.maxBodyBytes].
     *
     * @since 1.0.0
     */
    BodyTooLarge,

    /**
     * The text before the first boundary is longer than [MultipartLimits.maxPreambleBytes].
     *
     * @since 1.0.0
     */
    PreambleTooLong,

    /**
     * A boundary is followed by something other than `--`, blanks and a line break.
     *
     * @since 1.0.0
     */
    MalformedDelimiter,

    /**
     * The body ended before its closing boundary.
     *
     * @since 1.0.0
     */
    Truncated,
}
