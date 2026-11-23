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

package com.davils.kapi.spec.asyncapi.model.component

import com.davils.kapi.spec.asyncapi.model.binding.ChannelBindings
import com.davils.kapi.spec.asyncapi.model.binding.MessageBindings
import com.davils.kapi.spec.asyncapi.model.binding.OperationBindings
import com.davils.kapi.spec.asyncapi.model.binding.ServerBindings
import com.davils.kapi.spec.asyncapi.model.channel.Channel
import com.davils.kapi.spec.asyncapi.model.channel.Parameter
import com.davils.kapi.spec.asyncapi.model.info.ExternalDocs
import com.davils.kapi.spec.asyncapi.model.info.Tag
import com.davils.kapi.spec.asyncapi.model.message.CorrelationId
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.message.MessageTrait
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.asyncapi.model.operation.OperationReply
import com.davils.kapi.spec.asyncapi.model.operation.OperationTrait
import com.davils.kapi.spec.asyncapi.model.operation.ReplyAddress
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.model.security.SecurityScheme
import com.davils.kapi.spec.asyncapi.model.server.Server
import com.davils.kapi.spec.asyncapi.model.server.ServerVariable
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable

/**
 * The reusable objects of a document; nothing in it applies to the document until it is referenced.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class Components internal constructor(builder: ComponentsBuilder) : Element(builder) {
    /**
     * The reusable schemas of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val schemas: Map<String, Referable<Schema>> = builder.schemas.toMap()

    /**
     * The reusable servers of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val servers: Map<String, Referable<Server>> = builder.servers.toMap()

    /**
     * The reusable channels of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val channels: Map<String, Referable<Channel>> = builder.channels.toMap()

    /**
     * The reusable operations of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val operations: Map<String, Referable<Operation>> = builder.operations.toMap()

    /**
     * The reusable messages of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val messages: Map<String, Referable<Message>> = builder.messages.toMap()

    /**
     * The reusable security schemes of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val securitySchemes: Map<String, Referable<SecurityScheme>> = builder.securitySchemes.toMap()

    /**
     * The reusable server variables of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val serverVariables: Map<String, Referable<ServerVariable>> = builder.serverVariables.toMap()

    /**
     * The reusable parameters of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val parameters: Map<String, Referable<Parameter>> = builder.parameters.toMap()

    /**
     * The reusable correlation ids of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val correlationIds: Map<String, Referable<CorrelationId>> = builder.correlationIds.toMap()

    /**
     * The reusable replies of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val replies: Map<String, Referable<OperationReply>> = builder.replies.toMap()

    /**
     * The reusable reply addresses of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val replyAddresses: Map<String, Referable<ReplyAddress>> = builder.replyAddresses.toMap()

    /**
     * The reusable external documentation of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val externalDocs: Map<String, Referable<ExternalDocs>> = builder.externalDocs.toMap()

    /**
     * The reusable tags of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val tags: Map<String, Referable<Tag>> = builder.tags.toMap()

    /**
     * The reusable operation traits of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val operationTraits: Map<String, Referable<OperationTrait>> = builder.operationTraits.toMap()

    /**
     * The reusable message traits of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val messageTraits: Map<String, Referable<MessageTrait>> = builder.messageTraits.toMap()

    /**
     * The reusable server bindings of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val serverBindings: Map<String, Referable<ServerBindings>> = builder.serverBindings.toMap()

    /**
     * The reusable channel bindings of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val channelBindings: Map<String, Referable<ChannelBindings>> = builder.channelBindings.toMap()

    /**
     * The reusable operation bindings of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val operationBindings: Map<String, Referable<OperationBindings>> = builder.operationBindings.toMap()

    /**
     * The reusable message bindings of the document, by their key, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val messageBindings: Map<String, Referable<MessageBindings>> = builder.messageBindings.toMap()

    override val identity: List<Any?>
        get() = listOf(
            schemas,
            servers,
            channels,
            operations,
            messages,
            securitySchemes,
            serverVariables,
            parameters,
            correlationIds,
            replies,
            replyAddresses,
            externalDocs,
            tags,
            operationTraits,
            messageTraits,
            serverBindings,
            channelBindings,
            operationBindings,
            messageBindings,
        )
}
