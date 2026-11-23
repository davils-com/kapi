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
import com.davils.kapi.codegen.core.asyncapi.plan.Handshake
import com.davils.kapi.codegen.core.asyncapi.plan.HandshakeField
import com.davils.kapi.codegen.core.asyncapi.plan.HandshakePlace
import com.davils.kapi.codegen.core.asyncapi.plan.ScalarType
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.model.binding.WebSocketChannelBinding
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.resolve
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.Value

private const val PROTOCOL: String = "sec-websocket-protocol"

private const val TOKEN_SYMBOLS: String = "!#$%&'*+-.^_`|~"

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
        val query = binding?.query?.let { referable -> fields(channel, referable, HandshakePlace.Query) }.orEmpty()
        val headers = binding?.headers?.let { referable -> fields(channel, referable, HandshakePlace.Header) }.orEmpty()
        val protocol = headers.firstOrNull { field -> field.name.lowercase() == PROTOCOL }
        val written = headers.filterNot { field -> field.name.lowercase() == PROTOCOL }
        return Handshake(query + written, protocol?.choices.orEmpty())
    }

    private fun fields(channel: String, referable: Referable<Schema>, place: HandshakePlace): List<HandshakeField> {
        val schema = document.resolve(referable)
        val definition = targets.resolved(schema?.definition) as? Value.Mapping
        val type = (definition?.get(TYPE) as? Value.Text)?.value
        val properties = (definition?.get(PROPERTIES) as? Value.Mapping)?.takeIf { type == OBJECT }
        if (properties == null) {
            val text = "The ${place.label} of the ws binding of the channel ${shown(channel)} is no object with " +
                "properties"
            report.error(GenerationCodes.HANDSHAKE_UNSUPPORTED, text, schema?.location)
        }
        val required = (definition?.get(REQUIRED) as? Value.Sequence)?.items.orEmpty()
        val names = required.mapNotNull { item -> (item as? Value.Text)?.value }.toSet()
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
        val resolved = targets.resolved(value) as? Value.Mapping
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
        val isHeaderName = place == HandshakePlace.Query || isToken(name)
        if (scalar == null || !isHeaderName) {
            val text = "The ${place.label} ${shown(name)} of the channel ${shown(channel)} is no string, integer, " +
                "number or boolean, or no field name"
            report.error(GenerationCodes.HANDSHAKE_UNSUPPORTED, text, value.location)
        }
        val isOffered = choices.isNotEmpty() && choices.all(::isToken)
        if (isProtocol && !isOffered) {
            val text = "The subprotocols of the channel ${shown(channel)} must be tokens named by const or enum"
            report.error(GenerationCodes.HANDSHAKE_UNSUPPORTED, text, value.location)
        }
    }

    private fun scalarOf(schema: Value.Mapping?): ScalarType? {
        val type = (schema?.get(TYPE) as? Value.Text)?.value
        val format = (schema?.get(FORMAT) as? Value.Text)?.value
        val integer = ScalarType.Int.takeIf { format == INT32 } ?: ScalarType.Long
        val scalar = type?.let(scalars::get)
        return integer.takeIf { type == INTEGER } ?: scalar
    }

    private fun choicesOf(schema: Value.Mapping?): List<String> {
        val constant = (schema?.get(CONST) as? Value.Text)?.value
        val listed = (schema?.get(ENUM) as? Value.Sequence)?.items.orEmpty()
        val texts = listed.mapNotNull { item -> (item as? Value.Text)?.value }
        return listOfNotNull(constant).ifEmpty { texts.takeIf { texts.size == listed.size }.orEmpty() }
    }

    private fun isToken(text: String): Boolean {
        val isTokenText = text.all { char ->
            char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char in TOKEN_SYMBOLS
        }
        return text.isNotEmpty() && isTokenText
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
