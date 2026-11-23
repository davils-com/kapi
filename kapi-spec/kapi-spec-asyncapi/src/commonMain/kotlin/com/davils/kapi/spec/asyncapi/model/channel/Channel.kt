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

package com.davils.kapi.spec.asyncapi.model.channel

import com.davils.kapi.spec.asyncapi.model.binding.ChannelBindings
import com.davils.kapi.spec.asyncapi.model.info.ExternalDocs
import com.davils.kapi.spec.asyncapi.model.info.Tag
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference

/**
 * A channel of an application: an address messages flow through, and the messages that may flow through it.
 *
 * The [address] may name parameters in curly braces, such as `users.{userId}`, which the channel declares in its
 * [parameters]. The messages a channel declares are referenced by the operations on it.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class Channel internal constructor(builder: ChannelBuilder) : Element(builder) {
    /**
     * The address of the channel, such as a topic, a routing key or a path; `null` when it is dynamic or unknown.
     *
     * @since 1.0.0
     */
    public val address: String? = builder.address

    /**
     * The messages that may be sent through the channel, by their id.
     *
     * @since 1.0.0
     */
    public val messages: Map<String, Referable<Message>> = builder.messages.toMap()

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
     * A description, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The references to the servers the channel is available on; empty when it is available on every server.
     *
     * @since 1.0.0
     */
    public val servers: List<Reference> = builder.servers.toList()

    /**
     * The parameters of the address, by name.
     *
     * @since 1.0.0
     */
    public val parameters: Map<String, Referable<Parameter>> = builder.parameters.toMap()

    /**
     * The tags, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val tags: List<Referable<Tag>> = builder.tags.toList()

    /**
     * Further documentation, written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val externalDocs: Referable<ExternalDocs>? = builder.externalDocs

    /**
     * The protocol-specific details of the channel.
     *
     * @since 1.0.0
     */
    public val bindings: Referable<ChannelBindings>? = builder.bindings

    override val identity: List<Any?>
        get() = listOf(
            address,
            messages,
            title,
            summary,
            description,
            servers,
            parameters,
            tags,
            externalDocs,
            bindings,
        )
}
