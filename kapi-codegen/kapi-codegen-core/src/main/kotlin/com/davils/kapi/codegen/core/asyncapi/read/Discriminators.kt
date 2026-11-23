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

import com.davils.kapi.codegen.core.asyncapi.GenerationCodes
import com.davils.kapi.codegen.core.asyncapi.Report
import com.davils.kapi.codegen.core.asyncapi.plan.DirectionPlan
import com.davils.kapi.codegen.core.asyncapi.plan.MessagePlan
import com.davils.kapi.codegen.core.asyncapi.plan.PayloadSource
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.value.Value

private const val PROPERTIES: String = "properties"

private const val REQUIRED: String = "required"

private const val CONST: String = "const"

private const val ENUM: String = "enum"

private const val DISCRIMINATOR: String = "discriminator"

internal class Discriminators(document: Document, private val report: Report) {
    private val targets: Targets = Targets(document)

    fun distinguished(channel: String, location: Location?, direction: DirectionPlan): DirectionPlan {
        if (!direction.isSealed) return direction
        val shapes = direction.messages.map(::constantsOf)
        val declarations = direction.messages.mapNotNull(::declaredOf)
        val declared = declarations.distinct().singleOrNull()
        val first = shapes.first().keys.toList()
        val ordered = listOfNotNull(declared).filter { name -> name in first } + first
        val property = ordered.firstOrNull { name -> isDistinct(name, shapes) }
        if (property == null) {
            val keys = direction.messages.joinToString { message -> shown(message.key) }
            val text = "The messages $keys of the channel ${shown(channel)} travel in one direction, but no " +
                "required property with a constant value tells them apart; give each payload one, such as " +
                "type: {const: chat}"
            report.error(GenerationCodes.MESSAGE_INDISTINCT, text, location)
        }
        val tagged = direction.messages.mapIndexed { index, message ->
            message.copy(tag = property?.let(shapes[index]::get))
        }
        return direction.copy(messages = tagged, discriminator = property)
    }

    private fun isDistinct(name: String, shapes: List<Map<String, String>>): Boolean {
        val values = shapes.mapNotNull { shape -> shape[name] }
        return values.size == shapes.size && values.distinct().size == values.size
    }

    private fun definitionOf(message: MessagePlan): Value.Mapping? {
        val payload = message.payload as? PayloadSource.Declared
        return targets.resolved(payload?.run { schema.definition }) as? Value.Mapping
    }

    private fun declaredOf(message: MessagePlan): String? = Fields.text(definitionOf(message), DISCRIMINATOR)

    private fun constantsOf(message: MessagePlan): Map<String, String> {
        val definition = definitionOf(message)
        val properties = Fields.mapping(definition, PROPERTIES)?.entries.orEmpty()
        val names = Fields.texts(Fields.items(definition, REQUIRED)).toSet()
        val constants = properties.filterKeys { name -> name in names }.mapValues { (_, value) -> constantOf(value) }
        return constants.mapNotNull { (name, value) -> value?.let { found -> name to found } }.toMap()
    }

    private fun constantOf(value: Value): String? {
        val schema = targets.resolved(value)
        val listed = Fields.texts(Fields.items(schema, ENUM)).singleOrNull()
        return Fields.text(schema, CONST) ?: listed
    }
}
