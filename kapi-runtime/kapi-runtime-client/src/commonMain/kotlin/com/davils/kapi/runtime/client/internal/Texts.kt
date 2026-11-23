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

package com.davils.kapi.runtime.client.internal

import io.ktor.http.parseUrl
import kotlin.io.encoding.Base64

private const val SEPARATORS: String = "._-"

private const val HTTP: String = "http"

private const val HTTPS: String = "https"

private const val SCHEME_END: String = "://"

private val loopbackHosts: Set<String> = setOf("localhost", "127.0.0.1", "[::1]", "::1")

internal object Texts {
    val base64Url: Base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    fun isLowerWords(text: String): Boolean {
        val isStarted = text.firstOrNull() in 'a'..'z'
        val isEnded = text.lastOrNull()?.let { char -> char !in SEPARATORS } == true
        val isWritten = text.all { char -> char in 'a'..'z' || char in '0'..'9' || char in SEPARATORS }
        val hasDoubleSeparator = text.zipWithNext().any { (left, right) -> left in SEPARATORS && right in SEPARATORS }
        return isStarted && isEnded && isWritten && !hasDoubleSeparator
    }

    fun isPlainUrl(text: String, schemes: Set<String> = setOf(HTTP, HTTPS)): Boolean {
        val isVisible = text.all { char -> char in '!'..'~' } && '?' !in text && '#' !in text
        val url = parseUrl(text)?.takeIf { isVisible } ?: return false
        val scheme = text.substringBefore(delimiter = SCHEME_END, missingDelimiterValue = "")
        val authority = text.substringAfter(delimiter = SCHEME_END, missingDelimiterValue = "").substringBefore('/')
        val hasHost = url.host.isNotEmpty() && authority.isNotEmpty()
        return scheme in schemes && hasHost && '@' !in authority && '\\' !in text
    }

    fun isProtectedUrl(text: String): Boolean {
        if (!isPlainUrl(text)) return false
        val authority = text.substringAfter(SCHEME_END).substringBefore('/')
        val bracketed = authority.substringBefore(']') + "]"
        val host = bracketed.takeIf { authority.startsWith('[') } ?: authority.substringBefore(':')
        return text.startsWith("$HTTPS$SCHEME_END") || host in loopbackHosts
    }
}
