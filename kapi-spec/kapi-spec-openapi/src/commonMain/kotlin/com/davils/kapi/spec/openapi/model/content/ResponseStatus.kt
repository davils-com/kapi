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

package com.davils.kapi.spec.openapi.model.content

import kotlin.jvm.JvmInline

private const val DEFAULT: String = "default"

private const val RANGE: String = "XX"

private const val LENGTH: Int = 3

private fun isStatus(text: String): Boolean {
    if (text == DEFAULT) return true
    val rest = text.drop(1)
    val isClass = text.length == LENGTH && text.first() in '1'..'5'
    val isCode = rest.all { character -> character in '0'..'9' }
    return isClass && (isCode || rest == RANGE)
}

/**
 * A key of the `responses` object: an HTTP status code from `100` to `599`, a range of them such as `4XX`, or
 * `default` for every status the other keys do not cover.
 *
 * @property value the key as it was written, such as `200`, `4XX` or `default`.
 * @throws IllegalArgumentException if [value] is none of these forms; the `X` of a range is upper case.
 * @since 1.0.0
 */
@JvmInline
public value class ResponseStatus(public val value: String) {
    init {
        require(isStatus(value)) { "'$value' is no response status such as 200, 4XX or default" }
    }

    /**
     * Whether this is the `default` response.
     *
     * @since 1.0.0
     */
    public val isDefault: Boolean get() = value == DEFAULT

    /**
     * Whether this is a range of statuses, such as `4XX`.
     *
     * @since 1.0.0
     */
    public val isRange: Boolean get() = value.endsWith(RANGE)

    /**
     * The status code, such as `200`; `null` for a range and for `default`.
     *
     * @since 1.0.0
     */
    public val code: Int? get() = value.toIntOrNull()

    override fun toString(): String = value

    /**
     * Creates response statuses.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The `default` response.
         *
         * @since 1.0.0
         */
        public val Default: ResponseStatus = ResponseStatus(DEFAULT)

        /**
         * Returns [text] as a response status, or `null` instead of throwing.
         *
         * @param text the key of the `responses` object as it was written.
         * @return the status, or `null` if [text] is no status code from `100` to `599`, no range such as `4XX` and not
         * `default`.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): ResponseStatus? {
            if (!isStatus(text)) return null
            return ResponseStatus(text)
        }
    }
}
