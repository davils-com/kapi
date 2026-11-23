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

package com.davils.kapi.spec.asyncapi.compatibility

import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.effective
import com.davils.kapi.spec.asyncapi.model.channel.Channel
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.asyncapi.model.operation.OperationAction
import com.davils.kapi.spec.asyncapi.resolve
import com.davils.kapi.spec.core.collection.associateKeyed
import com.davils.kapi.spec.core.compatibility.Direction
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Reference
import com.davils.kore.core.annotation.DavilsInternal

internal class OperationComparison(private val context: Context) {
    private val entries: DocumentComparison.Entries = DocumentComparison.Entries(context)

    fun compare() {
        val operations = Both(before = context.baseline.operations, after = context.candidate.operations)
        entries.compare(entries = operations, kind = DocumentComparison.Kind.Operations, common = ::operation)
    }

    private fun operation(key: String, operations: Both<Operation>) {
        val before = context.baseline.effective(operations.before)
        val after = context.candidate.effective(operations.after)
        val owners = Both<Element>(before = operations.before, after = operations.after)
        val label = "The operation ${shown(key)}"
        if (before.action != after.action) {
            report(rule = "operation-action-changed", message = "$label changes its action", owners = owners)
        }
        val channelBefore = addressOf(document = context.baseline, channel = before.channel)
        val channelAfter = addressOf(document = context.candidate, channel = after.channel)
        if (channelBefore != channelAfter) {
            report(rule = "operation-channel-changed", message = "$label moves to another channel", owners = owners)
        }
        val bindings = context.resolved(before = before.bindings, after = after.bindings)
        if (bindings.before != bindings.after) {
            report(
                rule = "operation-bindings-changed",
                message = "The bindings of $label change",
                owners = owners,
            )
        }
        val requirements = Both(before = before.security, after = after.security)
        context.security(label = label, requirements = requirements, owners = owners)
        messages(label = label, operations = Both(before = before, after = after))
    }

    private fun messages(label: String, operations: Both<Operation>) {
        val direction = when (operations.after.action) {
            OperationAction.Send -> Direction.Response
            OperationAction.Receive -> Direction.Request
        }
        val old = operations.before.messages.associateKeyed { reference -> keyOf(reference) to reference }
        val new = operations.after.messages.associateKeyed { reference -> keyOf(reference) to reference }
        old.forEach { (key, reference) ->
            val before = context.baseline.resolve(reference) as? Message
            val found = new[key]
            val after = found?.let { candidate -> context.candidate.resolve(candidate) as? Message }
            if (found == null) {
                context.report(
                    rule = "message-removed",
                    impacts = direction.narrowed,
                    message = "$label no longer carries the message ${shown(key)}",
                    before = before,
                    after = operations.after,
                )
            }
            if (before != null && after != null) {
                message(
                    label = "The message ${shown(key)}",
                    messages = Both(before = before, after = after),
                    direction = direction,
                )
            }
        }
        new.forEach { (key, reference) ->
            if (key in old) return@forEach
            context.report(
                rule = "message-added",
                impacts = direction.widened,
                message = "$label now also carries the message ${shown(key)}",
                before = operations.before,
                after = context.candidate.resolve(reference),
            )
        }
    }

    private fun message(label: String, messages: Both<Message>, direction: Direction) {
        val before = context.baseline.effective(messages.before)
        val after = context.candidate.effective(messages.after)
        val owners = Both<Element>(before = messages.before, after = messages.after)
        val typeBefore = before.contentType ?: context.baseline.defaultContentType
        val typeAfter = after.contentType ?: context.candidate.defaultContentType
        if (typeBefore != typeAfter) {
            report(rule = "message-content-type-changed", message = "$label changes its content type", owners = owners)
        }
        val bindings = context.resolved(before = before.bindings, after = after.bindings)
        if (bindings.before != bindings.after) {
            report(
                rule = "message-bindings-changed",
                message = "The bindings of $label change",
                owners = owners,
            )
        }
        context.schemas(before = before.payload, after = after.payload, direction = direction)
        context.schemas(before = before.headers, after = after.headers, direction = direction)
    }

    private fun addressOf(document: Document, channel: Reference?): String? {
        val resolved = channel?.let { reference -> document.resolve(reference) as? Channel }
        return resolved?.address
    }

    private fun keyOf(reference: Reference): String {
        val tokens = reference.target.pointer.tokens
        return tokens.takeLast(1).joinToString()
    }

    private fun report(rule: String, message: String, owners: Both<Element>) {
        context.report(
            rule = rule,
            impacts = bothBreaking,
            message = message,
            before = owners.before,
            after = owners.after,
        )
    }
}
