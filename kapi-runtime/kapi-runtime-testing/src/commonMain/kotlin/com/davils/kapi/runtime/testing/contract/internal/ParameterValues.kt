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

import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.openapi.model.parameter.ParameterStyle
import com.davils.kapi.spec.openapi.model.schema.SchemaType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

private val delimiters: Map<ParameterStyle, String> =
    mapOf(ParameterStyle.SpaceDelimited to " ", ParameterStyle.PipeDelimited to "|")

internal class ParameterValues(private val shapes: Shapes) {
    fun queryValues(shape: ParameterShape, query: List<Pair<String, String>>, declared: Set<String>): List<String> {
        val isObject = isObject(shape)
        val isDeep = isObject && shape.style == ParameterStyle.DeepObject
        val isSpread = isObject && shape.style == ParameterStyle.Form && shape.isExploded
        val direct = query.filter { (key, _) -> key == shape.name }
        val prefix = "${shape.name}["
        val isNested = query.any { (key, _) -> (isDeep && key.startsWith(prefix)) || (isSpread && key !in declared) }
        val values = direct.map { (_, value) -> value }
        val marker = listOfNotNull("".takeIf { isNested })
        return values.ifEmpty { marker }
    }

    fun json(
        shape: ParameterShape,
        raw: List<String>,
        query: List<Pair<String, String>>,
        declared: Set<String>,
    ): JsonElement {
        val types = shapes.types(shape.schema)
        if (SchemaType.Array in types) return array(shape, raw)
        return objectOrScalar(shape = shape, raw = raw, query = query, declared = declared)
    }

    private fun objectOrScalar(
        shape: ParameterShape,
        raw: List<String>,
        query: List<Pair<String, String>>,
        declared: Set<String>,
    ): JsonElement {
        if (isObject(shape)) return objectOf(shape = shape, raw = raw, query = query, declared = declared)
        return scalar(text = stripped(shape, raw.first()), types = shapes.types(shape.schema))
    }

    private fun isObject(shape: ParameterShape): Boolean {
        val types = shapes.types(shape.schema)
        return SchemaType.Object in types && SchemaType.Array !in types
    }

    private fun array(shape: ParameterShape, raw: List<String>): JsonArray {
        val types = shapes.types(shapes.items(shape.schema))
        val isRepeated = shape.isExploded && shape.style == ParameterStyle.Form
        val items = raw.takeIf { isRepeated } ?: split(shape, raw.first())
        return JsonArray(items.map { text -> scalar(text = text, types = types) })
    }

    private fun objectOf(
        shape: ParameterShape,
        raw: List<String>,
        query: List<Pair<String, String>>,
        declared: Set<String>,
    ): JsonObject {
        val pairs = pairsOf(shape = shape, raw = raw, query = query, declared = declared)
        val members = pairs.associate { (key, value) ->
            key to scalar(text = value, types = shapes.types(shapes.property(shape.schema, key)))
        }
        return JsonObject(members)
    }

    private fun pairsOf(
        shape: ParameterShape,
        raw: List<String>,
        query: List<Pair<String, String>>,
        declared: Set<String>,
    ): List<Pair<String, String>> {
        if (shape.style == ParameterStyle.DeepObject) return deepPairs(shape.name, query)
        return spreadPairs(shape = shape, raw = raw, query = query, declared = declared)
    }

    private fun spreadPairs(
        shape: ParameterShape,
        raw: List<String>,
        query: List<Pair<String, String>>,
        declared: Set<String>,
    ): List<Pair<String, String>> {
        val isForm = shape.style == ParameterStyle.Form
        if (isForm) return formPairs(shape = shape, raw = raw, query = query, declared = declared)
        return listedPairs(shape, split(shape, raw.first()))
    }

    private fun deepPairs(name: String, query: List<Pair<String, String>>): List<Pair<String, String>> {
        val nested = query.filter { (key, _) -> key.startsWith("$name[") && key.endsWith(']') }
        return nested.map { (key, value) ->
            key.substring(
                startIndex = name.length + 1,
                endIndex = key.length - 1,
            ) to value
        }
    }

    private fun formPairs(
        shape: ParameterShape,
        raw: List<String>,
        query: List<Pair<String, String>>,
        declared: Set<String>,
    ): List<Pair<String, String>> {
        if (shape.isExploded) return query.filter { (key, _) -> key !in declared }
        return alternating(raw.first().split(','))
    }

    private fun listedPairs(shape: ParameterShape, items: List<String>): List<Pair<String, String>> {
        if (shape.isExploded) return items.map { item -> item.substringBefore('=') to item.substringAfter('=', "") }
        return alternating(items)
    }

    private fun alternating(items: List<String>): List<Pair<String, String>> =
        items.chunked(2).map { chunk -> chunk.first() to chunk.getOrElse(1) { "" } }

    private fun split(shape: ParameterShape, text: String): List<String> {
        if (shape.style == ParameterStyle.Matrix) return matrixItems(shape, text)
        val label = ".".takeIf { shape.isExploded && shape.style == ParameterStyle.Label }
        val delimiter = delimiters[shape.style] ?: label ?: ","
        return stripped(shape, text).split(delimiter)
    }

    private fun matrixItems(shape: ParameterShape, text: String): List<String> {
        val prefix = "${shape.name}="
        if (!shape.isExploded) return text.removePrefix(";$prefix").split(',')
        val parts = text.split(';').filter(String::isNotEmpty)
        return parts.map { part -> part.removePrefix(prefix) }
    }

    private fun stripped(shape: ParameterShape, text: String): String {
        val label = text.removePrefix(".").takeIf { shape.style == ParameterStyle.Label }
        val matrix = text.removePrefix(";${shape.name}=").takeIf { shape.style == ParameterStyle.Matrix }
        return label ?: matrix ?: text
    }

    private fun scalar(text: String, types: Set<SchemaType>): JsonElement {
        val isNumeric = SchemaType.Integer in types || SchemaType.Number in types
        val parsed = NumberValue.parseOrNull(text)?.takeIf { isNumeric }
        val number = parsed?.let { found -> JsonUnquotedLiteral(found.literal) }
        val isTrue = text.toBooleanStrictOrNull()?.takeIf { SchemaType.Boolean in types }
        val flag = isTrue?.let(::JsonPrimitive)
        val isNull = text.isEmpty() && SchemaType.Null in types
        return number ?: flag ?: JsonNull.takeIf { isNull } ?: JsonPrimitive(text)
    }
}
