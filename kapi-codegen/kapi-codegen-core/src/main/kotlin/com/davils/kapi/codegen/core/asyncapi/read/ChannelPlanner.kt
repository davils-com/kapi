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

package com.davils.kapi.codegen.core.asyncapi.read

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.codegen.core.asyncapi.GenerationCodes
import com.davils.kapi.codegen.core.asyncapi.plan.ChannelPlan
import com.davils.kapi.codegen.core.asyncapi.plan.DirectionPlan
import com.davils.kapi.codegen.core.asyncapi.plan.MessagePlan
import com.davils.kapi.codegen.core.asyncapi.plan.OperationFlow
import com.davils.kapi.codegen.core.asyncapi.plan.PathPart
import com.davils.kapi.codegen.core.asyncapi.plan.RequestPlan
import com.davils.kapi.codegen.core.asyncapi.plan.Side
import com.davils.kapi.codegen.core.naming.Identifiers
import com.davils.kapi.codegen.core.pipeline.Findings
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.effective
import com.davils.kapi.spec.asyncapi.model.binding.WebSocketChannelBinding
import com.davils.kapi.spec.asyncapi.model.binding.WebSocketMethod
import com.davils.kapi.spec.asyncapi.model.channel.Channel
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.asyncapi.model.operation.OperationAction
import com.davils.kapi.spec.asyncapi.model.operation.OperationReply
import com.davils.kapi.spec.asyncapi.model.server.Server
import com.davils.kapi.spec.asyncapi.resolve
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.core.model.Target

private const val MESSAGES: String = "messages"

private const val REQUEST: String = "request"

private val webSockets: Set<String> = setOf("ws", "wss")

