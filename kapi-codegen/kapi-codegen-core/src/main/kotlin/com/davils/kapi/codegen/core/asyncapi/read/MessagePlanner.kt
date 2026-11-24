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

import com.davils.kapi.codegen.core.MediaTypes
import com.davils.kapi.codegen.core.asyncapi.GenerationCodes
import com.davils.kapi.codegen.core.asyncapi.plan.MessagePlan
import com.davils.kapi.codegen.core.asyncapi.plan.PayloadSource
import com.davils.kapi.codegen.core.naming.Identifiers
import com.davils.kapi.codegen.core.pipeline.Findings
import com.davils.kapi.codegen.core.pipeline.place
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.effective
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.model.schema.SchemaFormat
import com.davils.kapi.spec.asyncapi.resolve
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Target

private const val COMPANION: String = "Companion"

private val modelled: Set<SchemaFormat> = setOf(SchemaFormat.AsyncApi, SchemaFormat.JsonSchema, SchemaFormat.OpenApi)

internal class MessagePlanner(private val document: Document, private val report: Findings) {
    private val targets: Targets = Targets(document)

    fun plan(key: String, referable: Referable<Message>): MessagePlan? {
        val declared = document.resolve(referable) ?: return null
        val message = document.effective(declared)
        audit(key, message)
        return MessagePlan(
            key = key,
            member = memberOf(key),
            payload = payloadOf(key, message),
            correlation = correlationOf(key, message),
            summary = message.summary ?: message.title ?: message.description,
            location = message.place,
        )
    }

    private fun memberOf(key: String): String {
        val name = Identifiers.type(key)
        return "${name}Message".takeIf { name == COMPANION } ?: name
    }

    private fun audit(key: String, message: Message) {
        val type = message.contentType ?: document.defaultContentType
        if (type == null) {
            val text = "The message ${shown(key)} names no content type; it is read and written as application/json"
            report.warn(GenerationCodes.CONTENT_TYPE_ASSUMED, text, message.location)
        }
        if (type != null && !MediaTypes.isJson(type)) {
            val text = "The message ${shown(key)} has the content type ${shown(type)}; the generator writes JSON only"
            report.error(GenerationCodes.CONTENT_TYPE_UNSUPPORTED, text, message.location)
        }
        if (message.headers != null) {
            val text = "The headers of the message ${shown(key)} are neither sent nor received: " +
                "the frames of a WebSocket carry no headers"
            report.warn(GenerationCodes.MESSAGE_HEADERS, text, message.location)
        }
    }

    private fun payloadOf(key: String, message: Message): PayloadSource {
        val referable = message.payload ?: return PayloadSource.Anything
        val schema = document.resolve(referable)
        val target = targets.targetOf(referable)
        schema?.let { found -> auditFormat(key, found) }
        val declared = declaredOf(target, schema)
        if (declared == null) {
            val text = "The payload of the message ${shown(key)} is no schema the reader resolved"
            report.error(GenerationCodes.REFERENCE_UNSUPPORTED, text, message.location)
        }
        return declared ?: PayloadSource.Anything
    }

    private fun declaredOf(target: Target?, schema: Schema?): PayloadSource.Declared? {
        if (target == null || schema == null) return null
        return PayloadSource.Declared(target, schema)
    }

    private fun auditFormat(key: String, schema: Schema) {
        if (schema.format in modelled) return
        val format = schema.schemaFormat ?: schema.format.name
        val text = "The payload of the message ${shown(key)} is written in ${shown(format)}, which the generator " +
            "has no Kotlin for; write it in JSON Schema or the schema of AsyncAPI"
        report.error(GenerationCodes.SCHEMA_FORMAT, text, schema.location)
    }

    private fun correlationOf(key: String, message: Message): List<String>? {
        val referable = message.correlationId ?: return null
        val correlation = document.resolve(referable)
        val expression = correlation?.expression.orEmpty()
        val tokens = Correlations.tokens(expression)
        if (tokens == null) {
            val text = "The correlation id of the message ${shown(key)} is ${shown(expression)}, but only " +
                "\$message.payload# with a JSON pointer can be read: the frames of a WebSocket carry no headers"
            report.error(GenerationCodes.CORRELATION_UNSUPPORTED, text, correlation?.location ?: message.location)
        }
        return tokens
    }
}
