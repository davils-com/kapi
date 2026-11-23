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

package com.davils.kapi.spec.asyncapi.model.binding

import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable

/**
 * The WebSocket binding of a channel: how the connection is opened and what the client sends with it.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class WebSocketChannelBinding internal constructor(builder: WebSocketChannelBindingBuilder) : Element(builder) {
    /**
     * The HTTP method of the request that opens the connection.
     *
     * @since 1.0.0
     */
    public val method: WebSocketMethod? = builder.method

    /**
     * The schema of the query parameters, which describes an object.
     *
     * @since 1.0.0
     */
    public val query: Referable<Schema>? = builder.query

    /**
     * The schema of the headers, which describes an object.
     *
     * @since 1.0.0
     */
    public val headers: Referable<Schema>? = builder.headers

    /**
     * The version of the binding, such as `0.1.0`.
     *
     * @since 1.0.0
     */
    public val bindingVersion: String? = builder.bindingVersion

    override val identity: List<Any?> get() = listOf(method, query, headers, bindingVersion)
}
