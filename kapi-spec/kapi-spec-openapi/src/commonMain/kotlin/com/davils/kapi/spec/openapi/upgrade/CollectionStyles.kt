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

package com.davils.kapi.spec.openapi.upgrade

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.core.node.yamlScalar
import com.davils.kapi.spec.openapi.read.shape.Keys

internal class CollectionStyles(private val notes: UpgradeNotes) {
    fun styleOf(place: Located): List<YamlEntry> {
        if (place.node[Keys.TYPE].string() != SwaggerKeys.ARRAY) return emptyList()
        val format = place.node[SwaggerKeys.COLLECTION_FORMAT].string() ?: SwaggerKeys.CSV
        val placement = place.node[Keys.IN].string()
        val table = delimited.takeIf { placement in delimitedPlacements } ?: simple
        val style = table[format] ?: lost(place, format)
        return style?.let(::entriesOf).orEmpty()
    }

    fun checkNested(place: Located) {
        val format = place.node[SwaggerKeys.COLLECTION_FORMAT].string() ?: return
        notes.lost(place, "The collection format $format of nested items has no equivalent in OpenAPI 3")
    }

    private fun lost(place: Located, format: String): Pair<String, Boolean>? {
        notes.lost(place, "The collection format $format has no style in OpenAPI 3, the parameter keeps its default")
        return null
    }

    private fun entriesOf(style: Pair<String, Boolean>): List<YamlEntry> =
        Trees.entries(Keys.STYLE to Trees.textOf(style.first), Keys.EXPLODE to yamlScalar(style.second))

    private companion object {
        val delimitedPlacements: Set<String> = setOf(SwaggerKeys.QUERY, SwaggerKeys.FORM_DATA)

        val delimited: Map<String, Pair<String, Boolean>> = mapOf(
            SwaggerKeys.CSV to (SwaggerKeys.FORM_STYLE to false),
            SwaggerKeys.SSV to (SwaggerKeys.SPACE_DELIMITED to false),
            SwaggerKeys.PIPES to (SwaggerKeys.PIPE_DELIMITED to false),
            SwaggerKeys.MULTI to (SwaggerKeys.FORM_STYLE to true),
        )

        val simple: Map<String, Pair<String, Boolean>> = mapOf(
            SwaggerKeys.CSV to (SwaggerKeys.SIMPLE_STYLE to false),
        )
    }
}
