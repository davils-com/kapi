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
import com.davils.kapi.codegen.core.model.ModelPlanner
import com.davils.kapi.codegen.core.model.PlanScope
import com.davils.kapi.codegen.core.naming.Identifiers
import com.davils.kapi.codegen.core.naming.NameClaim
import com.davils.kapi.codegen.core.naming.NameTable
import com.davils.kapi.codegen.core.operation.plan.BodyPlan
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ParameterPlan
import com.davils.kapi.codegen.core.operation.plan.ResponsePlan
import com.davils.kapi.codegen.core.operation.plan.StreamKind
import com.davils.kapi.codegen.core.options.GeneratorOptions
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.value.ExtensionName
import com.davils.kapi.spec.core.value.ExtensionReading
import com.davils.kapi.spec.openapi.model.path.Method
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.model.path.PathTemplate

private const val DEFAULT_GROUP: String = "Default"

private const val PAGES: String = "Pages"

private const val STREAM: String = "Stream"

internal class OperationPlanner(
    private val scope: PlanScope,
    options: GeneratorOptions,
    models: ModelPlanner,
) {
    private val parameters: ParameterPlanner = ParameterPlanner(scope, models)

    private val responses: ResponsePlanner = ResponsePlanner(scope, models)

    private val bodies: BodyPlanner = BodyPlanner(scope, models)

    private val pages: PagePlanner = PagePlanner(scope)

    private val security: SecurityPlanner = SecurityPlanner(scope)

    private val results: NameTable = NameTable(scope.findings)

    private val functions: MutableMap<String, NameTable> = mutableMapOf()

    private val prefix: String = options.typePrefix + options.apiName

    fun plan(): List<OperationPlan> {
        val paths = scope.document.paths
        val items = paths?.items.orEmpty()
        val sorted = items.entries.sortedBy { (template, _) -> template.value }
        return sorted.flatMap { (template, item) -> operations(template, item) }
    }

    fun nameOf(method: Method, path: String, operation: Operation): String {
        val declared = declaredName(operation) ?: method.keyword + " " + path
        return Identifiers.member(declared)
    }

    fun declaredName(operation: Operation): String? = text(operation, Keywords.NAME) ?: operation.operationId

    fun isIgnored(operation: Operation): Boolean {
        val reading = operation.extensions.flag(Keywords.IGNORE) as? ExtensionReading.Present
        return reading?.value == true
    }

    fun planned(site: OperationSite): OperationPlan {
        val operation = site.operation
        val context = prefix + Identifiers.type(site.name)
        val planned = parameters.plan(context, site.item, operation)
        val body = bodies.plan(context, operation)
        val answers = responses.plan(context, operation)
        val paged = pages.plan(operation, planned, answers)
        claim(group = site.group, name = site.name, operation = operation)
        if (paged != null) claimPages(group = site.group, name = site.name, operation = operation)
        val isStreamed = answers.any { answer -> answer.stream != null }
        if (isStreamed) claimStream(group = site.group, name = site.name, operation = operation)
        claim(operation, planned, body)
        val upgrade = text(operation, Keywords.UPGRADE)
        if (upgrade != null) auditUpgrade(operation, planned, answers)
        return OperationPlan(
            name = site.name,
            group = site.group,
            result = context + "Result",
            method = site.method.keyword.uppercase(),
            path = site.path,
            parameters = planned,
            body = body,
            responses = answers,
            security = security.plan(operation, isInherited = !site.isDelivery),
            delivery = DeliveryPlanner.plan(site.method, operation, site.item),
            pages = paged,
            origin = operation.location,
            isDelivery = site.isDelivery,
            upgrade = upgrade,
            summary = operation.summary,
            description = operation.description,
        )
    }

    private fun auditUpgrade(operation: Operation, parameters: List<ParameterPlan>, responses: List<ResponsePlan>) {
        val isStreamed = responses.any { response -> response.stream?.kind == StreamKind.Bytes }
        if (!isStreamed) {
            scope.findings.error(
                CodegenCodes.EXTENSION_VALUE,
                "${Keywords.UPGRADE} needs a response whose bytes are streamed",
                operation.location,
            )
        }
        if (parameters.any { parameter -> parameter.name == OperationPlan.INPUT }) {
            scope.findings.error(
                CodegenCodes.NAME_COLLISION,
                "A parameter becomes ${OperationPlan.INPUT}, which an upgraded operation writes after the switch; " +
                    "name it with ${Keywords.NAME}",
                operation.location,
            )
        }
    }

    private fun operations(template: PathTemplate, item: PathItem): List<OperationPlan> {
        val kept = item.operations.filterValues { operation -> !isIgnored(operation) }
        val sorted = kept.entries.sortedBy { (method, _) -> method.ordinal }
        return sorted.map { (method, operation) ->
            val site = OperationSite(
                name = nameOf(method, template.value, operation),
                group = groupOf(item, operation),
                path = template.value,
                method = method,
                item = item,
                operation = operation,
            )
            planned(site)
        }
    }

    private fun groupOf(item: PathItem, operation: Operation): String {
        val declared = text(operation, Keywords.GROUP) ?: text(item, Keywords.GROUP)
        val group = declared ?: operation.tags.firstOrNull() ?: DEFAULT_GROUP
        return prefix + Identifiers.type(group)
    }

    private fun claim(group: String, name: String, operation: Operation) {
        val claim = NameClaim(name = name, origin = "The operation $name", location = operation.location)
        functions.getOrPut(group) { NameTable(scope.findings) }.claim(claim)
        results.claim(claim)
    }

    private fun claimStream(group: String, name: String, operation: Operation) {
        val origin = "The stream of the operation $name"
        val claim = NameClaim(name = name + STREAM, origin = origin, location = operation.location)
        functions.getOrPut(group) { NameTable(scope.findings) }.claim(claim)
    }

    private fun claimPages(group: String, name: String, operation: Operation) {
        val origin = "The pages of the operation $name"
        val claim = NameClaim(name = name + PAGES, origin = origin, location = operation.location)
        functions.getOrPut(group) { NameTable(scope.findings) }.claim(claim)
    }

    private fun claim(operation: Operation, parameters: List<ParameterPlan>, body: BodyPlan?) {
        val names = NameTable(scope.findings)
        parameters.forEach { plan ->
            val origin = "The parameter ${plan.wireName}"
            names.claim(NameClaim(name = plan.name, origin = origin, location = operation.location))
        }
        body?.fields.orEmpty().forEach { field ->
            val origin = "The field ${field.wireName}"
            names.claim(NameClaim(name = field.name, origin = origin, location = operation.location))
        }
    }

    private fun text(element: Element, keyword: ExtensionName): String? {
        val reading = element.extensions.text(keyword) as? ExtensionReading.Present
        return reading?.value
    }
}
