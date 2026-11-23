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

package com.davils.kapi.runtime.testing.contract.internal

import com.davils.kapi.runtime.core.negotiation.MediaType

private const val JSON: String = "application/json"

private const val JSON_SUFFIX: String = "+json"

private val records: Set<String> = setOf("application/x-ndjson", "application/jsonl")

internal object MediaTypes {
    fun select(declared: Collection<String>, type: MediaType): String? {
        val essence = type.essence.lowercase()
        val wildcard = "${type.type.lowercase()}/*"
        return declared.firstOrNull { key -> essenceOf(key) == essence }
            ?: declared.firstOrNull { key -> essenceOf(key) == wildcard }
            ?: declared.firstOrNull { key -> essenceOf(key) == "*/*" }
    }

    fun isJson(essence: String): Boolean {
        val lower = essence.lowercase()
        return lower == JSON || lower.endsWith(JSON_SUFFIX)
    }

    fun isRecords(essence: String): Boolean = essence.lowercase() in records

    fun essenceOf(key: String): String {
        val essence = key.substringBefore(';')
        return essence.trim().lowercase()
    }
}
