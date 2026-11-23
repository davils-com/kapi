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

package com.davils.kapi.codegen.core.operation

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.emit.client.SignatureNames
import com.davils.kapi.codegen.core.model.PlanScope
import com.davils.kapi.codegen.core.naming.Identifiers
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ParameterPlace
import com.davils.kapi.spec.openapi.model.path.Callback
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.resolve

private const val WEBHOOKS: String = "Webhooks"
private const val CALLBACKS: String = "Callbacks"
private const val SEPARATOR: String = " "

internal class HookPlanner(
    private val scope: PlanScope,
    private val planner: OperationPlanner,
    private val prefix: String,
) {
    fun plan(): List<OperationPlan> {
        val plans = webhooks() + callbacks()
        plans.forEach(::audit)
        return plans
    }

    private fun webhooks(): List<OperationPlan> {
        val sorted = scope.document.webhooks.entries
            .sortedBy { (key, _) -> key }
        return sorted.flatMap { (key, item) -> deliveries(item = item, group = prefix + WEBHOOKS, label = key) }
    }

    private fun callbacks(): List<OperationPlan> {
        val paths = scope.document.paths
        val items = paths?.items.orEmpty()
        val sorted = items.entries.sortedBy { (template, _) -> template.value }
        return sorted.flatMap { (template, item) ->
            val kept = item.operations.filterValues { operation -> !planner.isIgnored(operation) }
            val ordered = kept.entries.sortedBy { (method, _) -> method.ordinal }
            ordered.flatMap { (method, operation) ->
                callbacksOf(planner.nameOf(method, template.value, operation), operation)
            }
        }
    }

    private fun callbacksOf(parent: String, operation: Operation): List<OperationPlan> {
        val sorted = operation.callbacks.entries.sortedBy { (key, _) -> key }
        return sorted.flatMap { (key, referable) ->
            val resolved = listOfNotNull(scope.document.resolve(referable))
            resolved.flatMap { callback -> expressions(parent + SEPARATOR + key, callback) }
        }
    }

    private fun expressions(label: String, callback: Callback): List<OperationPlan> {
        if (callback.expressions.size > 1) {
            scope.findings.error(
                CodegenCodes.OPERATION_UNSUPPORTED,
                "The generator has no Kotlin for a callback with more than one expression yet",
                callback.location,
            )
        }
        val items = callback.expressions.values.take(1)
        return items.flatMap { item -> deliveries(item, prefix + CALLBACKS, label) }
    }

    private fun deliveries(item: PathItem, group: String, label: String): List<OperationPlan> {
        val kept = item.operations.filterValues { operation -> !planner.isIgnored(operation) }
        val sorted = kept.entries.sortedBy { (method, _) -> method.ordinal }
        return sorted.map { (method, operation) ->
            val declared = planner.declaredName(operation) ?: label + SEPARATOR + method.keyword
            val site = OperationSite(
                name = Identifiers.member(declared),
                group = group,
                path = "/" + Identifiers.member(label),
                method = method,
                item = item,
                operation = operation,
                isDelivery = true,
            )
            planner.planned(site)
        }
    }

    private fun audit(plan: OperationPlan) {
        val segments = plan.parameters.filter { parameter -> parameter.placement == ParameterPlace.Path }
        val paths = segments.map { parameter -> "the path parameter ${parameter.wireName} of a webhook or a callback" }
        val pages = listOfNotNull(plan.pages).map { "pagination on a webhook or a callback" }
        val streamed = plan.responses.filter { response -> response.stream != null }
        val streams = streamed.map { "a streamed response of a webhook or a callback" }
        (paths + pages + streams).forEach { what ->
            scope.findings.error(
                CodegenCodes.OPERATION_UNSUPPORTED,
                "The generator has no Kotlin for $what yet",
                plan.origin,
            )
        }
        val taken = SignatureNames.of(plan).filter { name -> name == DESTINATION }
        taken.forEach { name ->
            scope.findings.error(
                CodegenCodes.NAME_COLLISION,
                "A parameter of the delivery ${plan.name} becomes $name, which the generated sender uses itself; " +
                    "name it with ${Keywords.NAME}",
                plan.origin,
            )
        }
    }

    companion object {
        const val DESTINATION: String = "destination"
    }
}
