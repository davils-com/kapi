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

import com.davils.kapi.runtime.core.codec.multipart.MultipartFailure
import com.davils.kapi.runtime.server.problem.ServerCodes

internal object MultipartRefusals {
    fun statusOf(failure: MultipartFailure): Int = when (failure) {
        MultipartFailure.TooManyParts,
        MultipartFailure.HeadersTooLong,
        MultipartFailure.TooManyHeaders,
        MultipartFailure.PartTooLarge,
        MultipartFailure.BodyTooLarge,
        MultipartFailure.PreambleTooLong,
        -> Statuses.TOO_LARGE

        MultipartFailure.MalformedHeader,
        MultipartFailure.MalformedDelimiter,
        MultipartFailure.Truncated,
        -> Statuses.BAD_REQUEST
    }

    fun codeOf(failure: MultipartFailure): String {
        if (statusOf(failure) == Statuses.TOO_LARGE) return ServerCodes.TOO_LARGE
        return ServerCodes.MALFORMED
    }
}
