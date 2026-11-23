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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kapi.spec.asyncapi.model.component.Components
import com.davils.kapi.spec.asyncapi.model.component.ComponentsBuilder
import com.davils.kapi.spec.asyncapi.read.reference.ReferenceKind
import com.davils.kapi.spec.asyncapi.read.shape.Fields
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.asyncapi.read.shape.Shapes

internal object ComponentsReader {
    fun components(cursor: Cursor): Components = cursor.read(Shapes.components, ComponentsBuilder()) { fields ->
        schemas.putAll(fields.entries(Keys.SCHEMAS).referables(ReferenceKind.Schema, SchemaReader::schema))
        servers.putAll(fields.entries(Keys.SERVERS).referables(ReferenceKind.Server, ServerReader::server))
        channels.putAll(fields.entries(Keys.CHANNELS).referables(ReferenceKind.Channel, ChannelReader::channel))
        val operations = fields.entries(Keys.OPERATIONS)
        this.operations.putAll(operations.referables(ReferenceKind.Operation, OperationReader::operation))
        messages.putAll(fields.entries(Keys.MESSAGES).referables(ReferenceKind.Message, MessageReader::message))
        parts(this, fields)
        traits(this, fields)
        bindings(this, fields)
    }

    private fun parts(builder: ComponentsBuilder, fields: Fields) {
        val schemes = fields.entries(Keys.SECURITY_SCHEMES)
        builder.securitySchemes.putAll(schemes.referables(ReferenceKind.SecurityScheme, SecurityReader::scheme))
        val variables = fields.entries(Keys.SERVER_VARIABLES)
        builder.serverVariables.putAll(variables.referables(ReferenceKind.ServerVariable, ServerReader::variable))
        val parameters = fields.entries(Keys.PARAMETERS)
        builder.parameters.putAll(parameters.referables(ReferenceKind.Parameter, ChannelReader::parameter))
        val correlationIds = fields.entries(Keys.CORRELATION_IDS)
        builder.correlationIds.putAll(
            correlationIds.referables(ReferenceKind.CorrelationId, MessageReader::correlationId),
        )
        val replies = fields.entries(Keys.REPLIES)
        builder.replies.putAll(replies.referables(ReferenceKind.OperationReply, OperationReader::reply))
        val addresses = fields.entries(Keys.REPLY_ADDRESSES)
        builder.replyAddresses.putAll(addresses.referables(ReferenceKind.ReplyAddress, OperationReader::replyAddress))
        val docs = fields.entries(Keys.EXTERNAL_DOCS)
        builder.externalDocs.putAll(docs.referables(ReferenceKind.ExternalDocs, InfoReader::externalDocs))
        builder.tags.putAll(fields.entries(Keys.TAGS).referables(ReferenceKind.Tag, InfoReader::tag))
    }

    private fun traits(builder: ComponentsBuilder, fields: Fields) {
        val operationTraits = fields.entries(Keys.OPERATION_TRAITS)
        builder.operationTraits.putAll(operationTraits.referables(ReferenceKind.OperationTrait, OperationReader::trait))
        val messageTraits = fields.entries(Keys.MESSAGE_TRAITS)
        builder.messageTraits.putAll(messageTraits.referables(ReferenceKind.MessageTrait, MessageReader::trait))
    }

    private fun bindings(builder: ComponentsBuilder, fields: Fields) {
        val servers = fields.entries(Keys.SERVER_BINDINGS)
        builder.serverBindings.putAll(servers.referables(ReferenceKind.ServerBindings, BindingReader::server))
        val channels = fields.entries(Keys.CHANNEL_BINDINGS)
        builder.channelBindings.putAll(channels.referables(ReferenceKind.ChannelBindings, BindingReader::channel))
        val operations = fields.entries(Keys.OPERATION_BINDINGS)
        builder.operationBindings.putAll(
            operations.referables(ReferenceKind.OperationBindings, BindingReader::operation),
        )
        val messages = fields.entries(Keys.MESSAGE_BINDINGS)
        builder.messageBindings.putAll(messages.referables(ReferenceKind.MessageBindings, BindingReader::message))
    }
}
