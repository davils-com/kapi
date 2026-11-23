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

package com.davils.kapi.spec.openapi.model.server

import com.davils.kapi.spec.core.model.Element

/**
 * A `server` object: a server that offers the API, which a document, a path item, an operation or a link may name.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class Server internal constructor(builder: ServerBuilder) : Element(builder) {
    /**
     * The `url` of the server, which may be relative to the document and may name variables in braces, such as
     * `https://{region}.example.com/v1`; required by OpenAPI, so a reading reports a server that leaves it out and
     * keeps it empty here.
     *
     * The address is not checked when it is read; a client calls it only through its destination policy. A reading
     * reports a variable that [variables] does not declare.
     *
     * @since 1.0.0
     */
    public val url: String = builder.url

    /**
     * The `description` of the server, which may use CommonMark; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The `name` of the server, a short name such as `production`; `null` when the document leaves it out and before
     * OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val name: String? = builder.name

    /**
     * The `variables` that [url] names, by name, in the order of the document; empty when the document declares none.
     *
     * @since 1.0.0
     */
    public val variables: Map<String, ServerVariable> = builder.variables.toMap()

    override val identity: List<Any?> get() = listOf(url, description, name, variables)
}
