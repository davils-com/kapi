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

package com.davils.kapi.spec.openapi.read.schema

import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.model.schema.FixedSchemaBuilder
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinitionBuilder
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.read
import com.davils.kapi.spec.openapi.read.shape.ObjectShape
import com.davils.kapi.spec.openapi.read.shape.Shapes
import com.davils.kore.core.annotation.DavilsInternal

internal object SchemaReader {
    private val shapes: Map<VersionFamily, ObjectShape> = mapOf(
        VersionFamily.V30 to Shapes.schema30,
        VersionFamily.V31 to Shapes.schema31,
        VersionFamily.V32 to Shapes.schema31,
    )

    fun read(cursor: Cursor): Schema {
        val isAccepting = cursor.flag() ?: return definition(cursor)
        val builder = FixedSchemaBuilder()
        builder.location = cursor.location
        builder.isAccepting = isAccepting
        return builder.build()
    }

    private fun definition(cursor: Cursor): Schema {
        val shape = shapes.getValue(cursor.scope.family)
        return cursor.read(shape, SchemaDefinitionBuilder()) { fields ->
            SchemaIdentity.fill(this, fields)
            SchemaConstraints.fill(this, fields)
            SchemaStructure.fill(this, fields)
            SchemaAnnotations.fill(this, fields)
        }
    }
}
