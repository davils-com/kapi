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
import com.davils.kaml.core.node.yamlMapping
import com.davils.kapi.spec.asyncapi.model.binding.ChannelBindings
import com.davils.kapi.spec.asyncapi.model.binding.MessageBindings
import com.davils.kapi.spec.asyncapi.model.binding.OperationBindings
import com.davils.kapi.spec.asyncapi.model.binding.ServerBindings
import com.davils.kapi.spec.asyncapi.model.binding.WebSocketChannelBinding
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.value.Value

internal class BindingWriter {
    fun server(bindings: ServerBindings): YamlNode = reserved(bindings, bindings.hasWebSocket, bindings.protocols)

    fun operation(bindings: OperationBindings): YamlNode = reserved(bindings, bindings.hasWebSocket, bindings.protocols)

    fun message(bindings: MessageBindings): YamlNode = reserved(bindings, bindings.hasWebSocket, bindings.protocols)

    fun channel(bindings: ChannelBindings): YamlNode {
        val out = Out()
        out.node(Keys.WEB_SOCKET, bindings.webSocket?.let(::webSocket))
        bindings.protocols.forEach { (protocol, value) -> out.node(protocol, Nodes.of(value)) }
        return out.build(bindings)
    }

    private fun reserved(element: Element, hasWebSocket: Boolean, protocols: Map<String, Value>): YamlNode {
        val out = Out()
        out.node(Keys.WEB_SOCKET, yamlMapping(emptyList()).takeIf { hasWebSocket })
        protocols.forEach { (protocol, value) -> out.node(protocol, Nodes.of(value)) }
        return out.build(element)
    }

    private fun webSocket(binding: WebSocketChannelBinding): YamlNode {
        val out = Out()
        out.text(key = Keys.METHOD, value = binding.method?.keyword)
        out.node(Keys.QUERY, SchemaWriter.slot(binding.query))
        out.node(Keys.HEADERS, SchemaWriter.slot(binding.headers))
        out.text(key = Keys.BINDING_VERSION, value = binding.bindingVersion)
        return out.build(binding)
    }
}
