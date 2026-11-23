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

package com.davils.kapi.spec.openapi.model.content

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.server.Server

/**
 * A link object: an operation that a caller may call with values taken from a response, which OpenAPI calls a
 * design-time link.
 *
 * The element is immutable and safe to share between threads. A reading reports a link that names its operation by
 * both or neither of [operationReference] and [operationId], and one whose [operationId] names no operation of the
 * document.
 *
 * @since 1.0.0
 */
public class Link internal constructor(builder: LinkBuilder) : Element(builder) {
    /**
     * The `operationRef` of the link: a URI reference to the operation, as it was written and not resolved; `null` when
     * the link names its operation by [operationId].
     *
     * @since 1.0.0
     */
    public val operationReference: String? = builder.operationReference

    /**
     * The `operationId` of the operation the link leads to; `null` when the link names it by [operationReference].
     *
     * @since 1.0.0
     */
    public val operationId: String? = builder.operationId

    /**
     * The `parameters` to pass to the operation by name, each a constant or a runtime expression such as
     * `$response.body#/id`, kept as written and not evaluated; empty when the link passes none.
     *
     * @since 1.0.0
     */
    public val parameters: Map<String, Value> = builder.parameters.toMap()

    /**
     * The `requestBody` to send to the operation, a constant or a runtime expression kept as written; `null` when the
     * link sends none.
     *
     * @since 1.0.0
     */
    public val requestBody: Value? = builder.requestBody

    /**
     * The `description` of the link, which may use CommonMark; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The `server` the operation is called on instead of its own; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val server: Server? = builder.server

    override val identity: List<Any?>
        get() = listOf(
            operationReference,
            operationId,
            parameters,
            requestBody,
            description,
            server,
        )
}
