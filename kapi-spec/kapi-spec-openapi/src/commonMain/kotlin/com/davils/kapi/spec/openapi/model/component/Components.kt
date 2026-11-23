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

package com.davils.kapi.spec.openapi.model.component

import com.davils.kapi.spec.core.collection.frozen
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.content.Example
import com.davils.kapi.spec.openapi.model.content.Link
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.content.RequestBody
import com.davils.kapi.spec.openapi.model.content.Response
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.path.Callback
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.security.SecurityScheme
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The `components` object of a document: reusable objects that other parts of the document refer to by `$ref`.
 *
 * The element is immutable and safe to share between threads. Every map keeps the order of the document and is
 * empty when the document declares nothing of its kind. A reading reports a name that holds other characters than
 * letters, digits, `.`, `-` and `_`.
 *
 * @since 1.0.0
 */
public class Components internal constructor(builder: ComponentsBuilder) :
    Element(builder) {
    /**
     * The `schemas` by name; a schema that refers to another holds the reference itself.
     *
     * @since 1.0.0
     */
    public val schemas: Map<String, Schema> = builder.schemas.frozen()

    /**
     * The `responses` by name, inline or by reference.
     *
     * @since 1.0.0
     */
    public val responses: Map<String, Referable<Response>> = builder.responses.frozen()

    /**
     * The `parameters` by name, inline or by reference.
     *
     * @since 1.0.0
     */
    public val parameters: Map<String, Referable<Parameter>> = builder.parameters.frozen()

    /**
     * The `examples` by name, inline or by reference.
     *
     * @since 1.0.0
     */
    public val examples: Map<String, Referable<Example>> = builder.examples.frozen()

    /**
     * The `requestBodies` by name, inline or by reference.
     *
     * @since 1.0.0
     */
    public val requestBodies: Map<String, Referable<RequestBody>> = builder.requestBodies.frozen()

    /**
     * The `headers` by name, inline or by reference.
     *
     * @since 1.0.0
     */
    public val headers: Map<String, Referable<Header>> = builder.headers.frozen()

    /**
     * The `securitySchemes` by name, inline or by reference, which security requirements name.
     *
     * @since 1.0.0
     */
    public val securitySchemes: Map<String, Referable<SecurityScheme>> =
        builder.securitySchemes.frozen()

    /**
     * The `links` by name, inline or by reference.
     *
     * @since 1.0.0
     */
    public val links: Map<String, Referable<Link>> = builder.links.frozen()

    /**
     * The `callbacks` by name, inline or by reference.
     *
     * @since 1.0.0
     */
    public val callbacks: Map<String, Referable<Callback>> = builder.callbacks.frozen()

    /**
     * The `pathItems` by name; a path item that refers to another holds the reference itself. Empty before
     * OpenAPI 3.1.
     *
     * @since 1.0.0
     */
    public val pathItems: Map<String, PathItem> = builder.pathItems.frozen()

    /**
     * The `mediaTypes` by name, inline or by reference, which content refers to by `$ref`; empty before OpenAPI 3.2.

     *
     * @since 1.0.0
     */
    public val mediaTypes: Map<String, Referable<MediaType>> = builder.mediaTypes.frozen()

    override val identity: List<Any?>
        get() = listOf(
            schemas,
            responses,
            parameters,
            examples,
            requestBodies,
            headers,
            securitySchemes,
            links,
            callbacks,
            pathItems,
            mediaTypes,
        )
}
