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

private const val FIRST_SUCCESS: Int = 200

private const val LAST_SUCCESS: Int = 299

/**
 * What every response type generated for an operation shares: its status and its header fields.
 *
 * @since 1.0.0
 */
public interface Response {
    /**
     * The HTTP status code.
     *
     * @since 1.0.0
     */
    public val status: Int

    /**
     * The header fields.
     *
     * @since 1.0.0
     */
    public val headers: Headers
}

/**
 * Whether the status of the response lies between 200 and 299.
 *
 * @since 1.0.0
 */
public val Response.isSuccess: Boolean get() = status in FIRST_SUCCESS..LAST_SUCCESS
