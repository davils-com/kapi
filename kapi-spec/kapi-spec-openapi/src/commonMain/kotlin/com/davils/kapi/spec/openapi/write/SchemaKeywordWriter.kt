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

package com.davils.kapi.spec.openapi.write

import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.model.schema.Bound
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.model.schema.SchemaType
import com.davils.kapi.spec.openapi.read.shape.Keys

internal class SchemaKeywordWriter(private val family: VersionFamily) {
    private val isLegacy: Boolean get() = family == VersionFamily.V30

    fun typing(out: Out, schema: SchemaDefinition) {
        val isNullable = isLegacy && SchemaType.Null in schema.types
        val written = schema.types.filterNot { type -> isLegacy && type == SchemaType.Null }
        val names = written.map { type -> yamlScalar(type.keyword) }
        out.node(Keys.TYPE, names.singleOrNull() ?: yamlSequence(names).takeIf { names.size > 1 })
        out.flag(Keys.NULLABLE, isNullable)
        out[Keys.FORMAT] = schema.format
        out.node(Keys.ENUM, schema.enumeration?.let { values -> yamlSequence(values.map(Nodes::of)) })
        out.node(Keys.CONST, schema.constant?.let(Nodes::of))
        examples(out, schema)
    }

    fun constraints(out: Out, schema: SchemaDefinition) {
        bound(out, schema.minimum, Keys.MINIMUM to Keys.EXCLUSIVE_MINIMUM)
        bound(out, schema.maximum, Keys.MAXIMUM to Keys.EXCLUSIVE_MAXIMUM)
        out.node(Keys.MULTIPLE_OF, schema.multipleOf?.let(Nodes::of))
        out.count(Keys.MIN_LENGTH, schema.minLength)
        out.count(Keys.MAX_LENGTH, schema.maxLength)
        out[Keys.PATTERN] = schema.pattern
        out.count(Keys.MIN_ITEMS, schema.minItems)
        out.count(Keys.MAX_ITEMS, schema.maxItems)
        out.count(Keys.MIN_CONTAINS, schema.minContains)
        out.count(Keys.MAX_CONTAINS, schema.maxContains)
        out.flag(Keys.UNIQUE_ITEMS, schema.hasUniqueItems)
        objects(out, schema)
    }

    private fun objects(out: Out, schema: SchemaDefinition) {
        out.count(Keys.MIN_PROPERTIES, schema.minProperties)
        out.count(Keys.MAX_PROPERTIES, schema.maxProperties)
        out.texts(Keys.REQUIRED, schema.required)
        out.map(Keys.DEPENDENT_REQUIRED, schema.dependentRequired, Nodes::texts)
    }

    private fun examples(out: Out, schema: SchemaDefinition) {
        if (isLegacy) return out.node(Keys.EXAMPLE, schema.examples.firstOrNull()?.let(Nodes::of))
        out.list(Keys.EXAMPLES, schema.examples, Nodes::of)
    }

    private fun bound(out: Out, bound: Bound?, keys: Pair<String, String>) {
        val value = bound?.let { declared -> Nodes.of(declared.value) }
        val isExclusive = bound?.isExclusive == true
        if (isLegacy) {
            out.node(keys.first, value)
            return out.flag(keys.second, isExclusive)
        }
        out.node(keys.first, value.takeUnless { isExclusive })
        out.node(keys.second, value.takeIf { isExclusive })
    }
}
