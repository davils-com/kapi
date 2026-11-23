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
import com.davils.kapi.spec.asyncapi.model.message.CorrelationId
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.message.MessageExample
import com.davils.kapi.spec.asyncapi.model.message.MessageTrait
import com.davils.kapi.spec.asyncapi.read.shape.Keys

internal class MessageWriter(private val info: InfoWriter, private val bindings: BindingWriter) {
    fun message(message: Message): YamlNode {
        val out = Out()
        out.node(Keys.HEADERS, SchemaWriter.slot(message.headers))
        out.node(Keys.PAYLOAD, SchemaWriter.slot(message.payload))
        out.node(
            Keys.CORRELATION_ID,
            message.correlationId?.let { written -> ReferenceWriter.referable(written, ::correlationId) },
        )
        out.text(key = Keys.CONTENT_TYPE, value = message.contentType)
        out.text(key = Keys.NAME, value = message.name)
        out.text(key = Keys.TITLE, value = message.title)
        out.text(key = Keys.SUMMARY, value = message.summary)
        out.text(key = Keys.DESCRIPTION, value = message.description)
        info.described(out, message.tags, message.externalDocs)
        out.node(
            Keys.BINDINGS,
            message.bindings?.let { written -> ReferenceWriter.referable(written, bindings::message) },
        )
        out.list(Keys.EXAMPLES, message.examples, ::example)
        out.list(Keys.TRAITS, message.traits, ReferenceWriter.referables(::trait))
        return out.build(message)
    }

    fun trait(trait: MessageTrait): YamlNode {
        val out = Out()
        out.node(Keys.HEADERS, SchemaWriter.slot(trait.headers))
        out.node(
            Keys.CORRELATION_ID,
            trait.correlationId?.let { written -> ReferenceWriter.referable(written, ::correlationId) },
        )
        out.text(key = Keys.CONTENT_TYPE, value = trait.contentType)
        out.text(key = Keys.NAME, value = trait.name)
        out.text(key = Keys.TITLE, value = trait.title)
        out.text(key = Keys.SUMMARY, value = trait.summary)
        out.text(key = Keys.DESCRIPTION, value = trait.description)
        info.described(out, trait.tags, trait.externalDocs)
        out.node(
            Keys.BINDINGS,
            trait.bindings?.let { written -> ReferenceWriter.referable(written, bindings::message) },
        )
        out.list(Keys.EXAMPLES, trait.examples, ::example)
        return out.build(trait)
    }

    fun correlationId(correlationId: CorrelationId): YamlNode {
        val out = Out()
        out.text(key = Keys.DESCRIPTION, value = correlationId.description)
        out.text(key = Keys.LOCATION, value = correlationId.expression)
        return out.build(correlationId)
    }

    private fun example(example: MessageExample): YamlNode {
        val out = Out()
        out.map(Keys.HEADERS, example.headers, Nodes::of)
        out.node(Keys.PAYLOAD, example.payload?.let(Nodes::of))
        out.text(key = Keys.NAME, value = example.name)
        out.text(key = Keys.SUMMARY, value = example.summary)
        return out.build(example)
    }
}
