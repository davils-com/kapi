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

import com.davils.kapi.runtime.testing.contract.ExchangeSide
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kore.core.failure.contained
import com.davils.kore.core.validation.violation.ViolationCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

internal class ValueCheck(
    private val context: CheckContext,
    private val findings: Findings,
    private val side: ExchangeSide,
    private val invalid: ViolationCode,
) {
    suspend fun check(
        shape: ParameterShape,
        raw: List<String>,
        query: List<Pair<String, String>>,
        declared: Set<String>,
    ) {
        val content = shape.content.entries.firstOrNull()
        if (content == null) return schemaCheck(shape = shape, raw = raw, query = query, declared = declared)
        contentCheck(shape = shape, key = content.key, referable = content.value, text = raw.first())
    }

    private suspend fun contentCheck(
        shape: ParameterShape,
        key: String,
        referable: Referable<MediaType>,
        text: String,
    ) {
        val schema = resolve(referable, shape.subject)?.schema ?: return
        val value = valueOf(text = text, isJson = MediaTypes.isJson(MediaTypes.essenceOf(key)))
        if (value == null) return report(shape)
        validate(shape = shape, schema = schema, value = value)
    }

    private inline fun <reified T : Element> resolve(referable: Referable<T>, subject: String): T? =
        context.resolved(referable = referable, findings = findings, side = side, subject = subject)

    private fun valueOf(text: String, isJson: Boolean): JsonElement? {
        if (!isJson) return JsonPrimitive(text)
        return contained { Json.parseToJsonElement(text) }.getOrNull()
    }

    private suspend fun schemaCheck(
        shape: ParameterShape,
        raw: List<String>,
        query: List<Pair<String, String>>,
        declared: Set<String>,
    ) {
        val schema = shape.schema ?: return
        val value = context.values.json(shape = shape, raw = raw, query = query, declared = declared)
        validate(shape = shape, schema = schema, value = value)
    }

    private suspend fun validate(shape: ParameterShape, schema: Schema, value: JsonElement) {
        val problems = context.schemas.validate(schema = schema, value = value, invalid = invalid)
        problems.forEach { problem ->
            findings.add(
                code = problem.code,
                side = side,
                subject = shape.subject,
                message = problem.message,
                location = shape.location,
                instance = problem.instance,
            )
        }
    }

    private fun report(shape: ParameterShape) {
        findings.add(
            code = invalid,
            side = side,
            subject = shape.subject,
            message = "The value is no JSON",
            location = shape.location,
        )
    }
}
