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

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.model.schema.SchemaBuilder
import com.davils.kapi.spec.asyncapi.model.schema.SchemaFormat
import com.davils.kapi.spec.asyncapi.read.reference.ReferenceKind
import com.davils.kapi.spec.asyncapi.read.reference.References
import com.davils.kapi.spec.asyncapi.read.shape.FieldKind
import com.davils.kapi.spec.asyncapi.read.shape.Fields
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.asyncapi.read.shape.Shapes
import com.davils.kapi.spec.core.model.Referable
import com.davils.kore.core.annotation.DavilsInternal

internal object SchemaReader {
    private val data: Set<String> = setOf("const", "enum", "default", "examples", "example")

    private val names: Set<String> = setOf(
        "properties",
        "patternProperties",
        "definitions",
        "${'$'}defs",
        "dependencies",
        "dependentSchemas",
    )

    private val resolvable: Set<SchemaFormat> = setOf(
        SchemaFormat.AsyncApi,
        SchemaFormat.JsonSchema,
        SchemaFormat.OpenApi,
    )

    fun slot(fields: Fields, name: String): Referable<Schema>? =
        fields.cursor(name)?.referable(ReferenceKind.Schema, ::schema)

    fun schema(cursor: Cursor): Schema? {
        val isMultiFormat = cursor.entries().any { entry -> entry.key == Keys.SCHEMA_FORMAT }
        if (isMultiFormat) return multiFormat(cursor)
        return plain(cursor)
    }

    private fun plain(cursor: Cursor): Schema? {
        if (!FieldKind.Schema.accepts(cursor.node)) {
            cursor.report(DiagnosticCodes.FIELD_TYPE, "A schema must be an object or a boolean")
            return null
        }
        nested(cursor, isNameMap = false)
        val builder = SchemaBuilder()
        builder.location = cursor.location
        builder.definition = Values.of(cursor)
        return builder.build()
    }

    private fun multiFormat(cursor: Cursor): Schema = cursor.read(Shapes.multiFormatSchema, SchemaBuilder()) { fields ->
        val written = fields.text(Keys.SCHEMA_FORMAT)
        schemaFormat = written
        format = written?.let(SchemaFormats::of) ?: format
        val body = fields.cursor(Keys.SCHEMA)
        body?.let { declared -> definition = Values.of(declared) }
        body?.takeIf { format in resolvable }?.let { declared -> nested(declared, isNameMap = false) }
    }

    private fun nested(cursor: Cursor, isNameMap: Boolean) {
        val entries = cursor.entries()
        val isReference = !isNameMap && entries.any(References::isNested)
        if (isReference) {
            References.read(cursor, ReferenceKind.Schema)
            return
        }
        val schemas = entries.filterNot { entry -> !isNameMap && entry.key in data }
        schemas.forEach { entry -> nested(entry.value, isNameMap = !isNameMap && entry.key in names) }
        cursor.items().forEach { item -> nested(item, isNameMap = false) }
    }
}
