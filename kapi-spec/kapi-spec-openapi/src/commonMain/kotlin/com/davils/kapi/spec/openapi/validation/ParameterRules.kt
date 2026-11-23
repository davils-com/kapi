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

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.parameter.ParameterPlacement
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.resolve

internal object ParameterRules {
    fun check(scope: RuleScope) {
        scope.all<Parameter>().forEach { parameter -> parameter(scope, parameter) }
        scope.all<Header>().forEach { header -> header(scope, header) }
        scope.all<PathItem>().forEach { item -> repeated(scope, item, item.parameters) }
        scope.all<Operation>().forEach { operation -> repeated(scope, operation, operation.parameters) }
    }

    private fun header(scope: RuleScope, header: Header) {
        val hasSchema = header.schema != null
        val isOneOf = hasSchema != header.content.isNotEmpty()
        if (isOneOf && header.content.size <= 1) return
        val message = "A header needs either a schema or a content with one media type"
        scope.report(DiagnosticCodes.PARAMETER_SHAPE, message, header)
    }

    private fun parameter(scope: RuleScope, parameter: Parameter) {
        val hasSchema = parameter.schema != null
        val isOneOf = hasSchema != parameter.content.isNotEmpty()
        if (!isOneOf || parameter.content.size > 1) {
            val message = "The parameter ${parameter.name} needs either a schema or a content with one media type"
            scope.report(DiagnosticCodes.PARAMETER_SHAPE, message, parameter)
        }
        style(scope, parameter)
        val isOptionalInPath = parameter.placement == ParameterPlacement.Path && !parameter.isRequired
        if (isOptionalInPath) {
            val message = "The path parameter ${parameter.name} is always required and should say so"
            scope.report(DiagnosticCodes.PARAMETER_PATH_OPTIONAL, message, parameter)
        }
    }

    private fun style(scope: RuleScope, parameter: Parameter) {
        val style = parameter.style ?: return
        if (style.isAllowedIn(parameter.placement)) return
        val place = parameter.placement.keyword
        val message = "The style ${style.keyword} cannot be used for the parameter ${parameter.name} in $place"
        scope.report(DiagnosticCodes.PARAMETER_STYLE, message, parameter)
    }

    private fun repeated(scope: RuleScope, owner: Element, parameters: List<Referable<Parameter>>) {
        val resolved = parameters.mapNotNull { parameter -> scope.document.resolve(parameter) }
        val alike = resolved.groupBy { parameter -> parameter.name to parameter.placement }
        val repeated = alike.filterValues { same -> same.size > 1 }
        repeated.keys.forEach { (name, placement) ->
            val message = "The parameter $name in ${placement.keyword} is declared more than once"
            scope.report(DiagnosticCodes.PARAMETER_DUPLICATE, message, owner)
        }
    }
}
