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
 * The keywords of a JSON Schema that a [SchemaComparison] compares, read from the model of a specification, such as
 * the schema definition of OpenAPI or the schema value of AsyncAPI.
 *
 * A view answers for one schema and is immutable once built; a reference is a view whose [target] is the view it
 * leads to, resolved one hop at a time through the targets of the reading, so the comparison bounds and breaks every
 * chain and every cycle. A view of a schema that keeps nothing but a reference answers `null` and empty for every
 * keyword.
 *
 * @since 1.0.0
 */
@DavilsInternal
public abstract class SchemaView {
    /**
     * Where the schema was written, or `null` for a schema built in code.
     *
     * @since 1.0.0
     */
    public abstract val location: Location?

    /**
     * The schema the reference of this schema leads to, or `null` if this schema is no reference.
     *
     * @since 1.0.0
     */
    public abstract val target: SchemaView?

    /**
     * Whether this schema is a reference whose target the reading did not resolve.
     *
     * @since 1.0.0
     */
    public abstract val isUnresolved: Boolean

    /**
     * `true` or `false` for a boolean schema, `null` for a schema object.
     *
     * @since 1.0.0
     */
    public abstract val isAccepting: Boolean?

    /**
     * The schema as it was written, for a schema of a format the comparison does not read, such as Avro, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val opaque: Value?

    /**
     * The JSON types the schema allows, such as `string` and `null`, or `null` if it allows every type.
     *
     * @since 1.0.0
     */
    public abstract val types: Set<String>?

    /**
     * The `format`, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val format: String?

    /**
     * The `pattern`, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val pattern: String?

    /**
     * The values of `enum`, or `null` without `enum`.
     *
     * @since 1.0.0
     */
    public abstract val enumeration: List<Value>?

    /**
     * The value of `const`, or `null` without `const`.
     *
     * @since 1.0.0
     */
    public abstract val constant: Value?

    /**
     * The `minLength`, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val minLength: NumberValue?

    /**
     * The `maxLength`, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val maxLength: NumberValue?

    /**
     * The lower bound of a number, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val minimum: NumberValue?

    /**
     * Whether [minimum] itself is excluded.
     *
     * @since 1.0.0
     */
    public abstract val isMinimumExclusive: Boolean

    /**
     * The upper bound of a number, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val maximum: NumberValue?

    /**
     * Whether [maximum] itself is excluded.
     *
     * @since 1.0.0
     */
    public abstract val isMaximumExclusive: Boolean

    /**
     * The `multipleOf`, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val multipleOf: NumberValue?

    /**
     * The `minItems`, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val minItems: NumberValue?

    /**
     * The `maxItems`, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val maxItems: NumberValue?

    /**
     * Whether `uniqueItems` is `true`.
     *
     * @since 1.0.0
     */
    public abstract val hasUniqueItems: Boolean

    /**
     * The `minProperties`, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val minProperties: NumberValue?

    /**
     * The `maxProperties`, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val maxProperties: NumberValue?

    /**
     * The `properties` by their names.
     *
     * @since 1.0.0
     */
    public abstract val properties: Map<String, SchemaView>

    /**
     * The names of the `required` properties.
     *
     * @since 1.0.0
     */
    public abstract val required: List<String>

    /**
     * The `additionalProperties`, or `null` without the keyword.
     *
     * @since 1.0.0
     */
    public abstract val additionalProperties: SchemaView?

    /**
     * The schema of the `items` of an array, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val items: SchemaView?

    /**
     * The schemas of `allOf`.
     *
     * @since 1.0.0
     */
    public abstract val allOf: List<SchemaView>

    /**
     * The schemas of `anyOf`.
     *
     * @since 1.0.0
     */
    public abstract val anyOf: List<SchemaView>

    /**
     * The schemas of `oneOf`.
     *
     * @since 1.0.0
     */
    public abstract val oneOf: List<SchemaView>

    /**
     * The name of the property of the `discriminator`, or `null`.
     *
     * @since 1.0.0
     */
    public abstract val discriminator: String?

    /**
     * Whether the schema is `deprecated`.
     *
     * @since 1.0.0
     */
    public abstract val isDeprecated: Boolean

    /**
     * Whether the schema is `readOnly`.
     *
     * @since 1.0.0
     */
    public abstract val isReadOnly: Boolean

    /**
     * Whether the schema is `writeOnly`.
     *
     * @since 1.0.0
     */
    public abstract val isWriteOnly: Boolean

    /**
     * Returns the schema the references of this schema lead to, following at most [maximumHops] of them.
     *
     * @param maximumHops the most references to follow in a row.
     * @return the first schema that is no reference, or the schema reached after [maximumHops] references.
     * @since 1.0.0
     */
    public fun dereferenced(maximumHops: Int): SchemaView {
        val chain = generateSequence(this, SchemaView::target)
        return chain.take(maximumHops + 1).last()
    }
}
