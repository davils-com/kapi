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

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.MediaTypes
import com.davils.kapi.codegen.core.ModelKind
import com.davils.kapi.codegen.core.model.ModelPlanner
import com.davils.kapi.codegen.core.model.PlanScope
import com.davils.kapi.codegen.core.model.plan.Chunks
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.operation.plan.BodyKind
import com.davils.kapi.codegen.core.operation.plan.BodyPlan
import com.davils.kapi.codegen.core.operation.plan.StreamKind
import com.davils.kapi.spec.core.value.ExtensionReading
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.content.RequestBody
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.resolve

private const val BYTES: String = "application/octet-stream"

private const val WILDCARD: Char = '*'

private val json: TypeUse = TypeUse(TypeShape.Scalar(ScalarKind.Json))

private val bytes: TypeShape = TypeShape.External("kotlinx.io.bytestring.ByteString", serializer = null)

private val structured: Map<String, BodyKind> = mapOf(
    "multipart/" to BodyKind.Multipart,
    "application/x-www-form-urlencoded" to BodyKind.Form,
)

internal class BodyPlanner(private val scope: PlanScope, private val models: ModelPlanner) {
    private val fields: FieldPlanner = FieldPlanner(scope, models)

    fun plan(context: String, operation: Operation): BodyPlan? {
        val declared = listOfNotNull(operation.requestBody)
        val bodies = declared.mapNotNull { referable -> scope.document.resolve(referable) }
        val plans = bodies.flatMap { body ->
            val ranked = scope.contentOf(body.content).entries.sortedBy { (type, _) -> kindOf(type).ordinal }
            ranked.take(1).map { (type, media) -> plan(context + "Request", body, baseOf(type) to media) }
        }
        val body = plans.firstOrNull() ?: return null
        return body.copy(isPatch = isPatch(operation, body))
    }

    private fun isPatch(operation: Operation, body: BodyPlan): Boolean {
        val reading = operation.extensions.flag(Keywords.TRI_STATE) as? ExtensionReading.Present
        val named = body.type.shape as? TypeShape.Named
        val isObject = named?.kind == ModelKind.Object && named.packageName == null
        val isAsked = reading?.value == true
        val isPatchable = body.kind == BodyKind.Json && isObject
        if (isAsked && !isPatchable) {
            scope.findings.error(
                CodegenCodes.OPERATION_UNSUPPORTED,
                "The generator has no Kotlin for ${Keywords.TRI_STATE} on a body that is no JSON object " +
                    "of this document yet",
                operation.location,
            )
        }
        return isAsked && isPatchable
    }

    private fun plan(context: String, body: RequestBody, entry: Pair<String, MediaType>): BodyPlan {
        val (type, media) = entry
        val isOptional = !body.isRequired
        val kind = BodyKind.Upload.takeIf { isUpload(media) } ?: kindOf(type)
        return when (kind) {
            BodyKind.Json -> BodyPlan(kind, typeOf(media, context, isOptional), type)

            BodyKind.Binary -> BodyPlan(kind, TypeUse(bytes, isNullable = isOptional), concrete(type))

            BodyKind.Form, BodyKind.Multipart -> fields(
                context = context,
                kind = kind,
                entry = type to media,
                isOptional = isOptional,
            )

            BodyKind.Upload -> BodyPlan(kind, TypeUse(Chunks.stream, isNullable = isOptional), concrete(type))
        }
    }

    private fun isUpload(media: MediaType): Boolean {
        val reading = media.extensions.text(Keywords.STREAM) as? ExtensionReading.Present
        val kind = reading?.value ?: return false
        if (kind != StreamKind.Bytes.function) {
            scope.findings.error(
                CodegenCodes.EXTENSION_VALUE,
                "${Keywords.STREAM} on a request body streams bytes and nothing else",
                media.location,
            )
        }
        return kind == StreamKind.Bytes.function
    }

    private fun fields(context: String, kind: BodyKind, entry: Pair<String, MediaType>, isOptional: Boolean): BodyPlan =
        BodyPlan(
            kind = kind,
            type = json.copy(isNullable = isOptional),
            contentType = entry.first,
            fields = fields.plan(context, entry.second, kind),
        )

    private fun typeOf(media: MediaType, context: String, isOptional: Boolean): TypeUse {
        val schema = media.schema ?: return json.copy(isNullable = isOptional)
        val type = models.typeOf(schema, context)
        return type.copy(isNullable = type.isNullable || isOptional)
    }

    private fun kindOf(type: String): BodyKind {
        if (MediaTypes.isJson(type)) return BodyKind.Json
        val base = MediaTypes.essenceOf(type)
        val known = structured.filterKeys { prefix -> base.startsWith(prefix) }
        return known.values.firstOrNull() ?: BodyKind.Binary
    }

    private fun baseOf(type: String): String = type.substringBefore(';').trim()

    private fun concrete(type: String): String = type.takeIf { WILDCARD !in type } ?: BYTES
}
