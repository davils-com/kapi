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

package com.davils.kapi.spec.asyncapi.compatibility

import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.model.schema.SchemaFormat
import com.davils.kapi.spec.core.collection.associateKeyed
import com.davils.kapi.spec.core.collection.frozenSet
import com.davils.kapi.spec.core.compatibility.FixedSchemaView
import com.davils.kapi.spec.core.compatibility.SchemaView
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.model.UriResolution
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal

private const val REFERENCE: String = "\$ref"

private const val NULL: String = "null"

private const val EXCLUSIVE_MINIMUM: String = "exclusiveMinimum"

private const val EXCLUSIVE_MAXIMUM: String = "exclusiveMaximum"

private val interpreted: Set<SchemaFormat> = setOf(SchemaFormat.AsyncApi, SchemaFormat.JsonSchema, SchemaFormat.OpenApi)

internal class ValueView(
    private val mapping: Value.Mapping,
    private val document: Document,
    private val schemaFormat: SchemaFormat,
) : SchemaView() {
    private val reference: String? = text(REFERENCE)

    private val resolved: Schema? by lazy {
        val base = mapping.location?.document
        val uri = reference?.let(UriResolution::parse)
        val target = uri?.let { parsed -> base?.let { found -> UriResolution.target(found, parsed) } }
        target?.let { found -> document.targets[found] as? Schema }
    }

    override val location: Location? get() = mapping.location

    override val target: SchemaView? get() = resolved?.let { schema -> of(schema, document) }

    override val isUnresolved: Boolean get() = reference != null && resolved == null

    override val isAccepting: Boolean? get() = null

    override val opaque: Value? get() = null

    override val types: Set<String>?
        get() {
            val written = mapping["type"]
            val types = textsOf(written) ?: return null
            val nullable = NULL.takeIf { schemaFormat == SchemaFormat.OpenApi && flag("nullable") }
            return (types + listOfNotNull(nullable)).frozenSet()
        }

    override val format: String? get() = text("format")

    override val pattern: String? get() = text("pattern")

    override val enumeration: List<Value>? get() = (mapping["enum"] as? Value.Sequence)?.items

    override val constant: Value? get() = mapping["const"]

    override val minLength: NumberValue? get() = number("minLength")

    override val maxLength: NumberValue? get() = number("maxLength")

    override val minimum: NumberValue? get() = number(EXCLUSIVE_MINIMUM) ?: number("minimum")

    override val isMinimumExclusive: Boolean get() = number(EXCLUSIVE_MINIMUM) != null || flag(EXCLUSIVE_MINIMUM)

    override val maximum: NumberValue? get() = number(EXCLUSIVE_MAXIMUM) ?: number("maximum")

    override val isMaximumExclusive: Boolean get() = number(EXCLUSIVE_MAXIMUM) != null || flag(EXCLUSIVE_MAXIMUM)

    override val multipleOf: NumberValue? get() = number("multipleOf")

    override val minItems: NumberValue? get() = number("minItems")

    override val maxItems: NumberValue? get() = number("maxItems")

    override val hasUniqueItems: Boolean get() = flag("uniqueItems")

    override val minProperties: NumberValue? get() = number("minProperties")

    override val maxProperties: NumberValue? get() = number("maxProperties")

    override val properties: Map<String, SchemaView> by lazy {
        val written = (mapping["properties"] as? Value.Mapping)?.entries.orEmpty()
        written.entries.associateKeyed { (name, value) -> name to nested(value) }
    }

    override val required: List<String> get() = textsOf(mapping["required"]).orEmpty()

    override val additionalProperties: SchemaView? get() = mapping["additionalProperties"]?.let(::nested)

    override val items: SchemaView?
        get() {
            val written = mapping["items"]
            return written?.takeIf { value -> value !is Value.Sequence }?.let(::nested)
        }

    override val allOf: List<SchemaView> get() = list("allOf")

    override val anyOf: List<SchemaView> get() = list("anyOf")

    override val oneOf: List<SchemaView> get() = list("oneOf")

    override val discriminator: String?
        get() {
            val written = mapping["discriminator"] as? Value.Mapping
            val property = written?.get("propertyName") as? Value.Text
            return text("discriminator") ?: property?.value
        }

    override val isDeprecated: Boolean get() = flag("deprecated")

    override val isReadOnly: Boolean get() = flag("readOnly")

    override val isWriteOnly: Boolean get() = flag("writeOnly")

    private fun nested(value: Value): SchemaView = viewOf(value = value, document = document, format = schemaFormat)

    private fun list(name: String): List<SchemaView> {
        val written = (mapping[name] as? Value.Sequence)?.items
        return written.orEmpty().map(::nested)
    }

    private fun text(name: String): String? = (mapping[name] as? Value.Text)?.value

    private fun number(name: String): NumberValue? = (mapping[name] as? Value.Number)?.value

    private fun flag(name: String): Boolean = (mapping[name] as? Value.Flag)?.isTrue == true

    private fun textsOf(value: Value?): List<String>? {
        val items = (value as? Value.Sequence)?.items ?: listOfNotNull(value)
        val texts = items.filterIsInstance<Value.Text>()
        return texts.map(Value.Text::value).takeIf { value != null }
    }

    companion object {
        fun of(schema: Schema, document: Document): SchemaView {
            val isRead = schema.format in interpreted
            if (!isRead) return OpaqueView(location = schema.location, value = schema.definition)
            return viewOf(value = schema.definition, document = document, format = schema.format)
        }

        fun viewOf(value: Value, document: Document, format: SchemaFormat): SchemaView = when (value) {
            is Value.Flag -> FixedSchemaView(location = value.location, isAccepting = value.isTrue)
            is Value.Mapping -> ValueView(mapping = value, document = document, schemaFormat = format)
            is Value.Null, is Value.Number, is Value.Text, is Value.Sequence -> opaqueOf(value)
        }

        private fun opaqueOf(value: Value): SchemaView = OpaqueView(location = value.location, value = value)
    }
}
