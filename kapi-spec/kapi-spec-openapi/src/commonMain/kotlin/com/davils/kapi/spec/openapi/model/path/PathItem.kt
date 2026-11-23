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

package com.davils.kapi.spec.openapi.model.path

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.server.Server

/**
 * A path item object: the operations available on one path, in `paths`, in `webhooks`, in a callback or, from
 * OpenAPI 3.1 on, among the components.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class PathItem internal constructor(builder: PathItemBuilder) :
    Element(builder) {
    /**
     * The `$ref` of the path item, which refers to a path item defined elsewhere; `null` when the document writes none.
     *
     * OpenAPI leaves open what fields written both beside the reference and in its target mean.
     *
     * @since 1.0.0
     */
    public val reference: Reference? = builder.reference

    /**
     * The `summary` of the path item, which applies to every operation on it; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val summary: String? = builder.summary

    /**
     * The `description` of the path item, which may use CommonMark; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The operations declared by the fields of their methods, such as `get`, in the order of [Method]; empty when the
     * path item declares none. [Method.Query] appears only from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public val operations: Map<Method, Operation> = builder.operations.toMap()

    /**
     * The `additionalOperations`: the operations of methods without a field of their own, such as `COPY`, keyed by
     * the name of the method as it is sent in a request; empty when the path item declares none and before
     * OpenAPI 3.2.
     *
     * A key is not checked when the path item is built; a reading reports a key that is no method token or that
     * names a method with a field of its own, such as `GET`.
     *
     * @since 1.0.0
     */
    public val additionalOperations: Map<String, Operation> = builder.additionalOperations.toMap()

    /**
     * The `servers` that serve every operation of the path item instead of those of the document; empty when the path
     * item names none.
     *
     * @since 1.0.0
     */
    public val servers: List<Server> = builder.servers.toList()

    /**
     * The `parameters` that every operation of the path item shares, inline or by reference; empty when the path item
     * declares none.
     *
     * An operation overrides a parameter of the same name and location. A reading reports a parameter declared twice
     * with the same name and location.
     *
     * @since 1.0.0
     */
    public val parameters: List<Referable<Parameter>> = builder.parameters.toList()

    override val identity: List<Any?>
        get() = listOf(reference, summary, description, operations, additionalOperations, servers, parameters)
}
