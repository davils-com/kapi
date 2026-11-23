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

package com.davils.kapi.spec.openapi.read.shape

import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.value.ExtensionName
import com.davils.kapi.spec.core.value.Extensions
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.Entry
import com.davils.kapi.spec.openapi.read.Values
import com.davils.kore.core.annotation.DavilsInternal

private const val MERGE_KEY: String = "<<"

internal class ShapeCheck private constructor(
    private val family: VersionFamily,
    private val shape: ObjectShape,
) {
    private val present: MutableMap<String, Cursor> = KeyedMap()

    private val extensions: MutableMap<ExtensionName, Value> = KeyedMap()

    private val unknown: MutableMap<String, Cursor> = KeyedMap()

    private fun fields(cursor: Cursor): Fields {
        cursor.entries().forEach(::sort)
        val missing = shape.required.filter { rule -> rule.isRequiredIn(family) && rule.name !in present }
        missing.forEach { rule ->
            cursor.report(DiagnosticCodes.FIELD_MISSING, "${shape.subject} needs the field ${rule.name}")
        }
        return Fields(
            cursor = cursor,
            present = present,
            extensions = Extensions(extensions),
            unknown = unknown,
        )
    }

    private fun sort(entry: Entry) {
        val extension = ExtensionName.parseOrNull(entry.key)
        if (extension != null) {
            extensions[extension] = Values.of(entry.value)
            return
        }
        declared(entry)
    }

    private fun declared(entry: Entry) {
        val rule = shape.rule(entry.key) ?: return undeclared(entry)
        if (!rule.isIn(family)) {
            val message = "${entry.key} is no field of ${shape.subject} in OpenAPI ${family.prefix}"
            return entry.report(DiagnosticCodes.FIELD_VERSION, message)
        }
        typed(entry, rule)
    }

    private fun typed(entry: Entry, rule: FieldRule) {
        if (rule.kind.accepts(entry.value.node)) {
            present[entry.key] = entry.value
            return
        }
        val message = "${entry.key} of ${shape.subject} must be ${rule.kind.label}"
        entry.value.report(DiagnosticCodes.FIELD_TYPE, message)
    }

    private fun undeclared(entry: Entry) {
        if (entry.key == MERGE_KEY) {
            val message = "A merge key is only read when the reader was told to accept merge keys"
            return entry.report(DiagnosticCodes.SOURCE_MERGE_KEY, message)
        }
        if (shape.isOpen) {
            unknown[entry.key] = entry.value
            return
        }
        unknown(entry)
    }

    private fun unknown(entry: Entry) {
        val nearest = Suggestions.nearest(entry.key, shape.names)
        val hint = nearest?.let { name -> ", did you mean $name?" }
        val message = "${entry.key} is no field of ${shape.subject}" + hint.orEmpty()
        entry.report(DiagnosticCodes.FIELD_UNKNOWN, message)
    }

    companion object {
        fun fields(cursor: Cursor, shape: ObjectShape): Fields? {
            if (!cursor.isMapping) {
                cursor.report(DiagnosticCodes.FIELD_TYPE, "${shape.subject} must be an object")
                return null
            }
            return ShapeCheck(cursor.scope.family, shape).fields(cursor)
        }
    }
}
