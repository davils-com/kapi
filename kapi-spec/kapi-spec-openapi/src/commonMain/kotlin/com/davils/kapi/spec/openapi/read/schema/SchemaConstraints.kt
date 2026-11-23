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

import com.davils.kapi.spec.openapi.model.schema.Bound
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinitionBuilder
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.Values
import com.davils.kapi.spec.openapi.read.shape.Fields
import com.davils.kapi.spec.openapi.read.shape.Keys

internal object SchemaConstraints {
    private val lowest: Comparator<Bound> = compareBy(Bound::value)

    fun fill(builder: SchemaDefinitionBuilder, fields: Fields) {
        builder.minimum = bound(fields, Keys.MINIMUM, Keys.EXCLUSIVE_MINIMUM)?.maxWithOrNull(lowest)
        builder.maximum = bound(fields, Keys.MAXIMUM, Keys.EXCLUSIVE_MAXIMUM)?.minWithOrNull(lowest)
        builder.multipleOf = fields.number(Keys.MULTIPLE_OF)
        builder.minLength = fields.count(Keys.MIN_LENGTH)
        builder.maxLength = fields.count(Keys.MAX_LENGTH)
        builder.pattern = fields.text(Keys.PATTERN)
        counts(builder, fields)
        builder.required.addAll(fields.texts(Keys.REQUIRED))
        fields.entries(Keys.DEPENDENT_REQUIRED).forEach { entry ->
            builder.dependentRequired[entry.key] = entry.value.items().mapNotNull(Cursor::text)
        }
        builder.enumeration = fields.cursor(Keys.ENUM)?.let { fields.items(Keys.ENUM).map(Values::of) }
        builder.constant = fields.value(Keys.CONST)
    }

    private fun counts(builder: SchemaDefinitionBuilder, fields: Fields) {
        builder.minItems = fields.count(Keys.MIN_ITEMS)
        builder.maxItems = fields.count(Keys.MAX_ITEMS)
        builder.minContains = fields.count(Keys.MIN_CONTAINS)
        builder.maxContains = fields.count(Keys.MAX_CONTAINS)
        builder.hasUniqueItems = fields.flag(Keys.UNIQUE_ITEMS)
        builder.minProperties = fields.count(Keys.MIN_PROPERTIES)
        builder.maxProperties = fields.count(Keys.MAX_PROPERTIES)
    }

    private fun bound(fields: Fields, name: String, exclusiveName: String): List<Bound>? {
        val exclusive = fields.cursor(exclusiveName)
        val isFlagged = exclusive?.flag() == true
        val strict = fields.number(exclusiveName)?.let { value -> Bound(value, isExclusive = true) }
        val plain = fields.number(name)?.let { value -> Bound(value, isFlagged) }
        return listOfNotNull(strict, plain).takeIf { bounds -> bounds.isNotEmpty() }
    }
}
