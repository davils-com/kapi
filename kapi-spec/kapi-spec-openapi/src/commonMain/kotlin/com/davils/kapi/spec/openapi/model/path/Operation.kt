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

package com.davils.kapi.spec.openapi.model.path

import com.davils.kapi.spec.core.collection.frozen
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.content.RequestBody
import com.davils.kapi.spec.openapi.model.content.Responses
import com.davils.kapi.spec.openapi.model.info.ExternalDocs
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.security.SecurityRequirement
import com.davils.kapi.spec.openapi.model.server.Server
import com.davils.kore.core.annotation.DavilsInternal

/**
 * An operation object: one method on one path, with its parameters, its request body and its responses.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class Operation internal constructor(builder: OperationBuilder) :
    Element(builder) {
    /**
     * The `operationId` of the operation, which identifies it among every operation of the document; `null` when the
     * document leaves it out.
     *
     * A reading reports an id that names more than one operation. The id is not checked to be a valid identifier.
     *
     * @since 1.0.0
     */
    public val operationId: String? = builder.operationId

    /**
     * The `summary` of what the operation does; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val summary: String? = builder.summary

    /**
     * The `description` of the operation, which may use CommonMark; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The names of the `tags` the operation is grouped under, in the order of the document; empty when it names none.
     *
     * A name need not be declared among the tags of the document.
     *
     * @since 1.0.0
     */
    public val tags: List<String> = builder.tags.frozen()

    /**
     * The `externalDocs` of the operation; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val externalDocs: ExternalDocs? = builder.externalDocs

    /**
     * The `parameters` of the operation, inline or by reference, which override those of the path item with the same
     * name and location; empty when it declares none.
     *
     * A reading reports a parameter declared twice, a path parameter the path does not name, a parameter of the path
     * that neither the operation nor the path item declares, and more than one parameter in `querystring` or one beside
     * parameters in `query`.
     *
     * @since 1.0.0
     */
    public val parameters: List<Referable<Parameter>> = builder.parameters.frozen()

    /**
     * The `requestBody` of the operation, inline or by reference; `null` when the operation takes none.
     *
     * @since 1.0.0
     */
    public val requestBody: Referable<RequestBody>? = builder.requestBody

    /**
     * The `responses` the operation may return; a reading reports an operation that declares none.
     *
     * @since 1.0.0
     */
    public val responses: Responses = builder.responses

    /**
     * The `callbacks` the operation may make, by name, inline or by reference; empty when it declares none.
     *
     * @since 1.0.0
     */
    public val callbacks: Map<String, Referable<Callback>> = builder.callbacks.frozen()

    /**
     * Whether the operation is `deprecated` and should no longer be used; `false` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val isDeprecated: Boolean = builder.isDeprecated

    /**
     * The `security` requirements of the operation, of which a request meets one; `null` when the document leaves it
     * out, so that the requirements of the document apply, and empty when the operation needs no security.
     *
     * @since 1.0.0
     */
    public val security: List<SecurityRequirement>? = builder.security?.frozen()

    /**
     * The `servers` that serve the operation instead of those of the path item and the document; empty when it names
     * none.
     *
     * @since 1.0.0
     */
    public val servers: List<Server> = builder.servers.frozen()

    override val identity: List<Any?>
        get() = listOf(
            operationId,
            summary,
            description,
            tags,
            externalDocs,
            parameters,
            requestBody,
            responses,
            callbacks,
            isDeprecated,
            security,
            servers,
        )
}
