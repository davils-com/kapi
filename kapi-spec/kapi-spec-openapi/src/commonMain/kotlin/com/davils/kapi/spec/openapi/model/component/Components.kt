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

package com.davils.kapi.spec.openapi.model.component

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

public class Components internal constructor(builder: ComponentsBuilder) :
    Element(builder) {
    public val schemas: Map<String, Schema> = builder.schemas.toMap()

    public val responses: Map<String, Referable<Response>> = builder.responses.toMap()

    public val parameters: Map<String, Referable<Parameter>> = builder.parameters.toMap()

    public val examples: Map<String, Referable<Example>> = builder.examples.toMap()

    public val requestBodies: Map<String, Referable<RequestBody>> = builder.requestBodies.toMap()

    public val headers: Map<String, Referable<Header>> = builder.headers.toMap()

    public val securitySchemes: Map<String, Referable<SecurityScheme>> =
        builder.securitySchemes.toMap()

    public val links: Map<String, Referable<Link>> = builder.links.toMap()

    public val callbacks: Map<String, Referable<Callback>> = builder.callbacks.toMap()

    public val pathItems: Map<String, PathItem> = builder.pathItems.toMap()

    /**
     * The media types that content refers to by `$ref`, by name; empty before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val mediaTypes: Map<String, Referable<MediaType>> = builder.mediaTypes.toMap()

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
