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
import com.davils.kapi.runtime.testing.contract.ContractFinding
import com.davils.kapi.runtime.testing.contract.ExchangeSide
import com.davils.kapi.runtime.testing.mock.RecordedExchange
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.schema.CompilerBuilder

internal class ExchangeCheck(document: Document, basePath: String, configure: CompilerBuilder.() -> Unit) {
    private val context: CheckContext = CheckContext(document, SchemaCache(document, configure))

    private val operations: Operations = Operations(document, basePath)

    suspend fun findings(index: Int, exchange: RecordedExchange): List<ContractFinding> {
        val request = exchange.request
        val method = request.method.uppercase()
        return when (val match = operations.match(method, request.path)) {
            OperationMatch.Unknown -> {
                val findings = Findings(exchange = index, operation = "$method ${request.path}")
                findings.add(
                    code = ContractCodes.OPERATION_UNKNOWN,
                    side = ExchangeSide.Request,
                    subject = "path",
                    message = "No path template of the document matches the path",
                    location = context.document.paths?.location,
                )
                findings.all
            }

            is OperationMatch.PathOnly -> {
                val findings = Findings(exchange = index, operation = "$method ${match.template}")
                findings.add(
                    code = ContractCodes.METHOD_UNDECLARED,
                    side = ExchangeSide.Request,
                    subject = "method",
                    message = "The path item declares no operation for the method",
                    location = match.item.location,
                )
                findings.all
            }

            is OperationMatch.Found -> {
                val findings = Findings(exchange = index, operation = "$method ${match.template}")
                RequestCheck(context = context, findings = findings, request = request, match = match).check()
                ResponseCheck(
                    context = context,
                    findings = findings,
                    response = exchange.response,
                    operation = match.operation,
                ).check()
                findings.all
            }
        }
    }
}
