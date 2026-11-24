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

package com.davils.kapi.codegen.core.naming

import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.options.GeneratorOptions
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.codegen.core.pipeline.valueOrNull
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.model.schema.Schema

internal class SchemaNames(private val options: GeneratorOptions, private val table: NameTable) {
    fun name(document: Document): Map<String, String> {
        val schemas = document.components?.schemas.orEmpty()
        val names = schemas.toSortedMap().mapValues { (key, schema) -> nameOf(key, schema) }
        names.forEach { (key, name) ->
            table.claim(NameClaim(name, "The schema ${shown(key)}", schemas.getValue(key).location))
        }
        return names
    }

    private fun nameOf(key: String, schema: Schema): String {
        val declared = schema.extensions.text(Keywords.NAME).valueOrNull()
        val chosen = declared?.takeIf(Identifiers::isIdentifier) ?: Identifiers.type(key)
        return options.typePrefix + options.apiName + chosen
    }
}
