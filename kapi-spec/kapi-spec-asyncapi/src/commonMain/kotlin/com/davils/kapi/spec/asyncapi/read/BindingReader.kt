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

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.diagnostic.shown
import com.davils.kapi.spec.asyncapi.model.binding.ChannelBindings
import com.davils.kapi.spec.asyncapi.model.binding.ChannelBindingsBuilder
import com.davils.kapi.spec.asyncapi.model.binding.MessageBindings
import com.davils.kapi.spec.asyncapi.model.binding.MessageBindingsBuilder
import com.davils.kapi.spec.asyncapi.model.binding.OperationBindings
import com.davils.kapi.spec.asyncapi.model.binding.OperationBindingsBuilder
import com.davils.kapi.spec.asyncapi.model.binding.ServerBindings
import com.davils.kapi.spec.asyncapi.model.binding.ServerBindingsBuilder
import com.davils.kapi.spec.asyncapi.model.binding.WebSocketChannelBinding
import com.davils.kapi.spec.asyncapi.model.binding.WebSocketChannelBindingBuilder
import com.davils.kapi.spec.asyncapi.model.binding.WebSocketMethod
import com.davils.kapi.spec.asyncapi.read.shape.Fields
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.asyncapi.read.shape.ShapeCheck
import com.davils.kapi.spec.asyncapi.read.shape.Shapes
import com.davils.kapi.spec.core.value.Value

internal object BindingReader {
    fun server(cursor: Cursor): ServerBindings = cursor.read(Shapes.bindings, ServerBindingsBuilder()) { fields ->
        hasWebSocket = isReserved(fields)
        protocols.putAll(protocolsOf(fields))
    }

    fun channel(cursor: Cursor): ChannelBindings = cursor.read(Shapes.bindings, ChannelBindingsBuilder()) { fields ->
        webSocket = fields.cursor(Keys.WEB_SOCKET)?.let(::webSocket)
        protocols.putAll(protocolsOf(fields))
    }

    fun operation(cursor: Cursor): OperationBindings = cursor.read(
        Shapes.bindings,
        OperationBindingsBuilder(),
    ) { fields ->
        hasWebSocket = isReserved(fields)
        protocols.putAll(protocolsOf(fields))
    }

    fun message(cursor: Cursor): MessageBindings = cursor.read(Shapes.bindings, MessageBindingsBuilder()) { fields ->
        hasWebSocket = isReserved(fields)
        protocols.putAll(protocolsOf(fields))
    }

    private fun webSocket(cursor: Cursor): WebSocketChannelBinding =
        cursor.read(Shapes.webSocketChannel, WebSocketChannelBindingBuilder()) { fields ->
            method = methodOf(fields)
            query = SchemaReader.slot(fields, Keys.QUERY)
            headers = SchemaReader.slot(fields, Keys.HEADERS)
            bindingVersion = fields.text(Keys.BINDING_VERSION)
        }

    private fun isReserved(fields: Fields): Boolean {
        val written = fields.cursor(Keys.WEB_SOCKET) ?: return false
        ShapeCheck.fields(written, Shapes.webSocketReserved)
        return true
    }

    private fun protocolsOf(fields: Fields): Map<String, Value> {
        val values = fields.values()
        return values.filterKeys { name -> name != Keys.WEB_SOCKET }
    }

    private fun methodOf(fields: Fields): WebSocketMethod? {
        val written = fields.text(Keys.METHOD) ?: return null
        val method = WebSocketMethod.entries.firstOrNull { candidate -> candidate.keyword == written }
        if (method == null) {
            val message = "The method of a WebSocket binding is GET or POST, not ${shown(written)}"
            fields.report(Keys.METHOD, DiagnosticCodes.BINDING_WEBSOCKET_METHOD, message)
        }
        return method
    }
}
