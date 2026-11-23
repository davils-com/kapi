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

import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.parameter.ParameterPlacement
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.resolve

internal object QueryStringRules {
    fun check(scope: RuleScope) {
        val parameters = scope.all<Parameter>()
        val whole = parameters.filter { parameter -> parameter.placement == ParameterPlacement.QueryString }
        whole.forEach { parameter -> shape(scope, parameter) }
        scope.all<PathItem>().forEach { item -> pathItem(scope, item) }
    }

    private fun shape(scope: RuleScope, parameter: Parameter) {
        val isSerialized = parameter.isExploded != null || parameter.isReservedAllowed
        if (parameter.schema == null && parameter.content.size == 1 && !isSerialized) return
        val name = parameter.name
        val message = "The querystring parameter $name needs a content with exactly one media type and no schema, " +
            "style, explode or allowReserved"
        scope.report(DiagnosticCodes.PARAMETER_QUERYSTRING, message, parameter)
    }

    private fun pathItem(scope: RuleScope, item: PathItem) {
        val shared = resolved(scope, item.parameters)
        val operations = item.operations.values + item.additionalOperations.values
        operations.forEach { operation ->
            val own = resolved(scope, operation.parameters)
            conflicts(scope, operation, effective(shared, own))
        }
    }

    private fun conflicts(scope: RuleScope, operation: Operation, parameters: List<Parameter>) {
        val whole = parameters.count { parameter -> parameter.placement == ParameterPlacement.QueryString }
        val hasQuery = parameters.any { parameter -> parameter.placement == ParameterPlacement.Query }
        if (whole > 1) {
            val message = "The operation has $whole parameters in querystring, but the query string is one"
            scope.report(DiagnosticCodes.PARAMETER_QUERYSTRING_REPEATED, message, operation)
        }
        if (whole == 0 || !hasQuery) return
        val message = "The operation describes its query string both whole, by a querystring parameter, and by query " +
            "parameters"
        scope.report(DiagnosticCodes.PARAMETER_QUERYSTRING_WITH_QUERY, message, operation)
    }

    private fun resolved(scope: RuleScope, parameters: List<Referable<Parameter>>): List<Parameter> =
        parameters.mapNotNull { parameter -> scope.document.resolve(parameter) }

    private fun effective(shared: List<Parameter>, own: List<Parameter>): List<Parameter> {
        val overridden = own.map { parameter -> parameter.name to parameter.placement }.toSet()
        val inherited = shared.filterNot { parameter -> parameter.name to parameter.placement in overridden }
        return inherited + own
    }
}
