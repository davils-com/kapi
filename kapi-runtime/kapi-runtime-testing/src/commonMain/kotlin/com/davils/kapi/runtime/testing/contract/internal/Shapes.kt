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

import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.model.schema.SchemaType

private const val MOST_HOPS: Int = 16

internal class Shapes(private val document: Document) {
    fun types(schema: Schema?): Set<SchemaType> = resolved(schema, 0)?.types.orEmpty()

    fun items(schema: Schema?): Schema? = resolved(schema, 0)?.items

    fun property(schema: Schema?, name: String): Schema? {
        val definition = resolved(schema, 0)
        val properties = definition?.properties.orEmpty()
        return properties[name] ?: definition?.additionalProperties
    }

    private fun resolved(schema: Schema?, hops: Int): SchemaDefinition? {
        val definition = schema as? SchemaDefinition
        val target = definition?.reference?.let { reference -> document.targets[reference.target] as? Schema }
        val next = target?.takeIf { hops < MOST_HOPS && definition.types.isEmpty() } ?: return definition
        return resolved(next, hops + 1)
    }
}
