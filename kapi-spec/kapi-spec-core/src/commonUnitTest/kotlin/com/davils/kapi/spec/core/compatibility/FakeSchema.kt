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

package com.davils.kapi.spec.core.compatibility

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal

internal fun at(pointer: String): Location = Location(DocumentUri.MEMORY, JsonPointer.parse(pointer))

internal fun number(value: Long): NumberValue = NumberValue.of(value)

internal fun schema(configure: FakeSchema.() -> Unit = {}): FakeSchema {
    val schema = FakeSchema()
    schema.configure()
    return schema
}

internal class FakeSchema : SchemaView() {
    override var location: Location? = null

    override var target: SchemaView? = null

    override var isUnresolved: Boolean = false

    override var isAccepting: Boolean? = null

    override var opaque: Value? = null

    override var types: Set<String>? = null

    override var format: String? = null

    override var pattern: String? = null

    override var enumeration: List<Value>? = null

    override var constant: Value? = null

    override var minLength: NumberValue? = null

    override var maxLength: NumberValue? = null

    override var minimum: NumberValue? = null

    override var isMinimumExclusive: Boolean = false

    override var maximum: NumberValue? = null

    override var isMaximumExclusive: Boolean = false

    override var multipleOf: NumberValue? = null

    override var minItems: NumberValue? = null

    override var maxItems: NumberValue? = null

    override var hasUniqueItems: Boolean = false

    override var minProperties: NumberValue? = null

    override var maxProperties: NumberValue? = null

    override var properties: Map<String, SchemaView> = emptyMap()

    override var required: List<String> = emptyList()

    override var additionalProperties: SchemaView? = null

    override var items: SchemaView? = null

    override var allOf: List<SchemaView> = emptyList()

    override var anyOf: List<SchemaView> = emptyList()

    override var oneOf: List<SchemaView> = emptyList()

    override var discriminator: String? = null

    override var isDeprecated: Boolean = false

    override var isReadOnly: Boolean = false

    override var isWriteOnly: Boolean = false
}
