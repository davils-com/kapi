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

import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.info.ExternalDocs

public class SchemaDefinition internal constructor(builder: SchemaDefinitionBuilder) :
    Schema(builder) {
    public val id: String? = builder.id

    public val dialect: String? = builder.dialect

    public val anchor: String? = builder.anchor

    public val dynamicAnchor: String? = builder.dynamicAnchor

    public val comment: String? = builder.comment

    public val definitions: Map<String, Schema> = builder.definitions.toMap()

    public val reference: Reference? = builder.reference

    public val dynamicReference: String? = builder.dynamicReference

    public val types: Set<SchemaType> = builder.types.toSet()

    public val format: String? = builder.format

    public val minimum: Bound? = builder.minimum

    public val maximum: Bound? = builder.maximum

    public val multipleOf: NumberValue? = builder.multipleOf

    public val minLength: Int? = builder.minLength

    public val maxLength: Int? = builder.maxLength

    public val pattern: String? = builder.pattern

    public val items: Schema? = builder.items

    public val prefixItems: List<Schema> = builder.prefixItems.toList()

    public val contains: Schema? = builder.contains

    public val minContains: Int? = builder.minContains

    public val maxContains: Int? = builder.maxContains

    public val minItems: Int? = builder.minItems

    public val maxItems: Int? = builder.maxItems

    public val hasUniqueItems: Boolean = builder.hasUniqueItems

    public val unevaluatedItems: Schema? = builder.unevaluatedItems

    public val properties: Map<String, Schema> = builder.properties.toMap()

    public val patternProperties: Map<String, Schema> = builder.patternProperties.toMap()

    public val additionalProperties: Schema? = builder.additionalProperties

    public val propertyNames: Schema? = builder.propertyNames

    public val required: List<String> = builder.required.toList()

    public val dependentRequired: Map<String, List<String>> = builder.dependentRequired.toMap()

    public val dependentSchemas: Map<String, Schema> = builder.dependentSchemas.toMap()

    public val minProperties: Int? = builder.minProperties

    public val maxProperties: Int? = builder.maxProperties

    public val unevaluatedProperties: Schema? = builder.unevaluatedProperties

    public val allOf: List<Schema> = builder.allOf.toList()

    public val anyOf: List<Schema> = builder.anyOf.toList()

    public val oneOf: List<Schema> = builder.oneOf.toList()

    public val not: Schema? = builder.not

    public val condition: Schema? = builder.condition

    public val consequence: Schema? = builder.consequence

    public val alternative: Schema? = builder.alternative

    public val title: String? = builder.title

    public val description: String? = builder.description

    public val default: Value? = builder.default

    public val examples: List<Value> = builder.examples.toList()

    public val isDeprecated: Boolean = builder.isDeprecated

    public val isReadOnly: Boolean = builder.isReadOnly

    public val isWriteOnly: Boolean = builder.isWriteOnly

    public val contentEncoding: String? = builder.contentEncoding

    public val contentMediaType: String? = builder.contentMediaType

    public val contentSchema: Schema? = builder.contentSchema

    public val enumeration: List<Value>? = builder.enumeration?.toList()

    public val constant: Value? = builder.constant

    public val discriminator: Discriminator? = builder.discriminator

    public val xml: Xml? = builder.xml

    public val externalDocs: ExternalDocs? = builder.externalDocs

    public val unknownKeywords: Map<String, Value> = builder.unknownKeywords.toMap()

    override val identity: List<Any?>
        get() = listOf(
            id,
            dialect,
            anchor,
            dynamicAnchor,
            comment,
            definitions,
            reference,
            dynamicReference,
            types,
            format,
            minimum,
            maximum,
            multipleOf,
            minLength,
            maxLength,
            pattern,
            items,
            prefixItems,
            contains,
            minContains,
            maxContains,
            minItems,
            maxItems,
            hasUniqueItems,
            unevaluatedItems,
            properties,
            patternProperties,
            additionalProperties,
            propertyNames,
            required,
            dependentRequired,
            dependentSchemas,
            minProperties,
            maxProperties,
            unevaluatedProperties,
            allOf,
            anyOf,
            oneOf,
            not,
            condition,
            consequence,
            alternative,
            title,
            description,
            default,
            examples,
            isDeprecated,
            isReadOnly,
            isWriteOnly,
            contentEncoding,
            contentMediaType,
            contentSchema,
            enumeration,
            constant,
            discriminator,
            xml,
            externalDocs,
            unknownKeywords,
        )
}
