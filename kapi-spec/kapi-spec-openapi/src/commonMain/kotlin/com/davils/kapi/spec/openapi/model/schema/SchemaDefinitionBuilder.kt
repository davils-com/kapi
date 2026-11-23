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

package com.davils.kapi.spec.openapi.model.schema

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.info.ExternalDocs
import com.davils.kore.core.annotation.DavilsDsl

@DavilsDsl
public class SchemaDefinitionBuilder internal constructor() :
    ElementBuilder<SchemaDefinition>() {
    override val subject: String get() = "openapi schema"

    public var id: String? = null

    public var dialect: String? = null

    public var anchor: String? = null

    public var dynamicAnchor: String? = null

    public var comment: String? = null

    internal val definitions: MutableMap<String, Schema> = linkedMapOf()

    public var reference: Reference? = null

    public var dynamicReference: String? = null

    internal val types: MutableSet<SchemaType> = linkedSetOf()

    public var format: String? = null

    public var minimum: Bound? = null

    public var maximum: Bound? = null

    public var multipleOf: NumberValue? = null

    public var minLength: Int? = null

    public var maxLength: Int? = null

    public var pattern: String? = null

    public var items: Schema? = null

    internal val prefixItems: MutableList<Schema> = mutableListOf()

    public var contains: Schema? = null

    public var minContains: Int? = null

    public var maxContains: Int? = null

    public var minItems: Int? = null

    public var maxItems: Int? = null

    public var hasUniqueItems: Boolean = false

    public var unevaluatedItems: Schema? = null

    internal val properties: MutableMap<String, Schema> = linkedMapOf()

    internal val patternProperties: MutableMap<String, Schema> = linkedMapOf()

    public var additionalProperties: Schema? = null

    public var propertyNames: Schema? = null

    internal val required: MutableList<String> = mutableListOf()

    internal val dependentRequired: MutableMap<String, List<String>> = linkedMapOf()

    internal val dependentSchemas: MutableMap<String, Schema> = linkedMapOf()

    public var minProperties: Int? = null

    public var maxProperties: Int? = null

    public var unevaluatedProperties: Schema? = null

    internal val allOf: MutableList<Schema> = mutableListOf()

    internal val anyOf: MutableList<Schema> = mutableListOf()

    internal val oneOf: MutableList<Schema> = mutableListOf()

    public var not: Schema? = null

    public var condition: Schema? = null

    public var consequence: Schema? = null

    public var alternative: Schema? = null

    public var title: String? = null

    public var description: String? = null

    public var default: Value? = null

    internal val examples: MutableList<Value> = mutableListOf()

    public var isDeprecated: Boolean = false

    public var isReadOnly: Boolean = false

    public var isWriteOnly: Boolean = false

    public var contentEncoding: String? = null

    public var contentMediaType: String? = null

    public var contentSchema: Schema? = null

    public var enumeration: List<Value>? = null

    public var constant: Value? = null

    public var discriminator: Discriminator? = null

    public var xml: Xml? = null

    public var externalDocs: ExternalDocs? = null

    internal val unknownKeywords: MutableMap<String, Value> = linkedMapOf()

    override fun assemble(): SchemaDefinition = SchemaDefinition(this)
}
