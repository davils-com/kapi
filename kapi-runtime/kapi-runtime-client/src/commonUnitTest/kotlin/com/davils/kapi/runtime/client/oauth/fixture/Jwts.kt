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

package com.davils.kapi.runtime.client.oauth.fixture

import io.ktor.http.decodeURLQueryComponent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.io.encoding.Base64

private val base64: Base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

internal object Jwts {
    fun header(token: String): JsonObject = part(token, 0)

    fun claims(token: String): JsonObject = part(token, 1)

    fun fields(form: String): Map<String, String> {
        val pairs = form.split('&').filter(String::isNotEmpty)
        return pairs.associate { field ->
            val value = field.substringAfter(delimiter = '=', missingDelimiterValue = "")
            decoded(field.substringBefore('=')) to decoded(value)
        }
    }

    fun text(json: JsonObject?, name: String): String? = (json?.get(name) as? JsonPrimitive)?.content

    fun member(json: JsonObject?, name: String): JsonObject? = json?.get(name) as? JsonObject

    private fun part(token: String, index: Int): JsonObject {
        val part = token.split('.')[index]
        val text = base64.decode(part).decodeToString()
        return Json.parseToJsonElement(text).jsonObject
    }

    private fun decoded(text: String): String = text.decodeURLQueryComponent(plusIsSpace = true)
}
