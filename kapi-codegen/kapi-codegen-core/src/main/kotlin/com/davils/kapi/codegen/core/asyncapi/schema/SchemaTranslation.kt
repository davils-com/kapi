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

package com.davils.kapi.codegen.core.asyncapi.schema

import com.davils.kapi.codegen.core.asyncapi.GenerationCodes
import com.davils.kapi.codegen.core.pipeline.Findings
import com.davils.kapi.spec.asyncapi.model.schema.SchemaFormat
import com.davils.kapi.spec.core.value.Value

private const val REFERENCE: String = "\$ref"

private const val ITEMS: String = "items"

private const val TYPE: String = "type"

private const val ENUM: String = "enum"

private const val NULLABLE: String = "nullable"

private const val NULL: String = "null"

private const val DEPENDENCIES: String = "dependencies"

private const val DISCRIMINATOR: String = "discriminator"

private const val PROPERTY_NAME: String = "propertyName"

private val annotations: Set<String> = setOf(
    "description",
    "title",
    "summary",
    "\$comment",
    "examples",
    "example",
    "deprecated",
)

private val dropped: Set<String> = setOf("\$schema", "\$id", "definitions", "\$defs", NULLABLE)

private val single: Set<String> = setOf(
    "not",
    "additionalProperties",
    "propertyNames",
    "contains",
    "if",
    "then",
    "else",
    "unevaluatedProperties",
    "unevaluatedItems",
)

private val lists: Set<String> = setOf("allOf", "anyOf", "oneOf")

private val maps: Set<String> = setOf("properties", "patternProperties", "dependentSchemas")

private val exclusives: List<Pair<String, String>> = listOf(
    "exclusiveMinimum" to "minimum",
    "exclusiveMaximum" to "maximum",
)

internal class SchemaTranslation(
    private val report: Findings,
    private val format: SchemaFormat,
    private val reference: (Value.Text) -> String?,
) {
    private val handlers: Map<String, (String, Value, MutableMap<String, Value>) -> Unit> = buildMap {
        dropped.forEach { keyword -> this[keyword] = { _, _, _ -> } }
        single.forEach { keyword -> this[keyword] = { key, value, into -> into[key] = schema(value) } }
        lists.forEach { keyword -> this[keyword] = { key, value, into -> into[key] = listed(value) } }
        maps.forEach { keyword -> this[keyword] = { key, value, into -> into[key] = mapped(value) } }
        this[ITEMS] = { key, value, into -> items(key, value, into) }
        this["additionalItems"] = { _, value, _ -> unsupported(value, "additionalItems") }
        this[DEPENDENCIES] = { _, value, into -> dependencies(value, into) }
        this[DISCRIMINATOR] = { key, value, into -> into[key] = discriminator(value) }
    }

    fun schema(value: Value): Value {
        val mapping = value as? Value.Mapping ?: return value
        val text = mapping[REFERENCE] as? Value.Text
        return text?.let { found -> referenced(mapping, found) } ?: translated(mapping)
    }

    private fun referenced(value: Value.Mapping, text: Value.Text): Value {
        val siblings = value.entries.keys - REFERENCE - annotations
        if (siblings.isNotEmpty()) {
            val names = siblings.joinToString()
            val message = "The keywords $names beside a \$ref are ignored, as JSON Schema draft-07 ignores them"
            report.warn(GenerationCodes.SCHEMA_REFERENCE_SIBLING, message, value.location)
        }
        val rewritten = reference(text)?.let { found -> mapOf(REFERENCE to Value.Text(found)) }
        return Value.Mapping(rewritten.orEmpty(), value.location)
    }

    private fun translated(value: Value.Mapping): Value {
        val entries = linkedMapOf<String, Value>()
        value.entries.forEach { (key, item) ->
            val handler = handlers[key] ?: ::kept
            handler(key, item, entries)
        }
        if (format == SchemaFormat.OpenApi) openApi(value, entries)
        return Value.Mapping(entries, value.location)
    }

    private fun kept(key: String, value: Value, into: MutableMap<String, Value>) {
        into[key] = value
    }

    private fun listed(value: Value): Value {
        val sequence = value as? Value.Sequence ?: return value
        return Value.Sequence(sequence.items.map(::schema), sequence.location)
    }

    private fun mapped(value: Value): Value {
        val mapping = value as? Value.Mapping ?: return value
        return Value.Mapping(mapping.entries.mapValues { (_, item) -> schema(item) }, mapping.location)
    }

    private fun items(key: String, value: Value, into: MutableMap<String, Value>) {
        if (value is Value.Sequence) unsupported(value, "items as a list")
        into[key] = schema(value)
    }

    private fun dependencies(value: Value, into: MutableMap<String, Value>) {
        val entries = (value as? Value.Mapping)?.entries.orEmpty()
        val required = entries.filterValues { item -> item is Value.Sequence }
        val schemas = entries.filterValues { item -> item !is Value.Sequence }
        if (required.isNotEmpty()) into["dependentRequired"] = Value.Mapping(required, value.location)
        if (schemas.isNotEmpty()) into["dependentSchemas"] = mapped(Value.Mapping(schemas, value.location))
    }

    private fun discriminator(value: Value): Value {
        val text = value as? Value.Text ?: return value
        return Value.Mapping(mapOf(PROPERTY_NAME to text), text.location)
    }

    private fun openApi(value: Value.Mapping, entries: MutableMap<String, Value>) {
        exclusives.forEach { (exclusive, bound) ->
            val flag = entries[exclusive] as? Value.Flag
            val limit = entries[bound]?.takeIf { flag?.isTrue == true }
            if (flag != null) entries.remove(exclusive)
            limit?.let { found ->
                entries.remove(bound)
                entries[exclusive] = found
            }
        }
        val isNullable = (value[NULLABLE] as? Value.Flag)?.isTrue == true
        val type = entries[TYPE] as? Value.Text
        if (isNullable && type != null) entries[TYPE] = Value.Sequence(listOf(type, Value.Text(NULL)))
        val listed = (entries[ENUM] as? Value.Sequence)?.takeIf { isNullable }
        val isOpen = listed?.run { items.none { item -> item is Value.Null } } == true
        if (listed != null && isOpen) entries[ENUM] = Value.Sequence(listed.items + Value.Null(), listed.location)
    }

    private fun unsupported(value: Value, keyword: String) {
        val message = "The generator has no Kotlin for $keyword of JSON Schema draft-07"
        report.error(GenerationCodes.SCHEMA_UNSUPPORTED, message, value.location)
    }
}
