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
import com.davils.kapi.spec.openapi.model.path.PathTemplate
import com.davils.kapi.spec.openapi.resolve

private const val PLACEHOLDER: String = "{}"

internal object PathRules {
    private val parameter: Regex = Regex("""\{[^{}/]+}""")

    private val brace: Regex = Regex("[{}]")

    fun check(scope: RuleScope) {
        val paths = scope.document.paths
        val items = paths?.items.orEmpty()
        items.forEach { (template, item) -> path(scope, template, item) }
        val alike = items.entries.groupBy { (template, _) ->
            parameter.replace(
                input = template.value,
                replacement = PLACEHOLDER,
            )
        }
        val ambiguous = alike.values.flatMap { same -> same.drop(1) }
        ambiguous.forEach { (template, item) ->
            val message = "The path $template differs from another path only by the names of its parameters"
            scope.report(DiagnosticCodes.PATH_AMBIGUOUS, message, item)
        }
    }

    private fun path(scope: RuleScope, template: PathTemplate, item: PathItem) {
        val names = template.parameters
        val rest = parameter.replace(input = template.value, replacement = "")
        val isBroken = brace.containsMatchIn(rest) || names.distinct().size < names.size
        if (isBroken) {
            val message = "The path $template must name every parameter once between a pair of braces"
            return scope.report(DiagnosticCodes.PATH_TEMPLATE, message, item)
        }
        val shared = declared(scope, item.parameters)
        val operations = item.operations.values + item.additionalOperations.values
        operations.forEach { operation ->
            operation(scope = scope, template = template, operation = operation, shared = shared)
        }
    }

    private fun operation(scope: RuleScope, template: PathTemplate, operation: Operation, shared: Set<String>) {
        val declared = shared + declared(scope, operation.parameters)
        val undeclared = template.parameters.filterNot { name -> name in declared }
        undeclared.forEach { name ->
            val message = "The path $template names the parameter $name, which the operation does not declare"
            scope.report(DiagnosticCodes.PATH_PARAMETER_UNDECLARED, message, operation)
        }
        val unused = declared.filterNot { name -> name in template.parameters }
        unused.forEach { name ->
            val message = "The operation declares the path parameter $name, which the path $template does not name"
            scope.report(DiagnosticCodes.PATH_PARAMETER_UNUSED, message, operation)
        }
    }

    private fun declared(scope: RuleScope, parameters: List<Referable<Parameter>>): Set<String> {
        val resolved = parameters.mapNotNull { parameter -> scope.document.resolve(parameter) }
        val inPath = resolved.filter { parameter -> parameter.placement == ParameterPlacement.Path }
        return inPath.map(Parameter::name).toSet()
    }
}
