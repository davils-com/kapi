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

@file:OptIn(ExperimentalSerializationApi::class, DavilsInternal::class)

package com.davils.kapi.spec.schema.internal.value

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.boolean
import com.davils.kapi.spec.schema.RefusalReason
import com.davils.kapi.spec.schema.internal.Refusal
import com.davils.kapi.spec.schema.internal.Trail
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

internal class YamlReader(private val maximumDepth: Int, private val maximumNodes: Int) {
    private var nodes: Int = 0

    private val scalars: Map<YamlTag, (YamlNode.Scalar) -> JsonElement?> = mapOf(
        YamlTag.NULL to { _ -> JsonNull },
        YamlTag.BOOLEAN to { scalar -> scalar.boolean()?.let(::JsonPrimitive) },
        YamlTag.INTEGER to ::number,
        YamlTag.FLOAT to ::number,
        YamlTag.STRING to { scalar -> JsonPrimitive(scalar.value) },
    )

    fun read(node: YamlNode): JsonElement = read(node, null, 0)

    private fun read(node: YamlNode, trail: Trail?, depth: Int): JsonElement {
        nodes++
        if (nodes > maximumNodes) refuse(RefusalReason.InstanceSize, "The YAML has more than $maximumNodes nodes", trail)
        if (depth > maximumDepth) refuse(RefusalReason.InstanceDepth, "The YAML is nested deeper than $maximumDepth levels", trail)
        return when (node) {
            is YamlNode.Scalar -> scalar(node, trail)
            is YamlNode.Sequence -> JsonArray(
                node.items.mapIndexed { index, item -> read(item, Trail(trail, index.toString()), depth + 1) },
            )
            is YamlNode.Mapping -> mapping(node, trail, depth)
        }
    }

    private fun mapping(node: YamlNode.Mapping, trail: Trail?, depth: Int): JsonElement {
        val members = LinkedHashMap<String, JsonElement>()
        node.entries.forEach { entry ->
            val key = (entry.key as? YamlNode.Scalar)?.value
                ?: refuse(RefusalReason.UnsupportedValue, "A key of the YAML mapping is no scalar", trail)
            val isDuplicate = key in members
            if (isDuplicate) refuse(RefusalReason.UnsupportedValue, "The key ${SafeText.quoted(key)} appears twice", trail)
            members[key] = read(entry.value, Trail(trail, key), depth + 1)
        }
        return JsonObject(members)
    }

    private fun scalar(node: YamlNode.Scalar, trail: Trail?): JsonElement {
        val converted = scalars[node.tag]?.invoke(node)
        return converted ?: refuse(
            RefusalReason.UnsupportedValue,
            "The YAML value ${SafeText.quoted(node.value)} with the tag ${SafeText.quoted(node.tag.uri)} is no JSON value",
            trail,
        )
    }

    private fun number(scalar: YamlNode.Scalar): JsonElement? {
        val decimal = Decimal.parse(scalar.value)
        return decimal?.let { JsonUnquotedLiteral(scalar.value) }
    }

    private fun refuse(reason: RefusalReason, message: String, trail: Trail?): Nothing = throw Refusal(reason, message, trail)
}
