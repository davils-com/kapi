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
import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The builder of [Components]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ComponentsBuilder internal constructor() : ElementBuilder<Components>() {
    override val subject: String get() = "asyncapi components"

    internal val schemas: MutableMap<String, Referable<Schema>> = KeyedMap()

    internal val servers: MutableMap<String, Referable<Server>> = KeyedMap()

    internal val channels: MutableMap<String, Referable<Channel>> = KeyedMap()

    internal val operations: MutableMap<String, Referable<Operation>> = KeyedMap()

    internal val messages: MutableMap<String, Referable<Message>> = KeyedMap()

    internal val securitySchemes: MutableMap<String, Referable<SecurityScheme>> = KeyedMap()

    internal val serverVariables: MutableMap<String, Referable<ServerVariable>> = KeyedMap()

    internal val parameters: MutableMap<String, Referable<Parameter>> = KeyedMap()

    internal val correlationIds: MutableMap<String, Referable<CorrelationId>> = KeyedMap()

    internal val replies: MutableMap<String, Referable<OperationReply>> = KeyedMap()

    internal val replyAddresses: MutableMap<String, Referable<ReplyAddress>> = KeyedMap()

    internal val externalDocs: MutableMap<String, Referable<ExternalDocs>> = KeyedMap()

    internal val tags: MutableMap<String, Referable<Tag>> = KeyedMap()

    internal val operationTraits: MutableMap<String, Referable<OperationTrait>> = KeyedMap()

    internal val messageTraits: MutableMap<String, Referable<MessageTrait>> = KeyedMap()

    internal val serverBindings: MutableMap<String, Referable<ServerBindings>> = KeyedMap()

    internal val channelBindings: MutableMap<String, Referable<ChannelBindings>> = KeyedMap()

    internal val operationBindings: MutableMap<String, Referable<OperationBindings>> = KeyedMap()

    internal val messageBindings: MutableMap<String, Referable<MessageBindings>> = KeyedMap()

    override fun assemble(): Components = Components(this)
}
