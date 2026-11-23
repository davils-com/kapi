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

package com.davils.kapi.runtime.testing.contract.internal

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.runtime.testing.contract.ContractCodes
import com.davils.kapi.runtime.testing.contract.ExchangeSide
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kore.core.failure.contained
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import com.davils.kapi.runtime.core.negotiation.MediaType as ContentType

private const val BODY: String = "body"

internal class ContentCheck(
    private val context: CheckContext,
    private val findings: Findings,
    private val side: ExchangeSide,
) {
    suspend fun check(
        content: Map<String, Referable<MediaType>>,
        contentType: String?,
        body: ByteString,
        location: Location?,
    ) {
        val type = contentType?.let(ContentType::parseOrNull)
        val key = type?.let { found -> MediaTypes.select(content.keys, found) }
        if (type == null || key == null) return refused(isMissing = type == null, location = location)
        val media = resolve(content.getValue(key)) ?: return
        val essence = type.essence
        val schema = media.schema.takeIf { MediaTypes.isJson(essence) }
        val itemSchema = media.itemSchema.takeIf { MediaTypes.isRecords(essence) }
        schema?.let { found -> document(found, body, media) }
        itemSchema?.let { found -> records(found, body, media) }
    }

    private inline fun <reified T : Element> resolve(referable: Referable<T>): T? =
        context.resolved(referable = referable, findings = findings, side = side, subject = BODY)

    private fun refused(isMissing: Boolean, location: Location?) {
        val code = ContractCodes.CONTENT_TYPE_MISSING.takeIf { isMissing } ?: ContractCodes.CONTENT_TYPE_UNDECLARED
        val message = "The body has no media type".takeIf { isMissing } ?: "The media type of the body is not declared"
        findings.add(code = code, side = side, subject = BODY, message = message, location = location)
    }

    private suspend fun document(schema: Schema, body: ByteString, media: MediaType) {
        val json = parsed(body.decodeToString())
        json ?: malformed(media = media, instance = null)
        json?.let { found -> validate(schema = schema, json = found, media = media, prefix = JsonPointer.ROOT) }
    }

    private suspend fun records(schema: Schema, body: ByteString, media: MediaType) {
        val text = body.decodeToString()
        val parts = text.split('\n')
        val lines = parts.map(String::trim).filter(String::isNotEmpty)
        lines.forEachIndexed { index, line ->
            val prefix = JsonPointer.of(index.toString())
            val json = parsed(line)
            json ?: malformed(media = media, instance = prefix)
            json?.let { found -> validate(schema = schema, json = found, media = media, prefix = prefix) }
        }
    }

    private fun malformed(media: MediaType, instance: JsonPointer?) {
        findings.add(
            code = ContractCodes.BODY_MALFORMED,
            side = side,
            subject = BODY,
            message = "The body is no JSON",
            location = media.location,
            instance = instance,
        )
    }

    private suspend fun validate(schema: Schema, json: JsonElement, media: MediaType, prefix: JsonPointer) {
        val problems = context.schemas.validate(schema = schema, value = json, invalid = ContractCodes.BODY_INVALID)
        problems.forEach { problem ->
            val instance = problem.instance?.let { found -> JsonPointer.of(prefix.tokens + found.tokens) }
            findings.add(
                code = problem.code,
                side = side,
                subject = BODY,
                message = problem.message,
                location = media.location,
                instance = instance,
            )
        }
    }

    private fun parsed(text: String): JsonElement? = contained { Json.parseToJsonElement(text) }.getOrNull()
}
