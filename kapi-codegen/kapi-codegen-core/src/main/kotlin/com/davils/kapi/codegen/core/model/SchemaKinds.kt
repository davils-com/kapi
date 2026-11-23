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


package com.davils.kapi.codegen.core.model

import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.model.schema.SchemaType
import com.davils.kapi.codegen.core.ModelKind

private val onlyNull: Set<SchemaType> = setOf(SchemaType.Null)

private val objectOnly: Set<SchemaType> = setOf(SchemaType.Object)

internal object SchemaKinds {
    fun kindOf(schema: Schema): ModelKind? {
        val definition = schema as? SchemaDefinition
        return definition?.let(::kindOf)
    }

    fun isNull(schema: Schema): Boolean {
        val definition = schema as? SchemaDefinition
        return definition?.types == onlyNull
    }

    fun isComposable(definition: SchemaDefinition): Boolean =
        definition.properties.isNotEmpty() || definition.allOf.isNotEmpty()

    fun isUnconstrained(schema: Schema): Boolean {
        val definition = schema as? SchemaDefinition ?: return false
        val isPlain = definition.reference == null && definition.enumeration == null && definition.items == null
        val isFlat = definition.allOf.isEmpty() && alternativesOf(definition).isEmpty()
        val isOpen = definition.properties.isEmpty() && definition.additionalProperties == null
        return isPlain && isFlat && isOpen && objectOnly.containsAll(definition.types)
    }

    fun aliasOf(definition: SchemaDefinition): Schema? {
        val members = definition.allOf.filterNot(::isUnconstrained)
        val isWrapper = definition.properties.isEmpty() && alternativesOf(definition).isEmpty()
        val isPlain = definition.enumeration == null && definition.types.isEmpty()
        return members.singleOrNull().takeIf { isWrapper && isPlain }
    }

    fun alternativesOf(definition: SchemaDefinition): List<Schema> =
        definition.oneOf.ifEmpty { definition.anyOf }

    fun optionalOf(definition: SchemaDefinition): Schema? {
        val alternatives = alternativesOf(definition)
        val others = alternatives.filterNot(::isNull)
        return others.singleOrNull().takeIf { alternatives.size == 2 }
    }

    private fun kindOf(definition: SchemaDefinition): ModelKind? {
        val isChoice = alternativesOf(definition).isNotEmpty() && optionalOf(definition) == null
        val isUnion = isChoice || definition.discriminator != null
        val composed = ModelKind.Object.takeIf { isComposable(definition) }
        val structured = ModelKind.Union.takeIf { isUnion } ?: composed
        return ModelKind.Enum.takeIf { definition.enumeration != null } ?: structured
    }
}
