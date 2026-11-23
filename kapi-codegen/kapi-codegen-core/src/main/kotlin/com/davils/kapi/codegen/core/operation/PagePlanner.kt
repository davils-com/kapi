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
import com.davils.kapi.codegen.core.model.PlanScope
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.PropertyPlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.operation.plan.PagePlan
import com.davils.kapi.codegen.core.operation.plan.PageStyle
import com.davils.kapi.codegen.core.operation.plan.ParameterPlace
import com.davils.kapi.codegen.core.operation.plan.ParameterPlan
import com.davils.kapi.codegen.core.operation.plan.ResponsePlan
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.path.Operation

private const val STYLE: String = "style"

private const val PARAMETER: String = "parameter"

private val successes: IntRange = 200..299

private val styles: Map<String, PageStyle> = mapOf(
    "cursor" to PageStyle.Cursor,
    "offset" to PageStyle.Offset,
    "page" to PageStyle.Numbered,
)

private val positions: Map<PageStyle, Set<ScalarKind>> = mapOf(
    PageStyle.Cursor to setOf(ScalarKind.Text),
    PageStyle.Offset to setOf(ScalarKind.Int, ScalarKind.Long),
    PageStyle.Numbered to setOf(ScalarKind.Int, ScalarKind.Long),
)

private val keys: Map<PageStyle, String> = mapOf(
    PageStyle.Cursor to "next",
    PageStyle.Offset to "items",
    PageStyle.Numbered to "items",
)

internal class PagePlanner(private val scope: PlanScope) {
    fun plan(operation: Operation, parameters: List<ParameterPlan>, responses: List<ResponsePlan>): PagePlan? {
        val declared = listOfNotNull(operation.extensions[Keywords.PAGINATION] as? Value.Mapping)
        val sites = declared.map { mapping ->
            PageSite(
                operation = operation,
                parameters = parameters,
                responses = responses,
                settings = textsOf(mapping),
            )
        }
        return sites.firstNotNullOfOrNull(::plan)
    }

    private fun textsOf(mapping: Value.Mapping): Map<String, String> {
        val texts = mapping.entries.mapValues { (_, value) -> value as? Value.Text }
        return texts.mapValues { (_, text) -> text?.value.orEmpty() }
    }

    private fun plan(site: PageSite): PagePlan? {
        val style = styles[site.settings[STYLE]] ?: return pending(site)
        val parameter = parameterOf(site, style)
        val answer = site.responses.firstOrNull { response -> response.status in successes && response.body != null }
        return PagePlan(
            style = style,
            parameter = parameter?.name.orEmpty(),
            isNarrow = kindOf(parameter) == ScalarKind.Int,
            answer = answer?.name.orEmpty(),
            property = propertyOf(site, style, answer?.body?.shape),
        )
    }

    private fun pending(site: PageSite): PagePlan? {
        scope.findings.warn(
            CodegenCodes.EXTENSION_PENDING,
            "${Keywords.PAGINATION} has no effect on the generated client yet for the style " +
                site.settings[STYLE],
            site.operation.location,
        )
        return null
    }

    private fun parameterOf(site: PageSite, style: PageStyle): ParameterPlan? {
        val name = site.settings[PARAMETER]
        val queries = site.parameters.filter { plan -> plan.placement == ParameterPlace.Query }
        val parameter = queries.firstOrNull { plan -> plan.wireName == name }
        if (kindOf(parameter) !in positions.getValue(style)) {
            refuse(site, "needs parameter to name a query parameter that is ${positions.getValue(style)}")
        }
        return parameter
    }

    private fun kindOf(parameter: ParameterPlan?): ScalarKind? {
        val scalar = parameter?.run { type.shape } as? TypeShape.Scalar
        return scalar?.kind
    }

    private fun propertyOf(site: PageSite, style: PageStyle, body: TypeShape?): String? {
        val key = site.settings[keys.getValue(style)]
        val property = propertiesOf(body).firstOrNull { candidate -> candidate.jsonName == key }
        val paged = property?.run { type.shape } ?: body.takeIf { key == null }
        if (!holds(style, paged)) {
            refuse(site, "needs ${keys.getValue(style)} to name what a successful JSON response pages by")
        }
        return property?.name
    }

    private fun holds(style: PageStyle, shape: TypeShape?): Boolean {
        val isCursor = (shape as? TypeShape.Scalar)?.kind == ScalarKind.Text
        val isList = shape is TypeShape.Items
        return isCursor.takeIf { style == PageStyle.Cursor } ?: isList
    }

    private fun propertiesOf(shape: TypeShape?): List<PropertyPlan> {
        val named = shape as? TypeShape.Named
        val objects = scope.plans.filterIsInstance<ModelPlan.ObjectPlan>()
        return objects.filter { plan -> plan.name == named?.name }.flatMap(ModelPlan.ObjectPlan::properties)
    }

    private fun refuse(site: PageSite, what: String) {
        scope.findings.error(
            CodegenCodes.PAGINATION_INVALID,
            "${Keywords.PAGINATION} with the style ${site.settings[STYLE]} $what",
            site.operation.location,
        )
    }
}
