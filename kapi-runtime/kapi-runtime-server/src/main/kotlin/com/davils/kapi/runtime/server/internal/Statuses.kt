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

package com.davils.kapi.runtime.server.internal

internal object Statuses {
    const val OK: Int = 200
    const val NO_CONTENT: Int = 204
    const val PARTIAL_CONTENT: Int = 206
    const val NOT_MODIFIED: Int = 304
    const val BAD_REQUEST: Int = 400
    const val UNAUTHORIZED: Int = 401
    const val FORBIDDEN: Int = 403
    const val NOT_ACCEPTABLE: Int = 406
    const val PRECONDITION_FAILED: Int = 412
    const val TOO_LARGE: Int = 413
    const val URI_TOO_LONG: Int = 414
    const val UNSUPPORTED_MEDIA_TYPE: Int = 415
    const val RANGE_NOT_SATISFIABLE: Int = 416
    const val UPGRADE_REQUIRED: Int = 426
    const val HEADERS_TOO_LARGE: Int = 431
    const val INTERNAL: Int = 500
    const val CLIENT_ERRORS: Int = 400
    const val SERVER_ERRORS: Int = 500
}