internal class ChannelPlanner(
    private val document: Document,
    private val report: Findings,
    private val prefix: String,
) {
    private val targets: Targets = Targets(document)

    private val messages: MessagePlanner = MessagePlanner(document, report)

    private val handshakes: HandshakePlanner = HandshakePlanner(document, report)

    private val security: SecurityPlanner = SecurityPlanner(document, report)

    private val discriminators: Discriminators = Discriminators(document, report)

    fun plan(): List<ChannelPlan> {
        val operations = document.operations.mapNotNull { (key, referable) ->
            document.resolve(referable)?.let { operation -> key to document.effective(operation) }
        }
        operations.forEach { (key, operation) -> auditChannel(key, operation) }
        val plans = document.channels.mapNotNull { (key, referable) -> channel(key, referable, operations) }
        ChannelNames(report).audit(plans)
        return plans
    }

    private fun auditChannel(key: String, operation: Operation) {
        val target = operation.channel?.target
        val isKnown = document.channels.keys.any { channel -> isChannel(target, channel) }
        if (!isKnown) {
            val text = "The operation ${shown(key)} names no channel of the document"
            report.error(GenerationCodes.OPERATION_CHANNEL, text, operation.location)
        }
    }

    private fun isChannel(target: Target?, key: String): Boolean {
        val referenced = (document.channels[key] as? Referable.Referenced)?.run { reference.target }
        return target != null && (target == targets.channel(key) || target == referenced)
    }

    private fun channel(
        key: String,
        referable: Referable<Channel>,
        operations: List<Pair<String, Operation>>,
    ): ChannelPlan? {
        val channel = document.resolve(referable) ?: return null
        val own = operations.filter { (_, operation) -> isChannel(operation.channel?.target, key) }
        val isServed = served(key, channel, own)
        return channel.takeIf { isServed }?.let { found -> planned(key, found, own) }
    }

    private fun served(key: String, channel: Channel, own: List<Pair<String, Operation>>): Boolean {
        val isWebSocket = isWebSocket(channel)
        val isAddressed = channel.address != null
        val isUsed = own.isNotEmpty()
        if (!isWebSocket) {
            val text = "The channel ${shown(key)} is served over no WebSocket and is skipped"
            report.warn(GenerationCodes.PROTOCOL_UNSUPPORTED, text, channel.location)
        }
        if (isWebSocket && !isAddressed) {
            val text = "The channel ${shown(key)} has no address a client could open and is skipped"
            report.warn(GenerationCodes.CHANNEL_SKIPPED, text, channel.location)
        }
        if (isWebSocket && isAddressed && !isUsed) {
            val text = "No operation names the channel ${shown(key)}, so neither side knows who sends what; it is " +
                "skipped"
            report.warn(GenerationCodes.CHANNEL_SKIPPED, text, channel.location)
        }
        return isWebSocket && isAddressed && isUsed
    }

    private fun serversOf(channel: Channel): List<Server> {
        val named = channel.servers.mapNotNull { reference -> document.resolve(reference) as? Server }
        return named.ifEmpty { document.servers.values.mapNotNull { referable -> document.resolve(referable) } }
    }

    private fun webServers(channel: Channel): List<Server> =
        serversOf(channel).filter { server -> server.protocol.lowercase() in webSockets }

    private fun isWebSocket(channel: Channel): Boolean {
        val protocols = serversOf(channel).map { server -> server.protocol.lowercase() }
        val isBound = bindingOf(channel) != null
        val isServed = protocols.any { protocol -> protocol in webSockets }
        return isServed || (protocols.isEmpty() && isBound)
    }

    private fun bindingOf(channel: Channel): WebSocketChannelBinding? {
        val bindings = channel.bindings?.let { referable -> document.resolve(referable) }
        return bindings?.webSocket
    }

    private fun planned(key: String, channel: Channel, own: List<Pair<String, Operation>>): ChannelPlan {
        val binding = bindingOf(channel)
        if (binding?.method == WebSocketMethod.Post) {
            val text = "The ws binding of the channel ${shown(key)} opens it with POST, but a WebSocket handshake " +
                "is a GET"
            report.error(GenerationCodes.BINDING_METHOD, text, binding.location)
        }
        val handshake = handshakes.plan(key, binding)
        val flows = own.map { (name, operation) ->
            flowOf(channelKey = key, channel = channel, name = name, operation = operation)
        }
        val sentByClient = keysOf(flows = flows, sending = OperationAction.Receive, replying = OperationAction.Send)
        val sentByServer = keysOf(flows = flows, sending = OperationAction.Send, replying = OperationAction.Receive)
        val used = sentByClient + sentByServer
        val planned = channel.messages.filterKeys { name -> name in used }.mapNotNull { (name, referable) ->
            messages.plan(name, referable)
        }
        val upstream = direction(
            planned = planned,
            keys = sentByClient,
            flows = flows,
            action = OperationAction.Receive,
        )
        val downstream = direction(
            planned = planned,
            keys = sentByServer,
            flows = flows,
            action = OperationAction.Send,
        )
        val operations = own.map { (_, operation) -> operation }
        val demanded = security.plan(
            channel = key,
            location = channel.location,
            operations = operations,
            servers = webServers(channel),
        )
        return ChannelPlan(
            key = key,
            base = prefix + Identifiers.type(key),
            path = pathOf(key, channel),
            handshake = handshake.fields,
            subprotocols = handshake.subprotocols,
            upstream = discriminators.distinguished(key, channel.location, upstream),
            downstream = discriminators.distinguished(key, channel.location, downstream),
            requests = flows.mapNotNull { flow -> requestOf(flow, planned) },
            security = demanded,
            summary = channel.summary ?: channel.title ?: channel.description,
            location = channel.location,
        )
    }

    private fun keysOf(flows: List<OperationFlow>, sending: OperationAction, replying: OperationAction): Set<String> {
        val sent = flows.filter { flow -> flow.action == sending }.flatMap(OperationFlow::messages)
        val replied = flows.filter { flow -> flow.action == replying }.flatMap { flow -> flow.replies.orEmpty() }
        return (sent + replied).toSet()
    }

    private fun direction(
        planned: List<MessagePlan>,
        keys: Set<String>,
        flows: List<OperationFlow>,
        action: OperationAction,
    ): DirectionPlan {
        val operations = flows.filter { flow -> flow.action == action }.map(OperationFlow::operation)
        return DirectionPlan(planned.filter { message -> message.key in keys }, operations)
    }

    private fun flowOf(channelKey: String, channel: Channel, name: String, operation: Operation): OperationFlow {
        val reply = operation.reply?.let { referable -> document.resolve(referable) }
        val replies = reply?.let { found ->
            repliesOf(channelKey = channelKey, channel = channel, name = name, reply = found)
        }
        return OperationFlow(
            operation = name,
            action = operation.action,
            messages = keysOf(channelKey, channel, operation.messages),
            replies = replies,
        )
    }

    private fun repliesOf(channelKey: String, channel: Channel, name: String, reply: OperationReply): List<String>? {
        val replyChannel = reply.channel
        val isSameChannel = replyChannel == null || isChannel(replyChannel.target, channelKey)
        val isValid = isSameChannel && reply.address == null
        if (!isValid) {
            val text = "The reply of the operation ${shown(name)} goes to another channel or a dynamic address, " +
                "which one WebSocket cannot reach"
            report.error(GenerationCodes.REPLY_CHANNEL, text, reply.location)
        }
        return keysOf(channelKey, channel, reply.messages).takeIf { isValid }
    }

    private fun keysOf(channelKey: String, channel: Channel, references: List<Reference>): List<String> {
        if (references.isEmpty()) return channel.messages.keys.toList()
        return references.mapNotNull { reference -> keyOf(channelKey, channel, reference) }
    }

    private fun keyOf(channelKey: String, channel: Channel, reference: Reference): String? {
        val tokens = reference.target.pointer.tokens
        val key = tokens.lastOrNull()
        val parent = Target(reference.target.document, JsonPointer.of(tokens.dropLast(2)))
        val isMessages = tokens.size >= 2 && tokens[tokens.size - 2] == MESSAGES
        val isOwn = isMessages && isChannel(parent, channelKey) && key in channel.messages
        if (!isOwn) {
            val text = "${shown(reference.text)} is none of the messages of the channel ${shown(channelKey)}"
            report.error(GenerationCodes.MESSAGE_FOREIGN, text, reference.location)
        }
        return key.takeIf { isOwn }
    }

    private fun requestOf(flow: OperationFlow, planned: List<MessagePlan>): RequestPlan? {
        val replies = flow.replies ?: return null
        val involved = planned.filter { message -> message.key in flow.messages || message.key in replies }
        val isCorrelated = involved.all { message -> message.correlation != null }
        val isPaired = flow.messages.isNotEmpty() && replies.isNotEmpty() && isCorrelated
        if (!isPaired) {
            val text = "The messages of the operation ${shown(flow.operation)} and its reply need correlation ids to " +
                "pair a reply with its request; the request is sent as any other message"
            report.warn(GenerationCodes.REPLY_UNPAIRED, text, involved.firstOrNull()?.location)
        }
        val function = REQUEST + Identifiers.type(flow.operation)
        val request = RequestPlan(
            operation = flow.operation,
            function = function,
            messages = flow.messages,
            replies = replies,
            sender = Side.Client.takeIf { flow.action == OperationAction.Receive } ?: Side.Server,
        )
        return request.takeIf { isPaired }
    }

    private fun pathOf(key: String, channel: Channel): List<PathPart> {
        val address = channel.address.orEmpty()
        val parts = Addresses.parts(address) { name -> parameterOf(channel, name) }
        if (parts == null) {
            val text = "The address ${shown(address)} of the channel ${shown(key)} is no path template of letters, " +
                "digits and {parameters}"
            report.error(GenerationCodes.CHANNEL_ADDRESS, text, channel.location)
        }
        return parts.orEmpty()
    }

    private fun parameterOf(channel: Channel, name: String): PathPart.Parameter {
        val parameter = channel.parameters[name]?.let { referable -> document.resolve(referable) }
        return PathPart.Parameter(
            name = name,
            member = Names.member(name),
            default = parameter?.default,
            choices = parameter?.enumeration.orEmpty(),
        )
    }
}
