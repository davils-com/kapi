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

package com.davils.kapi.codegen.core.asyncapi.read

import com.davils.kapi.codegen.core.asyncapi.GenerationCodes
import com.davils.kapi.codegen.core.asyncapi.Report
import com.davils.kapi.codegen.core.asyncapi.plan.Handshake
import com.davils.kapi.codegen.core.asyncapi.plan.HandshakeField
import com.davils.kapi.codegen.core.asyncapi.plan.HandshakePlace
import com.davils.kapi.codegen.core.asyncapi.plan.ScalarType
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.runtime.core.header.FieldSyntax
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.model.binding.WebSocketChannelBinding
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.resolve
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal

private const val PROTOCOL: String = "sec-websocket-protocol"

private const val INT32: String = "int32"

private const val INTEGER: String = "integer"

private val scalars: Map<String, ScalarType> = mapOf(
    "string" to ScalarType.Text,
    INTEGER to ScalarType.Long,
    "number" to ScalarType.Double,
    "boolean" to ScalarType.Flag,
)

private val reservedHeaders: Set<String> = setOf(
    "authorization",
    "connection",
    "content-length",
    "cookie",
    "host",
    "sec-websocket-accept",
    "sec-websocket-extensions",
    "sec-websocket-key",
    "sec-websocket-version",
    "transfer-encoding",
    "upgrade",
)

internal class HandshakePlanner(private val document: Document, private val report: Report) {
    private val targets: Targets = Targets(document)

    private val HandshakePlace.label: String
        get() = when (this) {
            HandshakePlace.Query -> "query parameter"
            HandshakePlace.Header -> "header"
        }

    fun plan(channel: String, binding: WebSocketChannelBinding?): Handshake {
        val query = fieldsOf(channel, binding, HandshakePlace.Query)
        val headers = fieldsOf(channel, binding, HandshakePlace.Header)
        val protocol = headers.firstOrNull { field -> field.name.lowercase() == PROTOCOL }
        val written = headers.filterNot { field -> field.name.lowercase() == PROTOCOL }
        return Handshake(query + written, protocol?.choices.orEmpty())
    }

    private fun fieldsOf(
        channel: String,
        binding: WebSocketChannelBinding?,
        place: HandshakePlace,
    ): List<HandshakeField> {
        val location = binding?.location
        val referable = when (place) {
            HandshakePlace.Query -> binding?.query
            HandshakePlace.Header -> binding?.headers
        }
        val fields = referable?.let { found ->
            fields(channel = channel, referable = found, place = place, binding = location)
        }
        return fields.orEmpty()
    }

    private fun fields(
        channel: String,
        referable: Referable<Schema>,
        place: HandshakePlace,
        binding: Location?,
    ): List<HandshakeField> {
        val schema = document.resolve(referable)
        val definition = targets.resolved(schema?.definition)
        val type = Fields.text(definition, TYPE)
        val properties = Fields.mapping(definition, PROPERTIES)?.takeIf { type == OBJECT }
        if (properties == null) {
            val text = "The ${place.label} of the ws binding of the channel ${shown(channel)} is no object with " +
                "properties"
            report.error(GenerationCodes.HANDSHAKE_UNSUPPORTED, text, schema?.location ?: binding)
        }
        val names = Fields.texts(Fields.items(definition, REQUIRED)).toSet()
        val entries = properties?.entries.orEmpty()
        return entries.mapNotNull { (name, value) ->
            field(channel = channel, place = place, name = name, value = value, isRequired = name in names)
        }
    }

    private fun field(
        channel: String,
        place: HandshakePlace,
        name: String,
        value: Value,
        isRequired: Boolean,
    ): HandshakeField? {
        val resolved = targets.resolved(value)
        val scalar = scalarOf(resolved)
        val choices = choicesOf(resolved)
        val isHeader = place == HandshakePlace.Header
        val isProtocol = isHeader && name.lowercase() == PROTOCOL
        val isReserved = isHeader && name.lowercase() in reservedHeaders
        audit(
            channel = channel,
            place = place,
            name = name,
            scalar = scalar,
            isProtocol = isProtocol,
            choices = choices,
            value = value,
        )
        if (isReserved) {
            val text = "The header ${shown(name)} of the channel ${shown(channel)} is written by the handshake " +
                "itself and is no parameter"
            report.warn(GenerationCodes.HANDSHAKE_RESERVED, text, value.location)
        }
        val field = HandshakeField(
            name = name,
            member = Names.member(name),
            place = place,
            type = scalar ?: ScalarType.Text,
            isRequired = isRequired,
            choices = choices,
        )
        return field.takeIf { !isReserved }
    }

    private fun audit(
        channel: String,
        place: HandshakePlace,
        name: String,
        scalar: ScalarType?,
        isProtocol: Boolean,
        choices: List<String>,
        value: Value,
    ) {
        val isHeaderName = place == HandshakePlace.Query || FieldSyntax.isToken(name)
        if (scalar == null || !isHeaderName) {
            val text = "The ${place.label} ${shown(name)} of the channel ${shown(channel)} is no string, integer, " +
                "number or boolean, or no field name"
            report.error(GenerationCodes.HANDSHAKE_UNSUPPORTED, text, value.location)
        }
        val isOffered = choices.isNotEmpty() && choices.all(FieldSyntax::isToken)
        if (isProtocol && !isOffered) {
            val text = "The subprotocols of the channel ${shown(channel)} must be tokens named by const or enum"
            report.error(GenerationCodes.HANDSHAKE_UNSUPPORTED, text, value.location)
        }
    }

    private fun scalarOf(schema: Value?): ScalarType? {
        val type = Fields.text(schema, TYPE)
        val format = Fields.text(schema, FORMAT)
        val integer = ScalarType.Int.takeIf { format == INT32 } ?: ScalarType.Long
        val scalar = type?.let(scalars::get)
        return integer.takeIf { type == INTEGER } ?: scalar
    }

    private fun choicesOf(schema: Value?): List<String> {
        val listed = Fields.items(schema, ENUM)
        val texts = Fields.texts(listed)
        val isComplete = texts.size == listed.size
        return listOfNotNull(Fields.text(schema, CONST)).ifEmpty { texts.takeIf { isComplete }.orEmpty() }
    }

    private companion object {
        const val TYPE: String = "type"
        const val FORMAT: String = "format"
        const val OBJECT: String = "object"
        const val PROPERTIES: String = "properties"
        const val REQUIRED: String = "required"
        const val CONST: String = "const"
        const val ENUM: String = "enum"
    }
}
