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

import com.davils.kapi.spec.core.collection.frozen
import com.davils.kapi.spec.core.model.Element
import com.davils.kore.core.annotation.DavilsInternal

/**
 * A discriminator object: the property whose value tells which of the schemas of a `oneOf`, an `anyOf` or an
 * `allOf` hierarchy a value matches.
 *
 * The element is immutable and safe to share between threads. It is a hint for tools; validation does not depend on
 * it.
 *
 * @since 1.0.0
 */
public class Discriminator internal constructor(builder: DiscriminatorBuilder) :
    Element(builder) {
    /**
     * The `propertyName` of the property that holds the discriminating value; required by OpenAPI, so a reading reports
     * a discriminator that leaves it out and keeps it empty here.
     *
     * @since 1.0.0
     */
    public val propertyName: String = builder.propertyName

    /**
     * The `mapping` from values of the property to schemas, each given by the name of a schema of the components or by
     * a reference, in the order of the document; empty when the discriminator declares none, so that a value names a
     * schema of the components itself.
     *
     * A reading reports a name that is no schema of the components.
     *
     * @since 1.0.0
     */
    public val mapping: Map<String, String> = builder.mapping.frozen()

    /**
     * The `defaultMapping`: the schema, by name or by reference, of a value whose discriminating property is missing
     * or maps to no schema; `null` when the document leaves it out and before OpenAPI 3.2.
     *
     * A reading reports a name that is no schema of the components, as it does for [mapping].
     *
     * @since 1.0.0
     */
    public val defaultMapping: String? = builder.defaultMapping

    override val identity: List<Any?> get() = listOf(propertyName, mapping, defaultMapping)
}
