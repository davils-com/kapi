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
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.asyncapi.model.operation.OperationReply
import com.davils.kapi.spec.core.collection.groupByKeyed
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.ViolationCode

private const val CHANNELS: String = "channels"

private const val MESSAGES: String = "messages"

internal object OperationRules {
    private val operationFinding: Pair<ViolationCode, String> =
        DiagnosticCodes.OPERATION_MESSAGE_NOT_IN_CHANNEL to "The operation"

    private val replyFinding: Pair<ViolationCode, String> =
        DiagnosticCodes.REPLY_MESSAGE_NOT_IN_CHANNEL to "The reply"

    fun check(scope: RuleScope) {
        val root = scope.document.operations.values
            .filterIsInstance<Referable.Inline<Operation>>()
        root.forEach { operation -> operation.value.channel?.let { channel -> placement(scope, channel) } }
        scope.all<Operation>().forEach { operation ->
            val channel = operation.channel
            channel?.let { declared ->
                messages(
                    scope = scope,
                    channel = declared,
                    messages = operation.messages,
                    finding = operationFinding,
                )
            }
        }
        scope.all<OperationReply>().forEach { reply -> reply(scope, reply) }
        keys(scope)
    }

    private fun keys(scope: RuleScope) {
        val listed = scope.document.operations.entries.mapNotNull { (key, referable) ->
            scope.resolve(referable)?.location?.let { location -> Listing(key, referable, location) }
        }
        val groups = listed.groupByKeyed { listing -> "${listing.location}" }
        val repeated = groups.values.filter { same -> same.size > 1 }
        repeated.forEach { same ->
            val keys = same.joinToString { listing -> shown(listing.key) }
            same.drop(1).forEach { listing ->
                val message = "One operation is listed under the keys $keys of operations, but an operation has one id"
                scope.report(DiagnosticCodes.OPERATION_KEY_DUPLICATE, message, listing.referable)
            }
        }
    }

    private fun placement(scope: RuleScope, channel: Reference) {
        val target = channel.target
        val tokens = target.pointer.tokens
        val isRootChannel = tokens.size == 2 && tokens.first() == CHANNELS && tokens.last() in scope.document.channels
        if (target.document == scope.root && isRootChannel) return
        val message = "An operation of the document names ${shown(
            channel.text,
        )}, which is no channel of the root channels"
        scope.report(DiagnosticCodes.OPERATION_CHANNEL_PLACEMENT, message, channel.location)
    }

    private fun reply(scope: RuleScope, reply: OperationReply) {
        val channel = reply.channel ?: return
        messages(scope = scope, channel = channel, messages = reply.messages, finding = replyFinding)
        val resolved = scope.document.targets[channel.target] as? Channel
        val address = resolved?.address
        if (reply.address == null || address == null) return
        val message = "The reply has an address, so its channel must not have one, but it has ${shown(address)}"
        scope.report(DiagnosticCodes.REPLY_ADDRESS_CONFLICT, message, reply)
    }

    private fun messages(
        scope: RuleScope,
        channel: Reference,
        messages: List<Reference>,
        finding: Pair<ViolationCode, String>,
    ) {
        val resolved = scope.document.targets[channel.target] as? Channel ?: return
        val strays = messages.filterNot { message ->
            isOf(
                scope = scope,
                channel = channel,
                resolved = resolved,
                message = message,
            )
        }
        strays.forEach { stray ->
            val message = "${finding.second} names ${shown(stray.text)}, which is no message of its channel " +
                shown(channel.text)
            scope.report(finding.first, message, stray.location)
        }
    }

    private fun isOf(scope: RuleScope, channel: Reference, resolved: Channel, message: Reference): Boolean {
        val prefix = channel.target.pointer.child(MESSAGES)
        val pointers = resolved.messages.keys.map { key -> prefix.child(key) }
        val isPlaced = message.target.document == channel.target.document && message.target.pointer in pointers
        val target = scope.document.targets[message.target] as? Message
        val declared = resolved.messages.values.map { referable -> scope.resolve(referable) }
        val isDeclared = target != null && target in declared
        return isPlaced || isDeclared
    }

    private data class Listing(val key: String, val referable: Referable<Operation>, val location: Location)
}
