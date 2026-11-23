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

package com.davils.kapi.spec.asyncapi.validation

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.diagnostic.shown
import com.davils.kapi.spec.asyncapi.model.channel.Channel
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet

private const val SERVERS: String = "servers"

internal object ChannelRules {
    fun check(scope: RuleScope) {
        scope.all<Channel>().forEach { channel -> parameters(scope, channel) }
        val root = scope.document.channels.values
            .filterIsInstance<Referable.Inline<Channel>>()
        root.forEach { channel -> channel.value.servers.forEach { server -> placement(scope, server) } }
    }

    private fun parameters(scope: RuleScope, channel: Channel) {
        val address = channel.address
        val named = address?.let(Templates::names)
        if (address != null && named == null) {
            val message = "The address ${shown(address)} is no valid template, every { needs a name and a closing }"
            scope.report(DiagnosticCodes.CHANNEL_ADDRESS_TEMPLATE, message, channel)
        }
        val names = OrderedSet(named.orEmpty())
        names.filterNot { name -> name in channel.parameters }.forEach { name ->
            val message = "The address of the channel names the parameter ${shown(
                name,
            )}, which the channel does not declare"
            scope.report(DiagnosticCodes.CHANNEL_PARAMETER_UNDECLARED, message, channel)
        }
        val unused = channel.parameters.entries.filter { (name, _) -> name !in names }
        unused.forEach { (name, parameter) ->
            val message = "The channel declares the parameter ${shown(name)}, which its address does not name"
            scope.report(DiagnosticCodes.CHANNEL_PARAMETER_UNUSED, message, parameter)
        }
    }

    private fun placement(scope: RuleScope, server: Reference) {
        val target = server.target
        val tokens = target.pointer.tokens
        val isRootServer = tokens.size == 2 && tokens.first() == SERVERS && tokens.last() in scope.document.servers
        if (target.document == scope.root && isRootServer) return
        val message = "A channel of the document names ${shown(server.text)}, which is no server of the root servers"
        scope.report(DiagnosticCodes.CHANNEL_SERVER_PLACEMENT, message, server.location)
    }
}
