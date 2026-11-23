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

package com.davils.kapi.spec.openapi.model.parameter

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.content.Example
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.schema.Schema

/**
 * A parameter object: one value of a request outside its body, in the path, the query, the query string, a header
 * or a cookie.
 *
 * The element is immutable and safe to share between threads. A parameter is identified by [name] and
 * [placement] together. A reading reports a parameter outside the query string that has both or neither of
 * [schema] and [content], or a [content] with more than one media type.
 *
 * @since 1.0.0
 */
public class Parameter internal constructor(builder: ParameterBuilder) :
    Element(builder) {
    /**
     * The `name` of the parameter, compared case-sensitively; for a path parameter, the name between braces in the path
     * template. Required by OpenAPI, so a reading reports a parameter that leaves it out and keeps it empty here.
     *
     * OpenAPI ignores a header parameter named `Accept`, `Content-Type` or `Authorization`.
     *
     * @since 1.0.0
     */
    public val name: String = builder.name

    /**
     * Where the parameter is sent, from `in`; required by OpenAPI. A reading reports a location it does not know, and
     * [ParameterPlacement.QueryString] before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val placement: ParameterPlacement = builder.placement

    /**
     * The `description` of the parameter, which may use CommonMark; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * Whether the parameter is `required`; `false` when the document leaves it out. A path parameter is always
     * required, and a reading reports one that does not say so.
     *
     * @since 1.0.0
     */
    public val isRequired: Boolean = builder.isRequired

    /**
     * Whether the parameter is `deprecated` and should no longer be used; `false` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val isDeprecated: Boolean = builder.isDeprecated

    /**
     * Whether a query parameter may be sent without a value, from `allowEmptyValue`; `false` when the document leaves
     * it out. OpenAPI advises against it and may remove it.
     *
     * @since 1.0.0
     */
    public val isEmptyValueAllowed: Boolean = builder.isEmptyValueAllowed

    /**
     * The `style` the value is serialized in; `null` when the document leaves it out, so that
     * [ParameterStyle.defaultFor] the placement applies. A reading reports a style the placement does not allow.
     *
     * @since 1.0.0
     */
    public val style: ParameterStyle? = builder.style

    /**
     * Whether arrays and objects are `explode`d into a value per item or property; `null` when the document leaves it
     * out, so that the default of the style applies, which is `true` for [ParameterStyle.Form] and
     * [ParameterStyle.Cookie] and `false` otherwise.
     *
     * @since 1.0.0
     */
    public val isExploded: Boolean? = builder.isExploded

    /**
     * Whether the value may hold the reserved characters of RFC 3986 unencoded, from `allowReserved`; `false` when the
     * document leaves it out.
     *
     * @since 1.0.0
     */
    public val isReservedAllowed: Boolean = builder.isReservedAllowed

    /**
     * The `schema` of the value; `null` when the parameter is described by [content] instead.
     *
     * @since 1.0.0
     */
    public val schema: Schema? = builder.schema

    /**
     * The `content` of the value by media type, which holds exactly one entry when the parameter is described by it;
     * empty when the parameter is described by [schema]. A parameter in the query string is described only by it.
     *
     * @since 1.0.0
     */
    public val content: Map<String, Referable<MediaType>> = builder.content.toMap()

    /**
     * The `example` of the value; `null` when the document leaves it out. A reading reports it beside [examples].
     *
     * @since 1.0.0
     */
    public val example: Value? = builder.example

    /**
     * The `examples` of the value by name, inline or by reference; empty when the document declares none.
     *
     * @since 1.0.0
     */
    public val examples: Map<String, Referable<Example>> = builder.examples.toMap()

    override val identity: List<Any?>
        get() = listOf(
            name,
            placement,
            description,
            isRequired,
            isDeprecated,
            isEmptyValueAllowed,
            style,
            isExploded,
            isReservedAllowed,
            schema,
            content,
            example,
            examples,
        )
}
