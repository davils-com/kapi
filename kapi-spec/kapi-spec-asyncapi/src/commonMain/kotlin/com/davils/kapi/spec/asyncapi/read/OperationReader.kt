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
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.asyncapi.model.operation.OperationAction
import com.davils.kapi.spec.asyncapi.model.operation.OperationBuilder
import com.davils.kapi.spec.asyncapi.model.operation.OperationReply
import com.davils.kapi.spec.asyncapi.model.operation.OperationReplyBuilder
import com.davils.kapi.spec.asyncapi.model.operation.OperationTrait
import com.davils.kapi.spec.asyncapi.model.operation.OperationTraitBuilder
import com.davils.kapi.spec.asyncapi.model.operation.ReplyAddress
import com.davils.kapi.spec.asyncapi.model.operation.ReplyAddressBuilder
import com.davils.kapi.spec.asyncapi.read.reference.ReferenceKind
import com.davils.kapi.spec.asyncapi.read.reference.References
import com.davils.kapi.spec.asyncapi.read.shape.Fields
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.asyncapi.read.shape.Shapes
import com.davils.kapi.spec.core.model.Reference

internal object OperationReader {
    fun operation(cursor: Cursor): Operation = cursor.read(Shapes.operation, OperationBuilder()) { fields ->
        action = actionOf(fields) ?: action
        channel = fields.cursor(Keys.CHANNEL)?.let { written ->
            References.required(written, ReferenceKind.Channel, "The channel of an operation")
        }
        title = fields.text(Keys.TITLE)
        summary = fields.text(Keys.SUMMARY)
        description = fields.text(Keys.DESCRIPTION)
        security.addAll(SecurityReader.schemesOf(fields))
        tags.addAll(InfoReader.tagsOf(fields))
        externalDocs = InfoReader.docsOf(fields)
        bindings = fields.cursor(Keys.BINDINGS)?.referable(ReferenceKind.OperationBindings, BindingReader::operation)
        traits.addAll(fields.items(Keys.TRAITS).referables(ReferenceKind.OperationTrait, ::trait))
        messages.addAll(messagesOf(fields, "A message of an operation"))
        reply = fields.cursor(Keys.REPLY)?.referable(ReferenceKind.OperationReply, ::reply)
    }

    fun trait(cursor: Cursor): OperationTrait = cursor.read(Shapes.operationTrait, OperationTraitBuilder()) { fields ->
        title = fields.text(Keys.TITLE)
        summary = fields.text(Keys.SUMMARY)
        description = fields.text(Keys.DESCRIPTION)
        security.addAll(SecurityReader.schemesOf(fields))
        tags.addAll(InfoReader.tagsOf(fields))
        externalDocs = InfoReader.docsOf(fields)
        bindings = fields.cursor(Keys.BINDINGS)?.referable(ReferenceKind.OperationBindings, BindingReader::operation)
    }

    fun reply(cursor: Cursor): OperationReply = cursor.read(Shapes.reply, OperationReplyBuilder()) { fields ->
        address = fields.cursor(Keys.ADDRESS)?.referable(ReferenceKind.ReplyAddress, ::replyAddress)
        channel = fields.cursor(Keys.CHANNEL)?.let { written ->
            References.required(written, ReferenceKind.Channel, "The channel of a reply")
        }
        messages.addAll(messagesOf(fields, "A message of a reply"))
    }

    fun replyAddress(cursor: Cursor): ReplyAddress = cursor.read(Shapes.replyAddress, ReplyAddressBuilder()) { fields ->
        description = fields.text(Keys.DESCRIPTION)
        expression = fields.text(Keys.LOCATION).orEmpty()
    }

    private fun messagesOf(fields: Fields, subject: String): List<Reference> {
        val written = fields.items(Keys.MESSAGES)
        return written.mapNotNull { item -> References.required(item, ReferenceKind.Message, subject) }
    }

    private fun actionOf(fields: Fields): OperationAction? {
        val written = fields.text(Keys.ACTION) ?: return null
        val action = OperationAction.entries.firstOrNull { candidate -> candidate.keyword == written }
        if (action == null) {
            val message = "The action of an operation is send or receive, not ${shown(written)}"
            fields.report(Keys.ACTION, DiagnosticCodes.OPERATION_ACTION, message)
        }
        return action
    }
}
