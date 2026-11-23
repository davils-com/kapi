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

package com.davils.kapi.spec.core.compatibility

import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The view of a boolean schema, `true` or `false`, which has no keywords.
 *
 * @property location where the schema was written, or `null`.
 * @property isAccepting whether the schema accepts every value or none.
 * @since 1.0.0
 */
@DavilsInternal
public class FixedSchemaView(
    override val location: Location?,
    override val isAccepting: Boolean,
) : SchemaView() {
    override val target: SchemaView? get() = null

    override val isUnresolved: Boolean get() = false

    override val opaque: Value? get() = null

    override val types: Set<String>? get() = null

    override val format: String? get() = null

    override val pattern: String? get() = null

    override val enumeration: List<Value>? get() = null

    override val constant: Value? get() = null

    override val minLength: NumberValue? get() = null

    override val maxLength: NumberValue? get() = null

    override val minimum: NumberValue? get() = null

    override val isMinimumExclusive: Boolean get() = false

    override val maximum: NumberValue? get() = null

    override val isMaximumExclusive: Boolean get() = false

    override val multipleOf: NumberValue? get() = null

    override val minItems: NumberValue? get() = null

    override val maxItems: NumberValue? get() = null

    override val hasUniqueItems: Boolean get() = false

    override val minProperties: NumberValue? get() = null

    override val maxProperties: NumberValue? get() = null

    override val properties: Map<String, SchemaView> get() = emptyMap()

    override val required: List<String> get() = emptyList()

    override val additionalProperties: SchemaView? get() = null

    override val items: SchemaView? get() = null

    override val allOf: List<SchemaView> get() = emptyList()

    override val anyOf: List<SchemaView> get() = emptyList()

    override val oneOf: List<SchemaView> get() = emptyList()

    override val discriminator: String? get() = null

    override val isDeprecated: Boolean get() = false

    override val isReadOnly: Boolean get() = false

    override val isWriteOnly: Boolean get() = false
}
