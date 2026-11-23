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

import com.davils.kapi.runtime.core.structured.BareItem
import com.davils.kapi.runtime.core.structured.InnerList
import com.davils.kapi.runtime.core.structured.Item
import com.davils.kapi.runtime.server.request.Request

private const val DERIVED: Char = '@'

private const val PARAMS: String = "\"@signature-params\": "

internal object SignatureBase {
    private val derived: Map<String, (Request) -> String?> = mapOf(
        "@method" to { request -> request.method },
        "@scheme" to { request -> request.scheme },
        "@authority" to { request -> request.authority },
        "@path" to { request -> request.path.ifEmpty { "/" } },
        "@query" to { request -> "?" + request.rawQuery.orEmpty() },
        "@request-target" to { request -> request.path + queryOf(request) },
        "@target-uri" to { request -> "${request.scheme}://${request.authority}${request.path}${queryOf(request)}" },
    )

    fun of(request: Request, covered: InnerList): String? {
        val names = covered.items.map(::nameOf)
        val isUnique = names.toSet().size == names.size
        val lines = covered.items.map { item -> lineOf(request, item) }
        val complete = lines.filterNotNull().takeIf { found -> found.size == lines.size && isUnique }
        return complete?.let { found -> found.joinToString("") { line -> line + "\n" } + PARAMS + covered }
    }

    fun nameOf(item: Item): String? = (item.value as? BareItem.Text)?.value

    private fun lineOf(request: Request, item: Item): String? {
        val name = nameOf(item)?.takeIf { found -> item.parameters.isEmpty && found == found.lowercase() }
        val value = name?.let { found ->
            val reader = derived.getOrElse(found) { { target: Request -> fieldOf(target, found) } }
            reader(request)
        }
        return value?.let { found -> "$item: $found" }
    }

    private fun fieldOf(request: Request, name: String): String? {
        val listed = request.headers.all(name)
        val values = listed
            .takeUnless { name.startsWith(DERIVED) }
            .orEmpty()
        return values.joinToString(", ") { value -> value.trim() }.takeIf { values.isNotEmpty() }
    }

    private fun queryOf(request: Request): String = request.rawQuery?.let { query -> "?$query" }.orEmpty()
}
