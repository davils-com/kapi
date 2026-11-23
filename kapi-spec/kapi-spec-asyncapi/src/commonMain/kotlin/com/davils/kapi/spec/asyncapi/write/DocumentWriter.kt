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

package com.davils.kapi.spec.asyncapi.write

import com.davils.kaml.core.node.YamlNode
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.model.component.Components
import com.davils.kapi.spec.asyncapi.read.shape.Keys

internal class DocumentWriter {
    private val security: SecurityWriter = SecurityWriter()

    private val bindings: BindingWriter = BindingWriter()

    private val info: InfoWriter = InfoWriter(security, bindings)

    private val messages: MessageWriter = MessageWriter(info, bindings)

    private val channels: ChannelWriter = ChannelWriter(info, messages, bindings)

    private val operations: OperationWriter = OperationWriter(info, security, bindings)

    fun document(document: Document): YamlNode {
        val out = Out()
        out.text(key = Keys.ASYNCAPI, value = document.version.literal)
        out.text(key = Keys.ID, value = document.id)
        out.node(Keys.INFO, info.info(document.info))
        out.map(Keys.SERVERS, document.servers, ReferenceWriter.referables(info::server))
        out.text(key = Keys.DEFAULT_CONTENT_TYPE, value = document.defaultContentType)
        out.map(Keys.CHANNELS, document.channels, ReferenceWriter.referables(channels::channel))
        out.map(Keys.OPERATIONS, document.operations, ReferenceWriter.referables(operations::operation))
        out.node(Keys.COMPONENTS, document.components?.let(::components))
        return out.build(document)
    }

    private fun components(components: Components): YamlNode {
        val out = Out()
        out.map(Keys.SCHEMAS, components.schemas, ReferenceWriter.referables(SchemaWriter::schema))
        out.map(Keys.SERVERS, components.servers, ReferenceWriter.referables(info::server))
        out.map(Keys.CHANNELS, components.channels, ReferenceWriter.referables(channels::channel))
        out.map(Keys.OPERATIONS, components.operations, ReferenceWriter.referables(operations::operation))
        out.map(Keys.MESSAGES, components.messages, ReferenceWriter.referables(messages::message))
        out.map(Keys.SECURITY_SCHEMES, components.securitySchemes, ReferenceWriter.referables(security::scheme))
        out.map(Keys.SERVER_VARIABLES, components.serverVariables, ReferenceWriter.referables(info::variable))
        out.map(Keys.PARAMETERS, components.parameters, ReferenceWriter.referables(channels::parameter))
        out.map(Keys.CORRELATION_IDS, components.correlationIds, ReferenceWriter.referables(messages::correlationId))
        out.map(Keys.REPLIES, components.replies, ReferenceWriter.referables(operations::reply))
        out.map(Keys.REPLY_ADDRESSES, components.replyAddresses, ReferenceWriter.referables(operations::replyAddress))
        out.map(Keys.EXTERNAL_DOCS, components.externalDocs, ReferenceWriter.referables(info::externalDocs))
        out.map(Keys.TAGS, components.tags, ReferenceWriter.referables(info::tag))
        out.map(Keys.OPERATION_TRAITS, components.operationTraits, ReferenceWriter.referables(operations::trait))
        out.map(Keys.MESSAGE_TRAITS, components.messageTraits, ReferenceWriter.referables(messages::trait))
        out.map(Keys.SERVER_BINDINGS, components.serverBindings, ReferenceWriter.referables(bindings::server))
        out.map(Keys.CHANNEL_BINDINGS, components.channelBindings, ReferenceWriter.referables(bindings::channel))
        out.map(Keys.OPERATION_BINDINGS, components.operationBindings, ReferenceWriter.referables(bindings::operation))
        out.map(Keys.MESSAGE_BINDINGS, components.messageBindings, ReferenceWriter.referables(bindings::message))
        return out.build(components)
    }
}
