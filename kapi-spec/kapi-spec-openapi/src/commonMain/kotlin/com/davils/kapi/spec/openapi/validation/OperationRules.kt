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

package com.davils.kapi.spec.openapi.validation

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.path.Operation

internal object OperationRules {
    fun check(scope: RuleScope) {
        val operations = scope.all<Operation>()
        val named = operations.filter { operation -> operation.operationId != null }
        val repeated = named.groupBy(Operation::operationId).values.flatMap { same -> same.drop(1) }
        repeated.forEach { operation ->
            val message = "The operation id ${operation.operationId.orEmpty()} names more than one operation"
            scope.report(DiagnosticCodes.OPERATION_ID_DUPLICATE, message, operation)
        }
        val silent = operations.filter { operation -> operation.responses.entries.isEmpty() }
        silent.forEach { operation ->
            val message = "The operation declares no response"
            scope.report(DiagnosticCodes.OPERATION_RESPONSES_EMPTY, message, operation)
        }
    }
}
