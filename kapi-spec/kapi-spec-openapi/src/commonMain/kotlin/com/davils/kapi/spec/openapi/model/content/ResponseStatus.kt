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

private val statusPattern: Regex = Regex("default|[1-5]([0-9]{2}|XX)")

@JvmInline
public value class ResponseStatus(public val value: String) {
    init {
        require(statusPattern.matches(value)) { "'$value' is no response status such as 200, 4XX or default" }
    }

    public val isDefault: Boolean get() = value == DEFAULT

    public val isRange: Boolean get() = value.endsWith(RANGE)

    public val code: Int? get() = value.toIntOrNull()

    override fun toString(): String = value

    public companion object {
        public val Default: ResponseStatus = ResponseStatus(DEFAULT)

        public fun parseOrNull(text: String): ResponseStatus? {
            if (!statusPattern.matches(text)) return null
            return ResponseStatus(text)
        }
    }
}
