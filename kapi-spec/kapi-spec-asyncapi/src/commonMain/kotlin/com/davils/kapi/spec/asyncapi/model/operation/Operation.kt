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

package com.davils.kapi.spec.asyncapi.model.operation

import com.davils.kapi.spec.asyncapi.model.binding.OperationBindings
import com.davils.kapi.spec.asyncapi.model.info.ExternalDocs
import com.davils.kapi.spec.asyncapi.model.info.Tag
import com.davils.kapi.spec.asyncapi.model.security.SecurityScheme
import com.davils.kapi.spec.core.collection.frozen
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kore.core.annotation.DavilsInternal

/**
 * An operation of an application: whether it sends or receives messages, on which channel, and which messages.
 *
 * [traits] holds the traits as they were declared; `Document.effective` applies them.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class Operation internal constructor(builder: OperationBuilder) : Element(builder) {
    /**
     * Whether the application sends or receives the messages.
     *
     * @since 1.0.0
     */
    public val action: OperationAction = builder.action

    /**
     * The reference to the channel of the operation; `null` when the document misses it.
     *
     * @since 1.0.0
     */
    public val channel: Reference? = builder.channel

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
     * The security schemes of the operation, any one of which a client may use.
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
     * The protocol-specific details of the operation.
     *
     * @since 1.0.0
     */
    public val bindings: Referable<OperationBindings>? = builder.bindings

    /**
     * The traits of the operation, in the order they were declared.
     *
     * @since 1.0.0
     */
    public val traits: List<Referable<OperationTrait>> = builder.traits.frozen()

    /**
     * The references to the messages of the channel the operation sends or receives; empty for all of them.
     *
     * @since 1.0.0
     */
    public val messages: List<Reference> = builder.messages.frozen()

    /**
     * The reply to the operation, for request and reply.
     *
     * @since 1.0.0
     */
    public val reply: Referable<OperationReply>? = builder.reply

    override val identity: List<Any?>
        get() = listOf(
            action,
            channel,
            title,
            summary,
            description,
            security,
            tags,
            externalDocs,
            bindings,
            traits,
            messages,
            reply,
        )
}
