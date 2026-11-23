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

import com.davils.kapi.spec.core.model.Element

/**
 * An XML object: how a schema maps to XML.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class Xml internal constructor(builder: XmlBuilder) : Element(builder) {
    /**
     * The `name` of the element or attribute; `null` when the document leaves it out, so that the name of the property
     * or of the schema applies.
     *
     * @since 1.0.0
     */
    public val name: String? = builder.name

    /**
     * The `namespace` of the element or attribute, an absolute IRI kept as written; `null` when the document leaves it
     * out.
     *
     * @since 1.0.0
     */
    public val namespace: String? = builder.namespace

    /**
     * The `prefix` used for the namespace; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val prefix: String? = builder.prefix

    /**
     * Whether the property is an `attribute` instead of an element; `false` when the document leaves it out.
     * OpenAPI 3.2 deprecates it in favor of [nodeType].
     *
     * @since 1.0.0
     */
    public val isAttribute: Boolean = builder.isAttribute

    /**
     * Whether the items of an array are `wrapped` in an element of their own; `false` when the document leaves it out.
     * OpenAPI 3.2 deprecates it in favor of [nodeType].
     *
     * @since 1.0.0
     */
    public val isWrapped: Boolean = builder.isWrapped

    /**
     * The `nodeType`: the kind of node the schema maps to; `null` when the document leaves it out and before
     * OpenAPI 3.2.

     *
     * A reading warns when [isAttribute] or [isWrapped] are written beside it, which OpenAPI 3.2 deprecates.
     *
     * @since 1.0.0
     */
    public val nodeType: XmlNodeType? = builder.nodeType

    override val identity: List<Any?> get() = listOf(name, namespace, prefix, isAttribute, isWrapped, nodeType)
}
