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

@file:OptIn(ExperimentalSerializationApi::class)

package com.davils.kapi.runtime.testing.contract.internal

import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.schema.Bound
import com.davils.kapi.spec.openapi.model.schema.FixedSchema
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.model.schema.SchemaType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

internal class SchemaJson(private val isLegacy: Boolean, private val referenceOf: (Reference) -> String) {
    fun of(schema: Schema): JsonElement = when (schema) {
        is FixedSchema -> JsonPrimitive(schema.isAccepting)
        is SchemaDefinition -> definition(schema)
    }

    private fun definition(schema: SchemaDefinition): JsonObject {
        val out = Members(::of)
        out.text("\$ref" to schema.reference?.let(referenceOf))
        identity(out, schema)
        typing(out, schema)
        constraints(out, schema)
        structure(out, schema)
        composition(out, schema)
        out.flag("readOnly" to schema.isReadOnly)
        out.flag("writeOnly" to schema.isWriteOnly)
        out.text("contentEncoding" to schema.contentEncoding)
        out.text("contentMediaType" to schema.contentMediaType)
        schema.unknownKeywords.forEach { (name, value) -> out.value(name to value) }
        return out.build()
    }

    private fun identity(out: Members, schema: SchemaDefinition) {
        out.text("\$id" to schema.id)
        out.text("\$schema" to schema.dialect)
        out.text("\$anchor" to schema.anchor)
        out.text("\$dynamicAnchor" to schema.dynamicAnchor)
        out.text("\$dynamicRef" to schema.dynamicReference)
        out.map("\$defs" to schema.definitions)
    }

    private fun typing(out: Members, schema: SchemaDefinition) {
        val isNullable = isLegacy && SchemaType.Null in schema.types
        val written = schema.types.filterNot { type -> isLegacy && type == SchemaType.Null }
        val names = written.map(SchemaType::keyword)
        out.texts("type" to names.takeIf { names.size > 1 })
        out.text("type" to names.singleOrNull())
        out.flag("nullable" to isNullable)
        out.text("format" to schema.format)
        out.values("enum" to schema.enumeration)
        out.value("const" to schema.constant)
    }

    private fun constraints(out: Members, schema: SchemaDefinition) {
        bound(out = out, bound = schema.minimum, keys = "minimum" to "exclusiveMinimum")
        bound(out = out, bound = schema.maximum, keys = "maximum" to "exclusiveMaximum")
        out.number("multipleOf" to schema.multipleOf)
        out.count("minLength" to schema.minLength)
        out.count("maxLength" to schema.maxLength)
        out.text("pattern" to schema.pattern)
        out.count("minItems" to schema.minItems)
        out.count("maxItems" to schema.maxItems)
        out.count("minContains" to schema.minContains)
        out.count("maxContains" to schema.maxContains)
        out.flag("uniqueItems" to schema.hasUniqueItems)
        out.count("minProperties" to schema.minProperties)
        out.count("maxProperties" to schema.maxProperties)
        out.texts("required" to schema.required.takeIf(List<String>::isNotEmpty))
        out.lists("dependentRequired" to schema.dependentRequired)
    }

    private fun structure(out: Members, schema: SchemaDefinition) {
        out.schema("items" to schema.items)
        out.list("prefixItems" to schema.prefixItems)
        out.schema("contains" to schema.contains)
        out.schema("unevaluatedItems" to schema.unevaluatedItems)
        out.map("properties" to schema.properties)
        out.map("patternProperties" to schema.patternProperties)
        out.schema("additionalProperties" to schema.additionalProperties)
        out.schema("propertyNames" to schema.propertyNames)
        out.map("dependentSchemas" to schema.dependentSchemas)
        out.schema("unevaluatedProperties" to schema.unevaluatedProperties)
    }

    private fun composition(out: Members, schema: SchemaDefinition) {
        out.list("allOf" to schema.allOf)
        out.list("anyOf" to schema.anyOf)
        out.list("oneOf" to schema.oneOf)
        out.schema("not" to schema.not)
        out.schema("if" to schema.condition)
        out.schema("then" to schema.consequence)
        out.schema("else" to schema.alternative)
        out.schema("contentSchema" to schema.contentSchema)
    }

    private fun bound(out: Members, bound: Bound?, keys: Pair<String, String>) {
        val isExclusive = bound?.isExclusive == true
        val isMarked = !isLegacy && isExclusive
        val value = bound?.value
        out.number(keys.first to value?.takeUnless { isMarked })
        out.number(keys.second to value?.takeIf { isMarked })
        out.flag(keys.second to (isLegacy && isExclusive))
    }

    private class Members(private val write: (Schema) -> JsonElement) {
        private val entries: MutableList<Pair<String, JsonElement?>> = mutableListOf()

        fun text(entry: Pair<String, String?>) {
            entries += entry.first to entry.second?.let(::JsonPrimitive)
        }

        fun texts(entry: Pair<String, List<String>?>) {
            entries += entry.first to entry.second?.let(::textsOf)
        }

        fun count(entry: Pair<String, Int?>) {
            entries += entry.first to entry.second?.let(::JsonPrimitive)
        }

        fun number(entry: Pair<String, NumberValue?>) {
            entries += entry.first to entry.second?.let { number -> JsonUnquotedLiteral(number.literal) }
        }

        fun flag(entry: Pair<String, Boolean>) {
            entries += entry.first to JsonPrimitive(true).takeIf { entry.second }
        }

        fun value(entry: Pair<String, Value?>) {
            entries += entry.first to entry.second?.let(::valueOf)
        }

        fun values(entry: Pair<String, List<Value>?>) {
            entries += entry.first to entry.second?.let { values -> JsonArray(values.map(::valueOf)) }
        }

        fun schema(entry: Pair<String, Schema?>) {
            entries += entry.first to entry.second?.let(write)
        }

        fun list(entry: Pair<String, List<Schema>>) {
            entries += entry.first to JsonArray(entry.second.map(write)).takeIf { entry.second.isNotEmpty() }
        }

        fun map(entry: Pair<String, Map<String, Schema>>) {
            val members = entry.second.mapValues { (_, schema) -> write(schema) }
            entries += entry.first to JsonObject(members).takeIf { members.isNotEmpty() }
        }

        fun lists(entry: Pair<String, Map<String, List<String>>>) {
            val members = entry.second.mapValues { (_, names) -> textsOf(names) }
            entries += entry.first to JsonObject(members).takeIf { members.isNotEmpty() }
        }

        fun build(): JsonObject {
            val present = entries.mapNotNull { (key, value) -> value?.let { found -> key to found } }
            return JsonObject(present.toMap())
        }

        private fun textsOf(names: List<String>): JsonArray = JsonArray(names.map(::JsonPrimitive))

        private fun valueOf(value: Value): JsonElement = when (value) {
            is Value.Null -> JsonNull
            is Value.Flag -> JsonPrimitive(value.isTrue)
            is Value.Number -> JsonUnquotedLiteral(value.value.literal)
            is Value.Text -> JsonPrimitive(value.value)
            is Value.Sequence -> JsonArray(value.items.map(::valueOf))
            is Value.Mapping -> JsonObject(value.entries.mapValues { (_, entry) -> valueOf(entry) })
        }
    }
}
