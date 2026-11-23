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

package com.davils.kapi.runtime.core.response

private const val FIRST_INFORMATIONAL: Int = 100

private const val FIRST_SUCCESS: Int = 200

private const val FIRST_REDIRECTION: Int = 300

private const val FIRST_CLIENT_ERROR: Int = 400

private const val FIRST_SERVER_ERROR: Int = 500

private const val LAST_STATUS: Int = 599

/**
 * The status codes of HTTP that kapi and generated code name, as RFC 9110 registers them, and the classes they fall
 * into.
 *
 * The object keeps no state and is safe to use from any thread.
 *
 * @since 1.0.0
 */
public object HttpStatus {
    /**
     * `101 Switching Protocols`: the server switches to the protocol of an `Upgrade`, such as WebSocket.
     *
     * @since 1.0.0
     */
    public const val SWITCHING_PROTOCOLS: Int = 101

    /**
     * `200 OK`: the request succeeded.
     *
     * @since 1.0.0
     */
    public const val OK: Int = 200

    /**
     * `201 Created`: the request created a resource.
     *
     * @since 1.0.0
     */
    public const val CREATED: Int = 201

    /**
     * `202 Accepted`: the request was accepted for later processing.
     *
     * @since 1.0.0
     */
    public const val ACCEPTED: Int = 202

    /**
     * `204 No Content`: the request succeeded and the response has no content.
     *
     * @since 1.0.0
     */
    public const val NO_CONTENT: Int = 204

    /**
     * `206 Partial Content`: the response holds the requested ranges of a representation.
     *
     * @since 1.0.0
     */
    public const val PARTIAL_CONTENT: Int = 206

    /**
     * `301 Moved Permanently`: the resource moved for good.
     *
     * @since 1.0.0
     */
    public const val MOVED_PERMANENTLY: Int = 301

    /**
     * `302 Found`: the resource is elsewhere for now.
     *
     * @since 1.0.0
     */
    public const val FOUND: Int = 302

    /**
     * `303 See Other`: the answer is found with a `GET` elsewhere.
     *
     * @since 1.0.0
     */
    public const val SEE_OTHER: Int = 303

    /**
     * `304 Not Modified`: the representation the client holds is still current.
     *
     * @since 1.0.0
     */
    public const val NOT_MODIFIED: Int = 304

    /**
     * `307 Temporary Redirect`: the resource is elsewhere for now; the method is kept.
     *
     * @since 1.0.0
     */
    public const val TEMPORARY_REDIRECT: Int = 307

    /**
     * `308 Permanent Redirect`: the resource moved for good; the method is kept.
     *
     * @since 1.0.0
     */
    public const val PERMANENT_REDIRECT: Int = 308

    /**
     * `400 Bad Request`: the request is malformed or invalid.
     *
     * @since 1.0.0
     */
    public const val BAD_REQUEST: Int = 400

    /**
     * `401 Unauthorized`: the request lacks valid credentials.
     *
     * @since 1.0.0
     */
    public const val UNAUTHORIZED: Int = 401

    /**
     * `403 Forbidden`: the credentials do not grant the request.
     *
     * @since 1.0.0
     */
    public const val FORBIDDEN: Int = 403

    /**
     * `404 Not Found`: there is no such resource.
     *
     * @since 1.0.0
     */
    public const val NOT_FOUND: Int = 404

    /**
     * `405 Method Not Allowed`: the resource does not support the method.
     *
     * @since 1.0.0
     */
    public const val METHOD_NOT_ALLOWED: Int = 405

    /**
     * `406 Not Acceptable`: no representation matches the `Accept` fields.
     *
     * @since 1.0.0
     */
    public const val NOT_ACCEPTABLE: Int = 406

    /**
     * `409 Conflict`: the request conflicts with the state of the resource.
     *
     * @since 1.0.0
     */
    public const val CONFLICT: Int = 409

    /**
     * `410 Gone`: the resource is gone for good.
     *
     * @since 1.0.0
     */
    public const val GONE: Int = 410

    /**
     * `412 Precondition Failed`: a condition of the request does not hold.
     *
     * @since 1.0.0
     */
    public const val PRECONDITION_FAILED: Int = 412

    /**
     * `413 Content Too Large`: the content of the request is larger than allowed.
     *
     * @since 1.0.0
     */
    public const val CONTENT_TOO_LARGE: Int = 413

