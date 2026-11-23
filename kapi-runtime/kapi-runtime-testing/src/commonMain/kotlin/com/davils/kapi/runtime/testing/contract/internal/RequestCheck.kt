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

import com.davils.kapi.runtime.testing.contract.ContractCodes
import com.davils.kapi.runtime.testing.contract.ExchangeSide
import com.davils.kapi.runtime.testing.mock.RecordedRequest
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.content.RequestBody
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.parameter.ParameterPlacement
import io.ktor.http.HttpHeaders

private const val BODY: String = "body"

private val ignoredHeaders: Set<String> = setOf("accept", "content-type", "authorization")

internal class RequestCheck(
    private val context: CheckContext,
    private val findings: Findings,
    private val request: RecordedRequest,
    private val match: OperationMatch.Found,
) {
    private val side: ExchangeSide = ExchangeSide.Request

    private val values: ValueCheck = ValueCheck(
        context = context,
        findings = findings,
        side = side,
        invalid = ContractCodes.PARAMETER_INVALID,
    )

    private val query: List<Pair<String, String>> = request.url.parameters.entries().flatMap { (name, found) ->
        found.map { value -> name to value }
    }

    suspend fun check() {
        val shapes = parameters().map(ParameterShape::of)
        val queries = shapes.filter { shape -> shape.placement == ParameterPlacement.Query }
        val declared = queries.map(ParameterShape::name).toSet()
        shapes.filterNot(::isIgnored).forEach { shape -> parameter(shape, declared) }
        val body = match.operation.requestBody
        body(declared = body?.let { found -> resolve(found, BODY) }, isDeclared = body != null)
    }

    private fun parameters(): List<Parameter> {
        val all = match.item.parameters + match.operation.parameters
        val resolved = all.mapNotNull { referable -> resolve(referable, "parameter") }
        val unique = resolved.associateBy { parameter -> parameter.name to parameter.placement }
        return unique.values.toList()
    }

    private inline fun <reified T : Element> resolve(referable: Referable<T>, subject: String): T? =
        context.resolved(referable = referable, findings = findings, side = side, subject = subject)

    private fun isIgnored(shape: ParameterShape): Boolean =
        shape.placement == ParameterPlacement.Header && shape.name.lowercase() in ignoredHeaders

    private suspend fun parameter(shape: ParameterShape, declared: Set<String>) {
        val raw = rawOf(shape, declared)
        if (raw.isNotEmpty()) return values.check(shape = shape, raw = raw, query = query, declared = declared)
        if (shape.isRequired) {
            findings.add(
                code = ContractCodes.PARAMETER_MISSING,
                side = side,
                subject = shape.subject,
                message = "The required parameter is missing",
                location = shape.location,
            )
        }
    }

    private fun rawOf(shape: ParameterShape, declared: Set<String>): List<String> = when (shape.placement) {
        ParameterPlacement.Path -> valuesOf(match.parameters, shape.name)
        ParameterPlacement.Query -> context.values.queryValues(shape = shape, query = query, declared = declared)
        ParameterPlacement.Header -> headerOf(shape.name)
        ParameterPlacement.Cookie -> valuesOf(cookies(), shape.name)
        ParameterPlacement.QueryString -> listOfNotNull(request.url.encodedQuery.takeIf(String::isNotEmpty))
    }

    private fun valuesOf(pairs: List<Pair<String, String>>, name: String): List<String> {
        val named = pairs.filter { (found, _) -> found == name }
        return named.map { (_, value) -> value }
    }

    private fun headerOf(name: String): List<String> {
        val found = request.headers.getAll(name) ?: return emptyList()
        return listOf(found.joinToString(","))
    }

    private fun cookies(): List<Pair<String, String>> {
        val headers = request.headers.getAll(HttpHeaders.Cookie).orEmpty()
        val parts = headers.joinToString(";").split(';')
        val pairs = parts.map(String::trim).filter { part -> '=' in part }
        return pairs.map { part -> part.substringBefore('=') to part.substringAfter('=') }
    }

    private suspend fun body(declared: RequestBody?, isDeclared: Boolean) {
        val isEmpty = request.body.size == 0
        if (isEmpty || declared == null) return absent(declared = declared, isDeclared = isDeclared, isEmpty = isEmpty)
        val content = ContentCheck(context, findings, side)
        content.check(
            content = declared.content,
            contentType = request.contentType,
            body = request.body,
            location = declared.location,
        )
    }

    private fun absent(declared: RequestBody?, isDeclared: Boolean, isEmpty: Boolean) {
        val isMissing = isEmpty && declared?.isRequired == true
        val isUndeclared = !isEmpty && !isDeclared
        if (isMissing) {
            findings.add(
                code = ContractCodes.BODY_MISSING,
                side = side,
                subject = BODY,
                message = "The required body is missing",
                location = declared.location,
            )
        }
        if (isUndeclared) {
            findings.add(
                code = ContractCodes.BODY_UNDECLARED,
                side = side,
                subject = BODY,
                message = "The operation declares no request body",
                location = match.operation.location,
            )
        }
    }
}
