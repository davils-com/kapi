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

package com.davils.kapi.spec.asyncapi.model.server

import com.davils.kapi.spec.asyncapi.model.binding.ServerBindings
import com.davils.kapi.spec.asyncapi.model.info.ExternalDocs
import com.davils.kapi.spec.asyncapi.model.info.Tag
import com.davils.kapi.spec.asyncapi.model.security.SecurityScheme
import com.davils.kapi.spec.core.collection.frozen
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kore.core.annotation.DavilsInternal

/**
 * A server a channel is available on: a message broker, a server or another kind of computer program.
 *
 * The [host] and the [pathname] may name variables in curly braces, such as `{region}.example.com`, which the server
 * declares in its [variables].
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class Server internal constructor(builder: ServerBuilder) : Element(builder) {
    /**
     * The host of the server, with an optional port, such as `rabbitmq.example.com:5672`.
     *
     * @since 1.0.0
     */
    public val host: String = builder.host

    /**
     * The protocol of the server, such as `ws`, `kafka` or `amqp`.
     *
     * @since 1.0.0
     */
    public val protocol: String = builder.protocol

    /**
     * The version of the protocol, such as `0.9.1`.
     *
     * @since 1.0.0
     */
    public val protocolVersion: String? = builder.protocolVersion

    /**
     * The path to a resource on the host, such as `/production`.
     *
     * @since 1.0.0
     */
    public val pathname: String? = builder.pathname

    /**
     * A description, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * A human-friendly title.
     *
     * @since 1.0.0
     */
    public val title: String? = builder.title

    /**
     * A short summary of what it is about.
     *
     * @since 1.0.0
     */
    public val summary: String? = builder.summary

    /**
     * The variables of the host and the path, by name.
     *
     * @since 1.0.0
     */
    public val variables: Map<String, Referable<ServerVariable>> = builder.variables.frozen()

    /**
     * The security schemes of the server, any one of which a client may use.
     *
     * @since 1.0.0
     */
    public val security: List<Referable<SecurityScheme>> = builder.security.frozen()

    /**
     * The tags, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val tags: List<Referable<Tag>> = builder.tags.frozen()

    /**
     * Further documentation, written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val externalDocs: Referable<ExternalDocs>? = builder.externalDocs

    /**
     * The protocol-specific details of the server.
     *
     * @since 1.0.0
     */
    public val bindings: Referable<ServerBindings>? = builder.bindings

    override val identity: List<Any?>
        get() = listOf(
            host,
            protocol,
            protocolVersion,
            pathname,
            description,
            title,
            summary,
            variables,
            security,
            tags,
            externalDocs,
            bindings,
        )
}
