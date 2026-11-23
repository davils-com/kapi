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

package com.davils.kapi.runtime.server.problem

/**
 * The codes of the problem details of RFC 9457 a server answers with, in the member `code` of a
 * [Problem][com.davils.kapi.runtime.core.problem.Problem] and of its violations.
 *
 * The codes are stable API: they never change their meaning once released.
 *
 * @since 1.0.0
 */
public object ServerCodes {
    /**
     * A parameter or a body the operation requires is missing; a violation of a 400 problem.
     *
     * @since 1.0.0
     */
    public const val REQUIRED: String = "kapi.server.required"

    /**
     * A parameter or a body is not what the document declares; a violation of a 400 problem.
     *
     * @since 1.0.0
     */
    public const val MALFORMED: String = "kapi.server.malformed"

    /**
     * The request is invalid; the code of a 400 problem with violations.
     *
     * @since 1.0.0
     */
    public const val INVALID: String = "kapi.runtime.problem.invalid"

    /**
     * The media type of the body is not one the operation reads; status 415.
     *
     * @since 1.0.0
     */
    public const val UNSUPPORTED_MEDIA_TYPE: String = "kapi.server.unsupported-media-type"

    /**
     * The content coding of the body is not one the server inflates; status 415.
     *
     * @since 1.0.0
     */
    public const val UNSUPPORTED_ENCODING: String = "kapi.server.unsupported-encoding"

    /**
     * No media type the operation answers with is acceptable to the client; status 406.
     *
     * @since 1.0.0
     */
    public const val NOT_ACCEPTABLE: String = "kapi.server.not-acceptable"

    /**
     * The body is larger than the server reads, or inflates to more; status 413.
     *
     * @since 1.0.0
     */
    public const val TOO_LARGE: String = "kapi.server.too-large"

    /**
     * The query is longer than the server reads; status 414.
     *
     * @since 1.0.0
     */
    public const val URI_TOO_LONG: String = "kapi.server.uri-too-long"

    /**
     * The header fields are more or longer than the server reads; status 431.
     *
     * @since 1.0.0
     */
    public const val HEADERS_TOO_LARGE: String = "kapi.server.headers-too-large"

    /**
     * The request carries no valid credentials; status 401.
     *
     * @since 1.0.0
     */
    public const val UNAUTHENTICATED: String = "kapi.server.unauthenticated"

    /**
     * The caller is known but may not do this; status 403.
     *
     * @since 1.0.0
     */
    public const val FORBIDDEN: String = "kapi.server.forbidden"

    /**
     * A webhook or a callback failed its verification; status 401.
     *
     * @since 1.0.0
     */
    public const val UNVERIFIED: String = "kapi.server.unverified"

    /**
     * A precondition of RFC 9110 §13 failed; status 412.
     *
     * @since 1.0.0
     */
    public const val PRECONDITION_FAILED: String = "kapi.server.precondition-failed"

    /**
     * No range of the request lies inside the representation; status 416.
     *
     * @since 1.0.0
     */
    public const val RANGE_NOT_SATISFIABLE: String = "kapi.server.range-not-satisfiable"

    /**
     * A WebSocket channel was asked for without an upgrade, or with no subprotocol it speaks; status 426 or 400.
     *
     * @since 1.0.0
     */
    public const val UPGRADE_REQUIRED: String = "kapi.server.upgrade-required"

    /**
     * The server failed to answer; status 500, which reveals nothing of the failure.
     *
     * @since 1.0.0
     */
    public const val INTERNAL: String = "kapi.server.internal"
}
