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
import com.davils.kapi.runtime.testing.mock.RecordedResponse
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.content.Response
import com.davils.kapi.spec.openapi.model.content.ResponseStatus
import com.davils.kapi.spec.openapi.model.path.Operation

private const val CONTENT_TYPE: String = "content-type"

private const val BODY: String = "body"

private const val HUNDRED: Int = 100

internal class ResponseCheck(
    private val context: CheckContext,
    private val findings: Findings,
    private val response: RecordedResponse,
    private val operation: Operation,
) {
    private val side: ExchangeSide = ExchangeSide.Response

    suspend fun check() {
        val entry = entryOf(response.status)
        if (entry == null) return undocumented()
        val declared = resolve(entry, "status ${response.status}") ?: return
        headers(declared)
        body(declared)
    }

    private inline fun <reified T : Element> resolve(referable: Referable<T>, subject: String): T? =
        context.resolved(referable = referable, findings = findings, side = side, subject = subject)

    private fun entryOf(status: Int): Referable<Response>? {
        val entries = operation.responses.entries
        val exact = ResponseStatus(status.toString())
        val range = ResponseStatus("${status / HUNDRED}XX")
        val keys = listOf(exact, range, ResponseStatus.Default)
        return keys.firstNotNullOfOrNull(entries::get)
    }

    private fun undocumented() {
        findings.add(
            code = ContractCodes.STATUS_UNDOCUMENTED,
            side = side,
            subject = "status ${response.status}",
            message = "The operation documents no response for the status",
            location = operation.responses.location,
        )
    }

    private suspend fun headers(declared: Response) {
        val values = ValueCheck(
            context = context,
            findings = findings,
            side = side,
            invalid = ContractCodes.HEADER_INVALID,
        )
        val headers = declared.headers.filterKeys { name -> name.lowercase() != CONTENT_TYPE }
        headers.forEach { (name, referable) ->
            val header = resolve(referable, "header $name")
            header?.let { found -> header(shape = ParameterShape.of(name, found), values = values) }
        }
    }

    private suspend fun header(shape: ParameterShape, values: ValueCheck) {
        val found = response.headers.getAll(shape.name)
        val raw = found?.let { all -> listOf(all.joinToString(",")) }
        raw?.let { present -> values.check(shape = shape, raw = present, query = emptyList(), declared = emptySet()) }
        raw ?: shape.takeIf(ParameterShape::isRequired)?.let(::missing)
    }

    private fun missing(shape: ParameterShape) {
        findings.add(
            code = ContractCodes.HEADER_MISSING,
            side = side,
            subject = "header ${shape.name}",
            message = "The required header is missing",
            location = shape.location,
        )
    }

    private suspend fun body(declared: Response) {
        if (response.body.size == 0) return
        if (declared.content.isEmpty()) return undeclared(declared)
        ContentCheck(context, findings, side).check(
            content = declared.content,
            contentType = response.contentType,
            body = response.body,
            location = declared.location,
        )
    }

    private fun undeclared(declared: Response) {
        findings.add(
            code = ContractCodes.BODY_UNDECLARED,
            side = side,
            subject = BODY,
            message = "The response declares no content",
            location = declared.location,
        )
    }
}
