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

import com.davils.kapi.spec.asyncapi.model.message.CorrelationId
import com.davils.kapi.spec.asyncapi.model.message.CorrelationIdBuilder
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.message.MessageBuilder
import com.davils.kapi.spec.asyncapi.model.message.MessageExample
import com.davils.kapi.spec.asyncapi.model.message.MessageExampleBuilder
import com.davils.kapi.spec.asyncapi.model.message.MessageTrait
import com.davils.kapi.spec.asyncapi.model.message.MessageTraitBuilder
import com.davils.kapi.spec.asyncapi.read.reference.ReferenceKind
import com.davils.kapi.spec.asyncapi.read.shape.Fields
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.asyncapi.read.shape.Shapes
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.Value

internal object MessageReader {
    fun message(cursor: Cursor): Message = cursor.read(Shapes.message, MessageBuilder()) { fields ->
        headers = SchemaReader.slot(fields, Keys.HEADERS)
        payload = SchemaReader.slot(fields, Keys.PAYLOAD)
        correlationId = correlationIdOf(fields)
        contentType = fields.text(Keys.CONTENT_TYPE)
        name = fields.text(Keys.NAME)
        title = fields.text(Keys.TITLE)
        summary = fields.text(Keys.SUMMARY)
        description = fields.text(Keys.DESCRIPTION)
        tags.addAll(InfoReader.tagsOf(fields))
        externalDocs = InfoReader.docsOf(fields)
        bindings = fields.cursor(Keys.BINDINGS)?.referable(ReferenceKind.MessageBindings, BindingReader::message)
        examples.addAll(fields.items(Keys.EXAMPLES).map(::example))
        traits.addAll(fields.items(Keys.TRAITS).referables(ReferenceKind.MessageTrait, ::trait))
    }

    fun trait(cursor: Cursor): MessageTrait = cursor.read(Shapes.messageTrait, MessageTraitBuilder()) { fields ->
        headers = SchemaReader.slot(fields, Keys.HEADERS)
        correlationId = correlationIdOf(fields)
        contentType = fields.text(Keys.CONTENT_TYPE)
        name = fields.text(Keys.NAME)
        title = fields.text(Keys.TITLE)
        summary = fields.text(Keys.SUMMARY)
        description = fields.text(Keys.DESCRIPTION)
        tags.addAll(InfoReader.tagsOf(fields))
        externalDocs = InfoReader.docsOf(fields)
        bindings = fields.cursor(Keys.BINDINGS)?.referable(ReferenceKind.MessageBindings, BindingReader::message)
        examples.addAll(fields.items(Keys.EXAMPLES).map(::example))
    }

    fun correlationId(cursor: Cursor): CorrelationId = cursor.read(
        Shapes.correlationId,
        CorrelationIdBuilder(),
    ) { fields ->
        description = fields.text(Keys.DESCRIPTION)
        expression = fields.text(Keys.LOCATION).orEmpty()
    }

    private fun correlationIdOf(fields: Fields): Referable<CorrelationId>? =
        fields.cursor(Keys.CORRELATION_ID)?.referable(ReferenceKind.CorrelationId, ::correlationId)

    private fun example(cursor: Cursor): MessageExample = cursor.read(
        Shapes.messageExample,
        MessageExampleBuilder(),
    ) { fields ->
        headers.putAll(headersOf(fields))
        payload = fields.value(Keys.PAYLOAD)
        name = fields.text(Keys.NAME)
        summary = fields.text(Keys.SUMMARY)
    }

    private fun headersOf(fields: Fields): Map<String, Value> {
        val entries = fields.entries(Keys.HEADERS)
        return entries.associate { entry -> entry.key to Values.of(entry.value) }
    }
}
