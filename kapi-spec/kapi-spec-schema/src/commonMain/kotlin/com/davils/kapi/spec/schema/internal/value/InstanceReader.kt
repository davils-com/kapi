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

package com.davils.kapi.spec.schema.internal.value

import com.davils.kapi.spec.schema.RefusalReason
import com.davils.kapi.spec.schema.internal.Refusal
import com.davils.kapi.spec.schema.internal.Trail
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class InstanceReader(private val maximumDepth: Int) {
    fun read(element: JsonElement): Instance = read(element, null, 0)

    private fun read(element: JsonElement, trail: Trail?, depth: Int): Instance {
        if (depth > maximumDepth) {
            throw Refusal(RefusalReason.InstanceDepth, "The value is nested deeper than $maximumDepth levels", trail)
        }
        return when (element) {
            is JsonPrimitive -> primitive(element, trail)
            is JsonArray -> array(element, trail, depth)
            is JsonObject -> members(element, trail, depth)
        }
    }

    private fun array(element: JsonArray, trail: Trail?, depth: Int): Instance {
        val items = element.mapIndexed { index, item -> read(item, Trail(trail, index.toString()), depth + 1) }
        return Instance.ArrayValue(items)
    }

    private fun members(element: JsonObject, trail: Trail?, depth: Int): Instance {
        val keys = ArrayList<String>(element.size)
        val values = ArrayList<Instance>(element.size)
        element.forEach { (key, value) ->
            keys += key
            values += read(value, Trail(trail, key), depth + 1)
        }
        return Instance.ObjectValue(keys, values)
    }

    private fun primitive(element: JsonPrimitive, trail: Trail?): Instance {
        val simple = simpleOf(element)
        return simple ?: number(element.content, trail)
    }

    private fun simpleOf(element: JsonPrimitive): Instance? {
        if (element is JsonNull) return Instance.NullValue
        val text = Instance.StringValue(element.content).takeIf { element.isString }
        return text ?: booleanOf(element.content)
    }

    private fun booleanOf(content: String): Instance? {
        val isBoolean = content == "true" || content == "false"
        return Instance.BooleanValue(content == "true").takeIf { isBoolean }
    }

    private fun number(content: String, trail: Trail?): Instance {
        val decimal = Decimal.parse(content) ?: throw Refusal(
            RefusalReason.UnsupportedValue,
            "The value is no JSON number of at most ${Decimal.MAXIMUM_LENGTH} characters",
            trail,
        )
        return Instance.NumberValue(decimal)
    }
}
