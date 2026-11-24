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

package com.davils.kapi.runtime.client.oauth.internal

import com.davils.kapi.runtime.client.internal.Texts
import com.davils.kore.core.failure.contained
import com.davils.krypt.primitive.Crypto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

private const val MOST_ERROR_LENGTH: Int = 64

private const val MOST_DEPTH: Int = 16

private const val RANDOM_BYTES: Int = 32

private const val ERROR: String = "error"

private const val OPENERS: String = "[{"

private const val CLOSERS: String = "]}"

internal object OAuthJson {
    fun objectOf(text: String): JsonObject? {
        if (!isShallow(text)) return null
        return contained { Json.parseToJsonElement(text) }.getOrNull() as? JsonObject
    }

    fun text(json: JsonObject, name: String): String? = (json[name] as? JsonPrimitive)
        ?.takeIf { value -> value.isString }
        ?.content

    fun number(json: JsonObject, name: String): Long? {
        val value = json[name] as? JsonPrimitive
        return value?.run { longOrNull ?: content.toLongOrNull() }
    }

    fun isTrue(json: JsonObject, name: String): Boolean {
        val value = json[name] as? JsonPrimitive
        return value?.takeUnless { found -> found.isString }?.content == "true"
    }

    fun texts(json: JsonObject, name: String): List<String> {
        val items = (json[name] as? JsonArray).orEmpty()
        return items.mapNotNull { item -> (item as? JsonPrimitive)?.takeIf { value -> value.isString }?.content }
    }

    fun errorOf(text: String): String? {
        val code = objectOf(text)?.let { json -> text(json, ERROR) }
        return code?.takeIf { found -> found.length in 1..MOST_ERROR_LENGTH && found.all(::isErrorChar) }
    }

    fun seconds(value: Long?, most: Long): Long? = value?.coerceIn(minimumValue = 0L, maximumValue = most)

    fun random(crypto: Crypto): String = Texts.base64Url.encode(crypto.environment.random.bytes(RANDOM_BYTES))

    private fun isErrorChar(char: Char): Boolean = char in ' '..'~' && char != '"' && char != '\\'

    private fun isShallow(text: String): Boolean {
        var depth = 0
        var deepest = 0
        var isQuoted = false
        var isEscaped = false
        text.forEach { char ->
            val isQuote = char == '"' && !isEscaped
            isEscaped = isQuoted && !isEscaped && char == '\\'
            if (isQuote) isQuoted = !isQuoted
            if (!isQuoted && char in OPENERS) depth += 1
            if (!isQuoted && char in CLOSERS) depth -= 1
            deepest = maxOf(a = deepest, b = depth)
        }
        return deepest <= MOST_DEPTH
    }
}
