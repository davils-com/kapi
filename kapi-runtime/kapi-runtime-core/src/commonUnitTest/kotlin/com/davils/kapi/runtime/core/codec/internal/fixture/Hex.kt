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

package com.davils.kapi.runtime.core.codec.internal.fixture

import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.codec.internal.CborReader
import com.davils.kapi.runtime.core.codec.internal.CborWriter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal fun written(json: String): String {
    val bytes = CborWriter().write(Json.parseToJsonElement(json))
    return bytes.toByteArray().toHexString()
}

internal fun read(hex: String): Decoding<JsonElement> = CborReader(hex.hexToByteArray()).read()

internal fun readJson(hex: String): JsonElement? {
    val decoded = read(hex) as? Decoding.Decoded
    return decoded?.value
}

internal fun reasonOf(hex: String): String? {
    val failed = read(hex) as? Decoding.Failed
    return failed?.reason
}
