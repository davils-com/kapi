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

package com.davils.kapi.codegen.core.sample

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.operation.plan.HeaderPlan
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ResponsePlan
import com.davils.kapi.codegen.core.pipeline.Findings
import com.davils.kapi.codegen.core.sample.plan.MockAnswer
import com.davils.kapi.codegen.core.sample.plan.SampleValue
import java.math.BigDecimal

private const val SUCCESS_RANGE: Int = 2
private const val HUNDRED: Int = 100

private val successes: Set<Int?> = (200..299).toSet()

internal class MockPlanner(private val samples: Samples, private val findings: Findings) {
    fun answer(operation: OperationPlan): MockAnswer? {
        val response = chosen(operation)
        val answer = response?.let(::answered)
        if (answer == null) unanswered(operation, response)
        return answer
    }

    private fun answered(response: ResponsePlan): MockAnswer? {
        val arguments = status(response) + body(response) + content(response) + items(response) + headers(response)
        val made = arguments.flatMap { (name, value) -> listOfNotNull(value).map { sample -> name to sample } }
        return MockAnswer(response, made.toMap()).takeIf { made.size == arguments.size }
    }

    private fun chosen(operation: OperationPlan): ResponsePlan? {
        val exact = operation.responses.filter { response -> response.status in successes }
        val ranged = operation.responses.filter { response -> response.range == SUCCESS_RANGE }
        val open = operation.responses.filter { response -> response.status == null && response.range == null }
        return (exact + ranged + open + operation.responses).firstOrNull()
    }

    private fun status(response: ResponsePlan): List<Pair<String, SampleValue?>> {
        val code = (response.range ?: SUCCESS_RANGE) * HUNDRED
        val status = STATUS to SampleValue.Number(BigDecimal(code), ScalarKind.Int)
        return listOf(status).filter { response.status == null }
    }

    private fun body(response: ResponsePlan): List<Pair<String, SampleValue?>> =
        listOfNotNull(response.body).map { type -> BODY to samples.of(type) }

    private fun content(response: ResponsePlan): List<Pair<String, SampleValue?>> {
        val isRaw = response.body == null && response.stream == null && response.accept.isNotEmpty()
        return listOf(CONTENT to SampleValue.Bytes).filter { isRaw }
    }

    private fun items(response: ResponsePlan): List<Pair<String, SampleValue?>> {
        val stream = response.stream ?: return emptyList()
        val item = samples.of(stream.item)
        return listOf(ITEMS to item?.let { sample -> SampleValue.Stream(listOf(sample)) })
    }

    private fun headers(response: ResponsePlan): List<Pair<String, SampleValue?>> =
        response.headers.map { header -> header.name to headerOf(header) }

    private fun headerOf(header: HeaderPlan): SampleValue? {
        if (header.type.isNullable) return SampleValue.Null
        return samples.of(header.type)
    }

    private fun unanswered(operation: OperationPlan, response: ResponsePlan?) {
        val unsampled = response?.let { chosen -> "its response ${chosen.name} has no sample" }
        val reason = unsampled ?: "it declares no response"
        findings.warn(
            CodegenCodes.MOCK_UNANSWERED,
            "The mock of ${operation.name} is left to a subclass, because $reason",
            operation.origin,
        )
    }

    companion object {
        const val STATUS: String = "status"
        const val BODY: String = "body"
        const val CONTENT: String = "content"

        const val ITEMS: String = "items"
    }
}
