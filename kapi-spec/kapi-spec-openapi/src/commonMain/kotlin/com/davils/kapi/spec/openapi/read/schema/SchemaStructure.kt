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

import com.davils.kapi.spec.openapi.model.schema.SchemaDefinitionBuilder
import com.davils.kapi.spec.openapi.read.mapped
import com.davils.kapi.spec.openapi.read.shape.Fields
import com.davils.kapi.spec.openapi.read.shape.Keys

internal object SchemaStructure {
    fun fill(builder: SchemaDefinitionBuilder, fields: Fields) {
        arrays(builder, fields)
        objects(builder, fields)
        composition(builder, fields)
    }

    private fun arrays(builder: SchemaDefinitionBuilder, fields: Fields) {
        builder.items = fields.cursor(Keys.ITEMS)?.let(SchemaReader::read)
        builder.prefixItems.addAll(fields.items(Keys.PREFIX_ITEMS).map(SchemaReader::read))
        builder.contains = fields.cursor(Keys.CONTAINS)?.let(SchemaReader::read)
        builder.unevaluatedItems = fields.cursor(Keys.UNEVALUATED_ITEMS)?.let(SchemaReader::read)
    }

    private fun objects(builder: SchemaDefinitionBuilder, fields: Fields) {
        builder.properties.putAll(fields.entries(Keys.PROPERTIES).mapped(SchemaReader::read))
        builder.patternProperties.putAll(fields.entries(Keys.PATTERN_PROPERTIES).mapped(SchemaReader::read))
        builder.additionalProperties = fields.cursor(Keys.ADDITIONAL_PROPERTIES)?.let(SchemaReader::read)
        builder.propertyNames = fields.cursor(Keys.PROPERTY_NAMES)?.let(SchemaReader::read)
        builder.dependentSchemas.putAll(fields.entries(Keys.DEPENDENT_SCHEMAS).mapped(SchemaReader::read))
        builder.unevaluatedProperties = fields.cursor(Keys.UNEVALUATED_PROPERTIES)?.let(SchemaReader::read)
    }

    private fun composition(builder: SchemaDefinitionBuilder, fields: Fields) {
        builder.allOf.addAll(fields.items(Keys.ALL_OF).map(SchemaReader::read))
        builder.anyOf.addAll(fields.items(Keys.ANY_OF).map(SchemaReader::read))
        builder.oneOf.addAll(fields.items(Keys.ONE_OF).map(SchemaReader::read))
        builder.not = fields.cursor(Keys.NOT)?.let(SchemaReader::read)
        builder.condition = fields.cursor(Keys.IF)?.let(SchemaReader::read)
        builder.consequence = fields.cursor(Keys.THEN)?.let(SchemaReader::read)
        builder.alternative = fields.cursor(Keys.ELSE)?.let(SchemaReader::read)
        builder.contentSchema = fields.cursor(Keys.CONTENT_SCHEMA)?.let(SchemaReader::read)
    }
}
