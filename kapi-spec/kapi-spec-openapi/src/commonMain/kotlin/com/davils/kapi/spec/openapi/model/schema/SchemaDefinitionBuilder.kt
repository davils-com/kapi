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

package com.davils.kapi.spec.openapi.model.schema

import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.info.ExternalDocs
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The builder of a [SchemaDefinition], filled by the readers of kapi. Every `var` holds the value of the keyword
 * of the same meaning in [SchemaDefinition].
 *
 * A builder is not safe to share between threads; the schema it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class SchemaDefinitionBuilder internal constructor() :
    ElementBuilder<SchemaDefinition>() {
    override val subject: String get() = "openapi schema"

    /**
     * The value of `$id`.
     *
     * @since 1.0.0
     */
    public var id: String? = null

    /**
     * The value of `$schema`.
     *
     * @since 1.0.0
     */
    public var dialect: String? = null

    /**
     * The value of `$anchor`.
     *
     * @since 1.0.0
     */
    public var anchor: String? = null

    /**
     * The value of `$dynamicAnchor`.
     *
     * @since 1.0.0
     */
    public var dynamicAnchor: String? = null

    /**
     * The value of `$comment`.
     *
     * @since 1.0.0
     */
    public var comment: String? = null

    internal val definitions: MutableMap<String, Schema> = KeyedMap()

    /**
     * The value of `$ref`.
     *
     * @since 1.0.0
     */
    public var reference: Reference? = null

    /**
     * The value of `$dynamicRef`.
     *
     * @since 1.0.0
     */
    public var dynamicReference: String? = null

    internal val types: MutableSet<SchemaType> = linkedSetOf()

    /**
     * The value of `format`.
     *
     * @since 1.0.0
     */
    public var format: String? = null

    /**
     * The lower limit, from `minimum` and `exclusiveMinimum`.
     *
     * @since 1.0.0
     */
    public var minimum: Bound? = null

    /**
     * The upper limit, from `maximum` and `exclusiveMaximum`.
     *
     * @since 1.0.0
     */
    public var maximum: Bound? = null

    /**
     * The value of `multipleOf`.
     *
     * @since 1.0.0
     */
    public var multipleOf: NumberValue? = null

    /**
     * The value of `minLength`.
     *
     * @since 1.0.0
     */
    public var minLength: Int? = null

    /**
     * The value of `maxLength`.
     *
     * @since 1.0.0
     */
    public var maxLength: Int? = null

    /**
     * The value of `pattern`.
     *
     * @since 1.0.0
     */
    public var pattern: String? = null

    /**
     * The value of `items`.
     *
     * @since 1.0.0
     */
    public var items: Schema? = null

    internal val prefixItems: MutableList<Schema> = mutableListOf()

    /**
     * The value of `contains`.
     *
     * @since 1.0.0
     */
    public var contains: Schema? = null

    /**
     * The value of `minContains`.
     *
     * @since 1.0.0
     */
    public var minContains: Int? = null

    /**
     * The value of `maxContains`.
     *
     * @since 1.0.0
     */
    public var maxContains: Int? = null

    /**
     * The value of `minItems`.
     *
     * @since 1.0.0
     */
    public var minItems: Int? = null

    /**
     * The value of `maxItems`.
     *
     * @since 1.0.0
     */
    public var maxItems: Int? = null

    /**
     * The value of `uniqueItems`.
     *
     * @since 1.0.0
     */
    public var hasUniqueItems: Boolean = false

    /**
     * The value of `unevaluatedItems`.
     *
     * @since 1.0.0
     */
    public var unevaluatedItems: Schema? = null

    internal val properties: MutableMap<String, Schema> = KeyedMap()

    internal val patternProperties: MutableMap<String, Schema> = KeyedMap()

    /**
     * The value of `additionalProperties`.
     *
     * @since 1.0.0
     */
    public var additionalProperties: Schema? = null

    /**
     * The value of `propertyNames`.
     *
     * @since 1.0.0
     */
    public var propertyNames: Schema? = null

    internal val required: MutableList<String> = mutableListOf()

    internal val dependentRequired: MutableMap<String, List<String>> = KeyedMap()

    internal val dependentSchemas: MutableMap<String, Schema> = KeyedMap()

    /**
     * The value of `minProperties`.
     *
     * @since 1.0.0
     */
    public var minProperties: Int? = null

    /**
     * The value of `maxProperties`.
     *
     * @since 1.0.0
     */
    public var maxProperties: Int? = null

    /**
     * The value of `unevaluatedProperties`.
     *
     * @since 1.0.0
     */
    public var unevaluatedProperties: Schema? = null

    internal val allOf: MutableList<Schema> = mutableListOf()

    internal val anyOf: MutableList<Schema> = mutableListOf()

    internal val oneOf: MutableList<Schema> = mutableListOf()

    /**
     * The value of `not`.
     *
     * @since 1.0.0
     */
    public var not: Schema? = null

    /**
     * The value of `if`.
     *
     * @since 1.0.0
     */
    public var condition: Schema? = null

    /**
     * The value of `then`.
     *
     * @since 1.0.0
     */
    public var consequence: Schema? = null

    /**
     * The value of `else`.
     *
     * @since 1.0.0
     */
    public var alternative: Schema? = null

    /**
     * The value of `title`.
     *
     * @since 1.0.0
     */
    public var title: String? = null

    /**
     * The value of `description`.
     *
     * @since 1.0.0
     */
    public var description: String? = null

    /**
     * The value of `default`.
     *
     * @since 1.0.0
     */
    public var default: Value? = null

    internal val examples: MutableList<Value> = mutableListOf()

    /**
     * The value of `deprecated`.
     *
     * @since 1.0.0
     */
    public var isDeprecated: Boolean = false

    /**
     * The value of `readOnly`.
     *
     * @since 1.0.0
     */
    public var isReadOnly: Boolean = false

    /**
     * The value of `writeOnly`.
     *
     * @since 1.0.0
     */
    public var isWriteOnly: Boolean = false

    /**
     * The value of `contentEncoding`.
     *
     * @since 1.0.0
     */
    public var contentEncoding: String? = null

    /**
     * The value of `contentMediaType`.
     *
     * @since 1.0.0
     */
    public var contentMediaType: String? = null

    /**
     * The value of `contentSchema`.
     *
     * @since 1.0.0
     */
    public var contentSchema: Schema? = null

    /**
     * The values of `enum`; `null` when the schema has no `enum`.
     *
     * @since 1.0.0
     */
    public var enumeration: List<Value>? = null

    /**
     * The value of `const`.
     *
     * @since 1.0.0
     */
    public var constant: Value? = null

    /**
     * The value of `discriminator`.
     *
     * @since 1.0.0
     */
    public var discriminator: Discriminator? = null

    /**
     * The value of `xml`.
     *
     * @since 1.0.0
     */
    public var xml: Xml? = null

    /**
     * The value of `externalDocs`.
     *
     * @since 1.0.0
     */
    public var externalDocs: ExternalDocs? = null

    internal val unknownKeywords: MutableMap<String, Value> = KeyedMap()

    override fun assemble(): SchemaDefinition = SchemaDefinition(this)
}
