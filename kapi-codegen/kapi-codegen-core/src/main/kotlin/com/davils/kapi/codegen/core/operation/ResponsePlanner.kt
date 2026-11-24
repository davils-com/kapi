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

package com.davils.kapi.codegen.core.operation

import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.MediaTypes
import com.davils.kapi.codegen.core.model.ModelPlanner
import com.davils.kapi.codegen.core.model.PlanScope
import com.davils.kapi.codegen.core.model.plan.Chunks
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.operation.plan.ResponsePlan
import com.davils.kapi.codegen.core.operation.plan.StreamKind
import com.davils.kapi.codegen.core.operation.plan.StreamPlan
import com.davils.kapi.codegen.core.pipeline.valueOrNull
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.content.Response
import com.davils.kapi.spec.openapi.model.content.ResponseStatus
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.resolve

private const val OTHER: String = "Other"

private const val ITEM: String = "Item"

private val streams: Map<String, StreamKind> = mapOf(
    "text/event-stream" to StreamKind.Events,
    "application/x-ndjson" to StreamKind.Records,
    "application/jsonl" to StreamKind.Records,
    "application/x-jsonlines" to StreamKind.Records,
    "application/json-seq" to StreamKind.Records,
)

private const val SEQUENCE: String = "application/json-seq"

private val json: TypeUse = TypeUse(TypeShape.Scalar(ScalarKind.Json))

private val names: Map<Int, String> = mapOf(
    200 to "Ok", 201 to "Created", 202 to "Accepted", 204 to "NoContent", 301 to "MovedPermanently", 302 to "Found",
    303 to "SeeOther", 304 to "NotModified", 307 to "TemporaryRedirect", 308 to "PermanentRedirect",
    400 to "BadRequest", 401 to "Unauthorized", 403 to "Forbidden", 404 to "NotFound", 405 to "MethodNotAllowed",
    406 to "NotAcceptable", 409 to "Conflict", 410 to "Gone", 412 to "PreconditionFailed",
    413 to "ContentTooLarge", 415 to "UnsupportedMediaType", 422 to "UnprocessableContent",
    428 to "PreconditionRequired", 429 to "TooManyRequests", 500 to "InternalServerError", 501 to "NotImplemented",
    502 to "BadGateway", 503 to "ServiceUnavailable", 504 to "GatewayTimeout",
)

private val ranges: Map<Int, String> = mapOf(
    1 to "Informational",
    2 to "Successful",
    3 to "Redirection",
    4 to "ClientError",
    5 to "ServerError",
)

internal class ResponsePlanner(private val scope: PlanScope, private val models: ModelPlanner) {
    private val headers: HeaderPlanner = HeaderPlanner(scope, models)

    fun plan(context: String, operation: Operation): List<ResponsePlan> {
        val declared = operation.responses.entries
        val entries = declared.entries.sortedBy { (status, _) -> status.value }
        return entries.flatMap { (status, referable) ->
            val resolved = listOfNotNull(scope.document.resolve(referable))
            resolved.map { response -> response(context, status, response) }
        }
    }

    private fun response(context: String, status: ResponseStatus, response: Response): ResponsePlan {
        val digit = status.value.first()
        val range = digit.digitToIntOrNull().takeIf { status.isRange }
        val name = nameOf(status, range)
        val forced = forcedOf(context + name, response)
        val media = jsonOf(scope.contentOf(response.content)).takeIf { forced == null }
        val accepted = listOfNotNull(forced?.first ?: media?.key)
        return ResponsePlan(
            name = name,
            status = status.code,
            range = range,
            body = media?.let { content -> typeOf(content.value, context + name) },
            accept = accepted.ifEmpty { scope.contentOf(response.content).keys.toList() },
            headers = headers.plan(context + name, response),
            stream = forced?.second ?: streamOf(context + name, response).takeIf { media == null },
        )
    }

    private fun forcedOf(context: String, response: Response): Pair<String, StreamPlan>? {
        val content = scope.contentOf(response.content)
        val declared = content.entries.firstOrNull { (_, media) -> declaredKind(media) != null }
        val (type, media) = declared ?: return null
        val kind = declaredKind(media) ?: StreamKind.Bytes
        val item = TypeUse(Chunks.item).takeIf { kind == StreamKind.Bytes } ?: itemOf(media, context + ITEM)
        return type to StreamPlan(kind, item)
    }

    private fun declaredKind(media: MediaType): StreamKind? {
        val declared = media.extensions.text(Keywords.STREAM).valueOrNull()
        return declared?.let(StreamKind::of)
    }

    private fun streamOf(context: String, response: Response): StreamPlan? {
        val kinds = scope.contentOf(response.content).mapKeys { (type, _) -> MediaTypes.essenceOf(type) }
        val streamed = kinds.entries.firstNotNullOfOrNull { (type, media) ->
            streams[type]?.let { kind -> Triple(kind, media, type == SEQUENCE) }
        }
        return streamed?.let { (kind, media, isSequence) ->
            StreamPlan(kind = kind, item = itemOf(media, context + ITEM), isSequence = isSequence)
        }
    }

    private fun nameOf(status: ResponseStatus, range: Int?): String {
        val code = status.code ?: return ranges[range] ?: OTHER
        return names[code] ?: "Status$code"
    }

    private fun jsonOf(content: Map<String, MediaType>): Map.Entry<String, MediaType>? =
        content.entries.firstOrNull { (type, _) -> MediaTypes.isJson(type) }

    private fun itemOf(media: MediaType, context: String): TypeUse {
        val schema = media.itemSchema ?: return typeOf(media, context)
        return models.typeOf(schema, context)
    }

    private fun typeOf(media: MediaType, context: String): TypeUse {
        val schema = media.schema ?: return json
        return models.typeOf(schema, context)
    }
}
