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

package com.davils.kapi.spec.openapi.read.schema

import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinitionBuilder
import com.davils.kapi.spec.openapi.model.schema.SchemaType
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.Values
import com.davils.kapi.spec.openapi.read.mapped
import com.davils.kapi.spec.openapi.read.reference.ReferenceKind
import com.davils.kapi.spec.openapi.read.reference.References
import com.davils.kapi.spec.openapi.read.shape.Fields
import com.davils.kapi.spec.openapi.read.shape.Keys

internal object SchemaIdentity {
    private val references: Map<VersionFamily, (Cursor, ReferenceKind) -> Reference?> = mapOf(
        VersionFamily.V30 to References::strict,
        VersionFamily.V31 to References::read,
        VersionFamily.V32 to References::read,
    )

    private val legacy: Set<String> = setOf(Keys.NULLABLE)

    fun fill(builder: SchemaDefinitionBuilder, fields: Fields) {
        builder.id = fields.text(Keys.SCHEMA_ID)
        builder.dialect = fields.text(Keys.DIALECT)
        builder.anchor = fields.text(Keys.ANCHOR)
        builder.dynamicAnchor = fields.text(Keys.DYNAMIC_ANCHOR)
        builder.comment = fields.text(Keys.COMMENT)
        builder.definitions.putAll(fields.entries(Keys.DEFINITIONS).mapped(SchemaReader::read))
        builder.dynamicReference = fields.text(Keys.DYNAMIC_REFERENCE)
        builder.format = fields.text(Keys.FORMAT)
        builder.reference = referenceOf(fields)
        builder.types.addAll(typesOf(fields))
        fields.unknown.forEach { (name, cursor) -> unknown(builder, name, cursor) }
    }

    private fun referenceOf(fields: Fields): Reference? {
        val read = references.getValue(fields.cursor.scope.family)
        return fields.cursor(Keys.REFERENCE)?.let { read(fields.cursor, ReferenceKind.Schema) }
    }

    private fun typesOf(fields: Fields): List<SchemaType> {
        val written = fields.cursor(Keys.TYPE)
        val listed = written?.items().orEmpty()
        val cursors = listed.ifEmpty { listOfNotNull(written) }
        val nullable = SchemaType.Null.takeIf { fields.flag(Keys.NULLABLE) }
        return cursors.mapNotNull(::typeOf) + listOfNotNull(nullable)
    }

    private fun typeOf(cursor: Cursor): SchemaType? {
        val type = cursor.text?.let(SchemaType::fromKeyword)
        if (type == null) {
            val message = "A type is null, boolean, integer, number, string, array or object"
            cursor.report(DiagnosticCodes.SCHEMA_TYPE, message)
        }
        return type
    }

    private fun unknown(builder: SchemaDefinitionBuilder, name: String, cursor: Cursor) {
        builder.unknownKeywords[name] = Values.of(cursor)
        if (name in legacy) {
            val message = "$name is the form of OpenAPI 3.0, in 3.1 a schema lists null among its types"
            return cursor.report(DiagnosticCodes.SCHEMA_VERSION_FORM, message)
        }
        cursor.report(DiagnosticCodes.SCHEMA_UNKNOWN_KEYWORD, "$name is no keyword the reader knows")
    }
}
