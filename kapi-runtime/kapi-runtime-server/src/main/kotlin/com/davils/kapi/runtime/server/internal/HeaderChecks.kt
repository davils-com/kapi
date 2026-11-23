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

private const val TAB: Char = '\t'

private const val DELETE: Char = '\u007F'

private const val MOST_VALUE_LENGTH: Int = 65_536

private val reserved: Set<String> = setOf(
    "content-type",
    "content-length",
    "transfer-encoding",
    "connection",
    "keep-alive",
    "te",
    "trailer",
)

internal object HeaderChecks {
    fun requireField(name: String, value: String) {
        require(PathTemplates.isToken(name)) { "The name of a header field is a token" }
        require(name.lowercase() !in reserved) { "The header field ${name.lowercase()} is written by the server" }
        require(isValue(value)) { "The value of a header field holds no control character and no line break" }
    }

    fun isValue(value: String): Boolean = value.length <= MOST_VALUE_LENGTH && value.none(::isControl)

    fun isLine(value: String): Boolean = value.none { char -> char == '\r' || char == '\n' || char == '\u0000' }

    private fun isControl(char: Char): Boolean = (char < ' ' && char != TAB) || char == DELETE
}