    /**
     * `414 URI Too Long`: the target of the request is longer than allowed.
     *
     * @since 1.0.0
     */
    public const val URI_TOO_LONG: Int = 414

    /**
     * `415 Unsupported Media Type`: the content is of a type the resource does not read.
     *
     * @since 1.0.0
     */
    public const val UNSUPPORTED_MEDIA_TYPE: Int = 415

    /**
     * `416 Range Not Satisfiable`: no requested range overlaps the representation.
     *
     * @since 1.0.0
     */
    public const val RANGE_NOT_SATISFIABLE: Int = 416

    /**
     * `422 Unprocessable Content`: the content is well formed but invalid.
     *
     * @since 1.0.0
     */
    public const val UNPROCESSABLE_CONTENT: Int = 422

    /**
     * `426 Upgrade Required`: the request must switch to another protocol.
     *
     * @since 1.0.0
     */
    public const val UPGRADE_REQUIRED: Int = 426

    /**
     * `428 Precondition Required`: the request must be conditional.
     *
     * @since 1.0.0
     */
    public const val PRECONDITION_REQUIRED: Int = 428

    /**
     * `429 Too Many Requests`: the client sent more requests than allowed.
     *
     * @since 1.0.0
     */
    public const val TOO_MANY_REQUESTS: Int = 429

    /**
     * `431 Request Header Fields Too Large`: the header fields are larger than allowed.
     *
     * @since 1.0.0
     */
    public const val REQUEST_HEADER_FIELDS_TOO_LARGE: Int = 431

    /**
     * `500 Internal Server Error`: the server failed unexpectedly.
     *
     * @since 1.0.0
     */
    public const val INTERNAL_SERVER_ERROR: Int = 500

    /**
     * `501 Not Implemented`: the server does not support the request.
     *
     * @since 1.0.0
     */
    public const val NOT_IMPLEMENTED: Int = 501

    /**
     * `502 Bad Gateway`: an upstream server answered invalidly.
     *
     * @since 1.0.0
     */
    public const val BAD_GATEWAY: Int = 502

    /**
     * `503 Service Unavailable`: the server cannot answer for now.
     *
     * @since 1.0.0
     */
    public const val SERVICE_UNAVAILABLE: Int = 503

    /**
     * `504 Gateway Timeout`: an upstream server did not answer in time.
     *
     * @since 1.0.0
     */
    public const val GATEWAY_TIMEOUT: Int = 504

    /**
     * Returns whether [status] is of the class 1xx, informational.
     *
     * @param status the status code; any integer is accepted.
     * @return `true` if [status] lies in the class.
     * @since 1.0.0
     */
    public fun isInformational(status: Int): Boolean = status in FIRST_INFORMATIONAL until FIRST_SUCCESS

    /**
     * Returns whether [status] is of the class 2xx, successful.
     *
     * @param status the status code; any integer is accepted.
     * @return `true` if [status] lies in the class.
     * @since 1.0.0
     */
    public fun isSuccess(status: Int): Boolean = status in FIRST_SUCCESS until FIRST_REDIRECTION

    /**
     * Returns whether [status] is of the class 3xx, a redirection.
     *
     * @param status the status code; any integer is accepted.
     * @return `true` if [status] lies in the class.
     * @since 1.0.0
     */
    public fun isRedirection(status: Int): Boolean = status in FIRST_REDIRECTION until FIRST_CLIENT_ERROR

    /**
     * Returns whether [status] is of the class 4xx, an error of the client.
     *
     * @param status the status code; any integer is accepted.
     * @return `true` if [status] lies in the class.
     * @since 1.0.0
     */
    public fun isClientError(status: Int): Boolean = status in FIRST_CLIENT_ERROR until FIRST_SERVER_ERROR

    /**
     * Returns whether [status] is of the class 5xx, an error of the server.
     *
     * @param status the status code; any integer is accepted.
     * @return `true` if [status] lies in the class.
     * @since 1.0.0
     */
    public fun isServerError(status: Int): Boolean = status in FIRST_SERVER_ERROR..LAST_STATUS

    /**
     * Returns whether [status] is a status code HTTP allows, from 100 to 599.
     *
     * @param status the status code; any integer is accepted.
     * @return `true` if [status] lies between 100 and 599.
     * @since 1.0.0
     */
    public fun isValid(status: Int): Boolean = status in FIRST_INFORMATIONAL..LAST_STATUS
}
