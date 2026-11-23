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
import com.davils.kapi.spec.core.collection.frozen
import com.davils.kapi.spec.core.collection.frozenSet
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.info.ExternalDocs
import com.davils.kore.core.annotation.DavilsInternal

private fun frozenLists(source: Map<String, List<String>>): Map<String, List<String>> {
    val copy = KeyedMap<String, List<String>>()
    source.forEach { (key, value) -> copy[key] = value.frozen() }
    return copy.frozen()
}

/**
 * A schema written as an object: its keywords of JSON Schema and of OpenAPI, as they were written.
 *
 * The element is immutable and safe to share between threads. Every collection keeps the order of the document and
 * is empty when the schema does not write its keyword; every other keyword the schema leaves out is `null` or
 * `false`. A keyword of a later version than the document is reported by a reading and not taken over.
 *
 * @since 1.0.0
 */
public class SchemaDefinition internal constructor(builder: SchemaDefinitionBuilder) :
    Schema(builder) {
    /**
     * The `$id` of the schema, the URI it is known by and that its references resolve against; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val id: String? = builder.id

    /**
     * The `$schema` of the schema, the URI of the dialect it is written in; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val dialect: String? = builder.dialect

    /**
     * The `$anchor` of the schema, a name that a reference may use as fragment; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val anchor: String? = builder.anchor

    /**
     * The `$dynamicAnchor` of the schema, a name that `$dynamicRef` resolves at evaluation; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val dynamicAnchor: String? = builder.dynamicAnchor

    /**
     * The `$comment` of the schema, a note for its authors; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val comment: String? = builder.comment

    /**
     * The schemas of `$defs` by name, which references within the schema may use; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val definitions: Map<String, Schema> = builder.definitions.frozen()

    /**
     * The `$ref` of the schema. In OpenAPI 3.0 a schema with a reference holds nothing else; from 3.1 on the other
     * keywords apply beside it.
     *
     * A reading reports a schema that refers to nothing but references that lead back to it or never end.
     *
     * @since 1.0.0
     */
    public val reference: Reference? = builder.reference

    /**
     * The `$dynamicRef` of the schema as it was written, resolved only when a value is validated; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val dynamicReference: String? = builder.dynamicReference

    /**
     * The types of `type`, which is a name or, from OpenAPI 3.1 on, a list of names; empty when the schema allows every
     * type. A reading adds [SchemaType.Null] for `nullable: true` of OpenAPI 3.0 and reports a name it does not know.
     *
     * @since 1.0.0
     */
    public val types: Set<SchemaType> = builder.types.frozenSet()

    /**
     * The `format` of the value, such as `date-time` or `int64`, as it was written.
     *
     * @since 1.0.0
     */
    public val format: String? = builder.format

    /**
     * The lower limit of a number, from `minimum` and `exclusiveMinimum`.
     *
     * A reading reports a schema whose range of numbers is empty.
     *
     * @since 1.0.0
     */
    public val minimum: Bound? = builder.minimum

    /**
     * The upper limit of a number, from `maximum` and `exclusiveMaximum`.
     *
     * @since 1.0.0
     */
    public val maximum: Bound? = builder.maximum

    /**
     * The `multipleOf` a number is a multiple of; a reading reports one that is not larger than zero.
     *
     * @since 1.0.0
     */
    public val multipleOf: NumberValue? = builder.multipleOf

    /**
     * The `minLength` of a string in characters; a reading reports one larger than [maxLength].
     *
     * @since 1.0.0
     */
    public val minLength: Int? = builder.minLength

    /**
     * The `maxLength` of a string in characters.
     *
     * @since 1.0.0
     */
    public val maxLength: Int? = builder.maxLength

    /**
     * The `pattern` a string matches, a regular expression of ECMA-262 as it was written.
     *
     * A reading reports a pattern longer than its limit, one that does not compile and one that repeats a repeated
     * group, which can take very long on crafted input. The pattern is not run here.
     *
     * @since 1.0.0
     */
    public val pattern: String? = builder.pattern

    /**
     * The schema of `items`: of every item of an array or, from OpenAPI 3.1 on, of every item after [prefixItems].
     *
     * @since 1.0.0
     */
    public val items: Schema? = builder.items

    /**
     * The schemas of `prefixItems`, one for each leading item of an array by position; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val prefixItems: List<Schema> = builder.prefixItems.frozen()

    /**
     * The schema of `contains`, which at least one item of an array matches; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val contains: Schema? = builder.contains

    /**
     * The `minContains`: how many items at least match [contains]; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val minContains: Int? = builder.minContains

    /**
     * The `maxContains`: how many items at most match [contains]; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val maxContains: Int? = builder.maxContains

    /**
     * The `minItems` of an array; a reading reports one larger than [maxItems].
     *
     * @since 1.0.0
     */
    public val minItems: Int? = builder.minItems

    /**
     * The `maxItems` of an array.
     *
     * @since 1.0.0
     */
    public val maxItems: Int? = builder.maxItems

    /**
     * Whether the items of an array are unique, from `uniqueItems`.
     *
     * @since 1.0.0
     */
    public val hasUniqueItems: Boolean = builder.hasUniqueItems

    /**
     * The schema of `unevaluatedItems`, which every item that no other keyword evaluated matches; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val unevaluatedItems: Schema? = builder.unevaluatedItems

    /**
     * The schemas of `properties` by the name of the property.
     *
     * @since 1.0.0
     */
    public val properties: Map<String, Schema> = builder.properties.frozen()

    /**
     * The schemas of `patternProperties` by a regular expression that names of properties match; from OpenAPI 3.1 on.
     * A reading checks each expression as it checks [pattern].
     *
     * @since 1.0.0
     */
    public val patternProperties: Map<String, Schema> = builder.patternProperties.frozen()

    /**
     * The schema of `additionalProperties`, which every property that neither [properties] nor [patternProperties]
     * names matches; `true` and `false` are read as a [FixedSchema].
     *
     * @since 1.0.0
     */
    public val additionalProperties: Schema? = builder.additionalProperties

    /**
     * The schema of `propertyNames`, which every name of a property matches; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val propertyNames: Schema? = builder.propertyNames

    /**
     * The names of the properties an object must have, from `required`.
     *
     * @since 1.0.0
     */
    public val required: List<String> = builder.required.frozen()

    /**
     * The `dependentRequired` properties: for the name of a property, the properties an object must have when it has
     * that one; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val dependentRequired: Map<String, List<String>> = frozenLists(builder.dependentRequired)

    /**
     * The `dependentSchemas`: for the name of a property, the schema an object matches when it has that property; from
     * OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val dependentSchemas: Map<String, Schema> = builder.dependentSchemas.frozen()

    /**
     * The `minProperties` of an object; a reading reports one larger than [maxProperties].
     *
     * @since 1.0.0
     */
    public val minProperties: Int? = builder.minProperties

    /**
     * The `maxProperties` of an object.
     *
     * @since 1.0.0
     */
    public val maxProperties: Int? = builder.maxProperties

    /**
     * The schema of `unevaluatedProperties`, which every property that no other keyword evaluated matches; from
     * OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val unevaluatedProperties: Schema? = builder.unevaluatedProperties

    /**
     * The schemas of `allOf`, which a value matches all of.
     *
     * @since 1.0.0
     */
    public val allOf: List<Schema> = builder.allOf.frozen()

    /**
     * The schemas of `anyOf`, which a value matches at least one of.
     *
     * @since 1.0.0
     */
    public val anyOf: List<Schema> = builder.anyOf.frozen()

    /**
     * The schemas of `oneOf`, which a value matches exactly one of.
     *
     * @since 1.0.0
     */
    public val oneOf: List<Schema> = builder.oneOf.frozen()

    /**
     * The schema of `not`, which a value must not match.
     *
     * @since 1.0.0
     */
    public val not: Schema? = builder.not

    /**
     * The schema of `if`, which decides whether [consequence] or [alternative] applies; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val condition: Schema? = builder.condition

    /**
     * The schema of `then`, which a value matching [condition] matches as well; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val consequence: Schema? = builder.consequence

    /**
     * The schema of `else`, which a value not matching [condition] matches; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val alternative: Schema? = builder.alternative

    /**
     * The `title` of the schema.
     *
     * @since 1.0.0
     */
    public val title: String? = builder.title

    /**
     * The `description` of the schema, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The `default` value, which a consumer assumes when the value is missing; it is not checked against the schema.
     *
     * @since 1.0.0
     */
    public val default: Value? = builder.default

    /**
     * The values of `examples` and, after them, the `example` of OpenAPI 3.0, which OpenAPI 3.1 deprecates.
     *
     * @since 1.0.0
     */
    public val examples: List<Value> = builder.examples.frozen()

    /**
     * Whether the schema is `deprecated` and should no longer be used.
     *
     * @since 1.0.0
     */
    public val isDeprecated: Boolean = builder.isDeprecated

    /**
     * Whether the value is `readOnly`: sent in responses and not in requests.
     *
     * @since 1.0.0
     */
    public val isReadOnly: Boolean = builder.isReadOnly

    /**
     * Whether the value is `writeOnly`: sent in requests and not in responses.
     *
     * @since 1.0.0
     */
    public val isWriteOnly: Boolean = builder.isWriteOnly

    /**
     * The `contentEncoding` of a string that carries binary data, such as `base64`; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val contentEncoding: String? = builder.contentEncoding

    /**
     * The `contentMediaType` of the content a string carries, such as `image/png`; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val contentMediaType: String? = builder.contentMediaType

    /**
     * The `contentSchema` of the content a string carries once decoded; from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val contentSchema: Schema? = builder.contentSchema

    /**
     * The values of `enum`, one of which a value equals; `null` when the schema has no `enum`, empty when it lists no
     * value and so allows none.
     *
     * @since 1.0.0
     */
    public val enumeration: List<Value>? = builder.enumeration?.frozen()

    /**
     * The value of `const`, which a value equals; `null` when the schema has no `const`, and [Value.Null] for a `const`
     * of `null`. From OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val constant: Value? = builder.constant

    /**
     * The `discriminator` of the schema.
     *
     * @since 1.0.0
     */
    public val discriminator: Discriminator? = builder.discriminator

    /**
     * The `xml` object of the schema.
     *
     * @since 1.0.0
     */
    public val xml: Xml? = builder.xml

    /**
     * The `externalDocs` of the schema.
     *
     * @since 1.0.0
     */
    public val externalDocs: ExternalDocs? = builder.externalDocs

    /**
     * The keywords that the reader does not know, by name, with their values as written; a reading reports each of
     * them, `nullable` of OpenAPI 3.0 in a later document among them.
     *
     * @since 1.0.0
     */
    public val unknownKeywords: Map<String, Value> = builder.unknownKeywords.frozen()

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
