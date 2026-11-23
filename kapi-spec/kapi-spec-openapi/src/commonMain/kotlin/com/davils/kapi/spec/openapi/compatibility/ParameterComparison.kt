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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.openapi.compatibility

import com.davils.kapi.spec.core.collection.associateKeyed
import com.davils.kapi.spec.core.compatibility.Direction
import com.davils.kapi.spec.core.compatibility.Impact
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.parameter.ParameterPlacement
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.resolve
import com.davils.kore.core.annotation.DavilsInternal

private const val SEPARATOR: String = "#"

internal class ParameterComparison(private val context: Context) {
    fun compare(name: String, paths: Both<DocumentComparison.Path>, operations: Both<Operation>) {
        val old = effective(path = paths.before, operation = operations.before, document = context.baseline)
        val new = effective(path = paths.after, operation = operations.after, document = context.candidate)
        old.forEach { (key, parameter) ->
            val found = new[key]
            if (found == null) removed(name, parameter)
            found?.let { candidate -> common(name, Both(before = parameter, after = candidate)) }
        }
        new.forEach { (key, parameter) -> if (key !in old) added(name, parameter) }
    }

    private fun removed(name: String, parameter: Parameter) {
        context.report(
            rule = "parameter-removed",
            impacts = clientBreaking,
            message = "The parameter ${labelOf(parameter)} of $name is removed",
            before = parameter,
            after = null,
        )
    }

    private fun added(name: String, parameter: Parameter) {
        val rule = "parameter-required-added".takeIf { parameter.isRequired } ?: "parameter-added"
        val impacts = clientBreaking.takeIf { parameter.isRequired } ?: neitherBreaking
        val required = " required".takeIf { parameter.isRequired }.orEmpty()
        context.report(
            rule = rule,
            impacts = impacts,
            message = "The$required parameter ${labelOf(parameter)} is added to $name",
            before = null,
            after = parameter,
        )
    }

    private fun common(name: String, parameters: Both<Parameter>) {
        val before = parameters.before
        val after = parameters.after
        val label = "The parameter ${labelOf(after)} of $name"
        val isReshaped = before.style != after.style || before.isExploded != after.isExploded
        if (after.isRequired && !before.isRequired) {
            val message = "$label is required"
            report(parameters = parameters, rule = "parameter-required", impacts = clientBreaking, message = message)
        }
        if (before.isRequired && !after.isRequired) {
            val message = "$label is optional"
            report(parameters = parameters, rule = "parameter-optional", impacts = providerBreaking, message = message)
        }
        if (isReshaped) {
            val message = "$label is serialized differently"
            report(parameters = parameters, rule = "parameter-serialization", impacts = bothBreaking, message = message)
        }
        if (after.isDeprecated && !before.isDeprecated) {
            val message = "$label is deprecated"
            report(parameters = parameters, rule = "parameter-deprecated", impacts = informing, message = message)
        }
        val old = schemaOf(parameter = before, document = context.baseline)
        val new = schemaOf(parameter = after, document = context.candidate)
        context.schemas(before = old, after = new, direction = Direction.Request)
    }

    private fun report(parameters: Both<Parameter>, rule: String, impacts: Pair<Impact, Impact>, message: String) {
        context.report(
            rule = rule,
            impacts = impacts,
            message = message,
            before = parameters.before,
            after = parameters.after,
        )
    }

    private fun effective(
        path: DocumentComparison.Path,
        operation: Operation,
        document: Document,
    ): Map<String, Parameter> {
        val shared = path.item.parameters.mapNotNull { parameter -> document.resolve(parameter) }
        val own = operation.parameters.mapNotNull { parameter -> document.resolve(parameter) }
        val names = path.template.parameters
        return (shared + own).associateKeyed { parameter -> keyOf(names, parameter) to parameter }
    }

    private fun keyOf(names: List<String>, parameter: Parameter): String {
        val placement = parameter.placement
        if (placement == ParameterPlacement.Path) return placement.keyword + SEPARATOR + names.indexOf(parameter.name)
        val name = parameter.name.lowercase().takeIf { placement == ParameterPlacement.Header } ?: parameter.name
        return placement.keyword + SEPARATOR + name
    }

    private fun schemaOf(parameter: Parameter, document: Document): Schema? {
        val content = parameter.content.values.firstOrNull()
        val media = content?.let { referable -> document.resolve<MediaType>(referable) }
        return parameter.schema ?: media?.schema
    }

    private fun labelOf(parameter: Parameter): String = "${shown(parameter.name)} in ${parameter.placement.keyword}"
}
