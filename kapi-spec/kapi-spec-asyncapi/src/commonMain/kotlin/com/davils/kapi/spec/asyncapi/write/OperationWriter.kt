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
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.asyncapi.model.operation.OperationReply
import com.davils.kapi.spec.asyncapi.model.operation.OperationTrait
import com.davils.kapi.spec.asyncapi.model.operation.ReplyAddress
import com.davils.kapi.spec.asyncapi.read.shape.Keys

internal class OperationWriter(
    private val info: InfoWriter,
    private val security: SecurityWriter,
    private val bindings: BindingWriter,
) {
    fun operation(operation: Operation): YamlNode {
        val out = Out()
        out.text(key = Keys.ACTION, value = operation.action.keyword)
        out.node(Keys.CHANNEL, operation.channel?.let(ReferenceWriter::reference))
        out.text(key = Keys.TITLE, value = operation.title)
        out.text(key = Keys.SUMMARY, value = operation.summary)
        out.text(key = Keys.DESCRIPTION, value = operation.description)
        out.list(Keys.SECURITY, operation.security, ReferenceWriter.referables(security::scheme))
        info.described(out, operation.tags, operation.externalDocs)
        out.node(
            Keys.BINDINGS,
            operation.bindings?.let { written -> ReferenceWriter.referable(written, bindings::operation) },
        )
        out.list(Keys.TRAITS, operation.traits, ReferenceWriter.referables(::trait))
        out.list(Keys.MESSAGES, operation.messages, ReferenceWriter::reference)
        out.node(Keys.REPLY, operation.reply?.let { written -> ReferenceWriter.referable(written, ::reply) })
        return out.build(operation)
    }

    fun trait(trait: OperationTrait): YamlNode {
        val out = Out()
        out.text(key = Keys.TITLE, value = trait.title)
        out.text(key = Keys.SUMMARY, value = trait.summary)
        out.text(key = Keys.DESCRIPTION, value = trait.description)
        out.list(Keys.SECURITY, trait.security, ReferenceWriter.referables(security::scheme))
        info.described(out, trait.tags, trait.externalDocs)
        out.node(
            Keys.BINDINGS,
            trait.bindings?.let { written -> ReferenceWriter.referable(written, bindings::operation) },
        )
        return out.build(trait)
    }

    fun reply(reply: OperationReply): YamlNode {
        val out = Out()
        out.node(Keys.ADDRESS, reply.address?.let { written -> ReferenceWriter.referable(written, ::replyAddress) })
        out.node(Keys.CHANNEL, reply.channel?.let(ReferenceWriter::reference))
        out.list(Keys.MESSAGES, reply.messages, ReferenceWriter::reference)
        return out.build(reply)
    }

    fun replyAddress(address: ReplyAddress): YamlNode {
        val out = Out()
        out.text(key = Keys.DESCRIPTION, value = address.description)
        out.text(key = Keys.LOCATION, value = address.expression)
        return out.build(address)
    }
}
