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

package com.davils.kapi.codegen.core.extension

import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.spec.core.value.ExtensionName

internal object ExtensionCatalogue {
    const val NAMESPACE: String = "x-kapi-"

    val foreign: Set<String> = setOf("x-kapi-requires-restart", "x-kapi-deprecation")

    val unsupported: Map<String, String> = mapOf(
        "x-kapi-websocket" to "WebSocket endpoints are not generated",
        "x-kapi-asyncapi" to "AsyncAPI documents are not generated",
    )

    private val schema: Set<ExtensionSite> = setOf(ExtensionSite.Schema)

    private val operation: Set<ExtensionSite> = setOf(ExtensionSite.Operation)

    private val operationOrPath: Set<ExtensionSite> = setOf(ExtensionSite.Operation, ExtensionSite.PathItem)

    private val named: Set<ExtensionSite> = schema + operation + ExtensionSite.Parameter

    private val sensitive: Set<ExtensionSite> = schema + ExtensionSite.Parameter + ExtensionSite.Header

    val keywords: List<ExtensionKeyword> = listOf(
        ExtensionKeyword(Keywords.NAME, ExtensionShape.Identifier, named),
        ExtensionKeyword(Keywords.TYPE, ExtensionShape.TypeReference, schema),
        ExtensionKeyword(Keywords.IGNORE, ExtensionShape.Flag, schema + operation),
        ExtensionKeyword(Keywords.SENSITIVE, ExtensionShape.Flag, sensitive),
        ExtensionKeyword(Keywords.ENUM_NAMES, ExtensionShape.Identifiers, schema),
        ExtensionKeyword(Keywords.ENUM_NAMES_ALIAS, ExtensionShape.Identifiers, schema),
        ExtensionKeyword(Keywords.GROUP, ExtensionShape.Label, operationOrPath),
        ExtensionKeyword(Keywords.IDEMPOTENT, ExtensionShape.Flag, operation),
        ExtensionKeyword(Keywords.TIMEOUT, ExtensionShape.Timeout, operationOrPath),
        ExtensionKeyword(Keywords.PAGINATION, ExtensionShape.Pagination, operation),
        ExtensionKeyword(Keywords.TRI_STATE, ExtensionShape.Flag, operation),
        ExtensionKeyword(Keywords.STREAM, ExtensionShape.Stream, setOf(ExtensionSite.MediaType)),
        ExtensionKeyword(Keywords.UPGRADE, ExtensionShape.Label, operation),
        ExtensionKeyword(Keywords.RATE_LIMIT, ExtensionShape.RateLimit, operation),
    )

    private val byName: Map<ExtensionName, ExtensionKeyword> = keywords.associateBy(ExtensionKeyword::name)

    fun keyword(name: ExtensionName): ExtensionKeyword? = byName[name]

    fun isReserved(name: ExtensionName): Boolean = name.value.startsWith(NAMESPACE) && name.value !in foreign
}
