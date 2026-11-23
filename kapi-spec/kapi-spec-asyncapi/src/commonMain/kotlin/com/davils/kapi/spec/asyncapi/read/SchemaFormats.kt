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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kapi.spec.asyncapi.model.schema.SchemaFormat

internal object SchemaFormats {
    private const val VERSION: String = "version="

    private const val QUOTE: String = "\""

    private val asyncApi: Set<String> = family("application/vnd.aai.asyncapi")

    private val jsonSchema: Set<String> = setOf("application/schema+json", "application/schema+yaml")

    private val openApi: Set<String> = family("application/vnd.oai.openapi")

    private val avro: Set<String> = family("application/vnd.apache.avro")

    private val formats: List<Pair<SchemaFormat, (String, String?) -> Boolean>> = listOf(
        SchemaFormat.AsyncApi to { type, _ -> type in asyncApi },
        SchemaFormat.JsonSchema to { type, version -> type in jsonSchema && version == "draft-07" },
        SchemaFormat.OpenApi to { type, version -> type in openApi && version.orEmpty().startsWith("3.0") },
        SchemaFormat.Avro to { type, version -> type in avro && version?.startsWith("1.") != false },
        SchemaFormat.Raml to { type, version -> type == "application/raml+yaml" && version == "1.0" },
        SchemaFormat.Protobuf to { type, version ->
            type == "application/vnd.google.protobuf" && version in setOf(null, "2", "3")
        },
    )

    fun of(text: String): SchemaFormat {
        val compact = text.filterNot { character -> character == ' ' || character == '\t' }.lowercase()
        val parts = compact.split(';')
        val type = parts.first()
        val parameter = parts.drop(1).firstOrNull { part -> part.startsWith(VERSION) }
        val version = parameter?.run { removePrefix(VERSION).removeSurrounding(QUOTE) }
        val known = formats.firstOrNull { (_, accepts) -> accepts(type, version) }
        return known?.first ?: SchemaFormat.Unknown
    }

    private fun family(type: String): Set<String> = setOf(type, "$type+json", "$type+yaml")
}
