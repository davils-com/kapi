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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.openapi.compatibility

import com.davils.kapi.spec.core.collection.associateKeyed
import com.davils.kapi.spec.core.collection.frozenSet
import com.davils.kapi.spec.core.compatibility.FixedSchemaView
import com.davils.kapi.spec.core.compatibility.SchemaView
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.model.schema.FixedSchema
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.model.schema.SchemaType
import com.davils.kapi.spec.openapi.resolve
import com.davils.kore.core.annotation.DavilsInternal

internal class DefinitionView(
    private val definition: SchemaDefinition,
    private val document: Document,
) : SchemaView() {
    private val resolved: Schema? by lazy {
        definition.reference?.let { reference -> document.resolve(reference) as? Schema }
    }

    override val location: Location? get() = definition.location

    override val target: SchemaView? get() = resolved?.let { schema -> viewOf(schema, document) }

    override val isUnresolved: Boolean get() = definition.reference != null && resolved == null

    override val isAccepting: Boolean? get() = null

    override val opaque: Value? get() = null

    override val types: Set<String>?
        get() = definition.types.takeIf { types -> types.isNotEmpty() }?.let(::keywordsOf)

    override val format: String? get() = definition.format

    override val pattern: String? get() = definition.pattern

    override val enumeration: List<Value>? get() = definition.enumeration

    override val constant: Value? get() = definition.constant

    override val minLength: NumberValue? get() = numberOf(definition.minLength)

    override val maxLength: NumberValue? get() = numberOf(definition.maxLength)

    override val minimum: NumberValue? get() = definition.minimum?.value

    override val isMinimumExclusive: Boolean get() = definition.minimum?.isExclusive == true

    override val maximum: NumberValue? get() = definition.maximum?.value

    override val isMaximumExclusive: Boolean get() = definition.maximum?.isExclusive == true

    override val multipleOf: NumberValue? get() = definition.multipleOf

    override val minItems: NumberValue? get() = numberOf(definition.minItems)

    override val maxItems: NumberValue? get() = numberOf(definition.maxItems)

    override val hasUniqueItems: Boolean get() = definition.hasUniqueItems

    override val minProperties: NumberValue? get() = numberOf(definition.minProperties)

    override val maxProperties: NumberValue? get() = numberOf(definition.maxProperties)

    override val properties: Map<String, SchemaView> by lazy {
        definition.properties.entries.associateKeyed { (name, property) -> name to viewOf(property, document) }
    }

    override val required: List<String> get() = definition.required

    override val additionalProperties: SchemaView? get() = definition.additionalProperties?.let(::nested)

    override val items: SchemaView? get() = definition.items?.let(::nested)

    override val allOf: List<SchemaView> get() = definition.allOf.map(::nested)

    override val anyOf: List<SchemaView> get() = definition.anyOf.map(::nested)

    override val oneOf: List<SchemaView> get() = definition.oneOf.map(::nested)

    override val discriminator: String? get() = definition.discriminator?.propertyName

    override val isDeprecated: Boolean get() = definition.isDeprecated

    override val isReadOnly: Boolean get() = definition.isReadOnly

    override val isWriteOnly: Boolean get() = definition.isWriteOnly

    private fun nested(schema: Schema): SchemaView = viewOf(schema, document)

    private fun numberOf(value: Int?): NumberValue? = value?.let { number -> NumberValue.of(number.toLong()) }

    private fun keywordsOf(types: Set<SchemaType>): Set<String> = types.map(SchemaType::keyword).frozenSet()

    companion object {
        fun viewOf(schema: Schema, document: Document): SchemaView = when (schema) {
            is FixedSchema -> FixedSchemaView(location = schema.location, isAccepting = schema.isAccepting)
            is SchemaDefinition -> DefinitionView(definition = schema, document = document)
        }
    }
}
