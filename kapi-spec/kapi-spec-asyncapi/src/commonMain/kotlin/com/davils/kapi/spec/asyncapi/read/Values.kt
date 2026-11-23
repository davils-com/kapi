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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.boolean
import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.diagnostic.shown
import com.davils.kapi.spec.core.collection.associateKeyed
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal

internal object Values {
    private val scalars: Map<YamlTag, (YamlNode.Scalar, Location) -> Value?> = mapOf(
        YamlTag.NULL to { _, location -> Value.Null(location) },
        YamlTag.BOOLEAN to { scalar, location -> Value.Flag(scalar.boolean() == true, location) },
        YamlTag.INTEGER to ::number,
        YamlTag.FLOAT to ::number,
        YamlTag.STRING to { scalar, location -> Value.Text(scalar.value, location) },
    )

    fun of(cursor: Cursor): Value = when (val node = cursor.node) {
        is YamlNode.Scalar -> scalar(cursor, node)
        is YamlNode.Sequence -> sequence(cursor)
        is YamlNode.Mapping -> mapping(cursor)
    }

    private fun sequence(cursor: Cursor): Value {
        val items = cursor.items()
        return Value.Sequence(items.map(::of), cursor.location)
    }

    private fun mapping(cursor: Cursor): Value {
        val entries = cursor.entries()
        val values = entries.associateKeyed { entry -> entry.key to of(entry.value) }
        return Value.Mapping(values, cursor.location)
    }

    private fun scalar(cursor: Cursor, node: YamlNode.Scalar): Value {
        val value = scalars[node.tag]?.invoke(node, cursor.location)
        if (value == null) {
            val message = "${shown(node.value)} is a value that JSON cannot express"
            cursor.report(DiagnosticCodes.SOURCE_NON_JSON_VALUE, message)
        }
        return value ?: Value.Text(node.value, cursor.location)
    }

    private fun number(scalar: YamlNode.Scalar, location: Location): Value? {
        val number = NumberValue.parseOrNull(scalar.value)
        return number?.let { parsed -> Value.Number(parsed, location) }
    }
}
