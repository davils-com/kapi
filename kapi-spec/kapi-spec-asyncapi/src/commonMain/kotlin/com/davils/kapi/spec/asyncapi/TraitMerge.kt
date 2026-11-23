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

package com.davils.kapi.spec.asyncapi

import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.message.MessageBuilder
import com.davils.kapi.spec.asyncapi.model.message.MessageTrait
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.asyncapi.model.operation.OperationBuilder
import com.davils.kapi.spec.asyncapi.model.operation.OperationTrait
import com.davils.kapi.spec.core.collection.toKeyedMap
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.value.Extensions
import com.davils.kore.core.annotation.DavilsInternal

internal object TraitMerge {
    fun operation(operation: Operation, traits: List<OperationTrait>): Operation {
        val latest = traits.asReversed()
        val builder = OperationBuilder()
        builder.location = operation.location
        builder.adopt(extensions(operation, traits))
        builder.action = operation.action
        builder.channel = operation.channel
        builder.title = operation.title ?: latest.firstNotNullOfOrNull(OperationTrait::title)
        builder.summary = operation.summary ?: latest.firstNotNullOfOrNull(OperationTrait::summary)
        builder.description = operation.description ?: latest.firstNotNullOfOrNull(OperationTrait::description)
        builder.security.addAll(firstNonEmpty(listOf(operation.security) + latest.map(OperationTrait::security)))
        builder.tags.addAll(firstNonEmpty(listOf(operation.tags) + latest.map(OperationTrait::tags)))
        builder.externalDocs = operation.externalDocs ?: latest.firstNotNullOfOrNull(OperationTrait::externalDocs)
        builder.bindings = operation.bindings ?: latest.firstNotNullOfOrNull(OperationTrait::bindings)
        builder.messages.addAll(operation.messages)
        builder.reply = operation.reply
        return builder.build()
    }

    fun message(message: Message, traits: List<MessageTrait>): Message {
        val latest = traits.asReversed()
        val builder = MessageBuilder()
        builder.location = message.location
        builder.adopt(extensions(message, traits))
        builder.headers = message.headers ?: latest.firstNotNullOfOrNull(MessageTrait::headers)
        builder.payload = message.payload
        builder.correlationId = message.correlationId ?: latest.firstNotNullOfOrNull(MessageTrait::correlationId)
        builder.contentType = message.contentType ?: latest.firstNotNullOfOrNull(MessageTrait::contentType)
        builder.name = message.name ?: latest.firstNotNullOfOrNull(MessageTrait::name)
        builder.title = message.title ?: latest.firstNotNullOfOrNull(MessageTrait::title)
        builder.summary = message.summary ?: latest.firstNotNullOfOrNull(MessageTrait::summary)
        builder.description = message.description ?: latest.firstNotNullOfOrNull(MessageTrait::description)
        builder.tags.addAll(firstNonEmpty(listOf(message.tags) + latest.map(MessageTrait::tags)))
        builder.externalDocs = message.externalDocs ?: latest.firstNotNullOfOrNull(MessageTrait::externalDocs)
        builder.bindings = message.bindings ?: latest.firstNotNullOfOrNull(MessageTrait::bindings)
        builder.examples.addAll(firstNonEmpty(listOf(message.examples) + latest.map(MessageTrait::examples)))
        return builder.build()
    }

    private fun <T> firstNonEmpty(candidates: List<List<T>>): List<T> =
        candidates.firstOrNull { candidate -> candidate.isNotEmpty() }.orEmpty()

    private fun extensions(target: Element, traits: List<Element>): Extensions {
        val inherited = traits.flatMap { trait -> trait.extensions.entries.toList() }
        val own = target.extensions.entries.toList()
        val merged = (inherited + own).toKeyedMap()
        return Extensions(merged)
    }
}
