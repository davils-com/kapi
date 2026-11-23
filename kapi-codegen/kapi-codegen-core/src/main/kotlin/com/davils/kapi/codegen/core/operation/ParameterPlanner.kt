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

import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.parameter.ParameterPlacement
import com.davils.kapi.spec.openapi.model.parameter.ParameterStyle
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.core.value.ExtensionReading
import com.davils.kapi.spec.openapi.resolve
import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.model.ModelPlanner
import com.davils.kapi.codegen.core.model.PlanScope
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.naming.Identifiers
import com.davils.kapi.codegen.core.operation.plan.ParameterPlace
import com.davils.kapi.codegen.core.operation.plan.ParameterPlan

private val places: Map<ParameterPlacement, ParameterPlace> = mapOf(
    ParameterPlacement.Path to ParameterPlace.Path,
    ParameterPlacement.Query to ParameterPlace.Query,
    ParameterPlacement.Header to ParameterPlace.Header,
    ParameterPlacement.Cookie to ParameterPlace.Cookie,
)

private val owned: Set<String> = setOf("accept", "content-type", "authorization")

internal class ParameterPlanner(private val scope: PlanScope, private val models: ModelPlanner) {
    private val types: ParameterTypes = ParameterTypes(scope)

    private val names: ClientNames = ClientNames(scope.findings)

    fun plan(context: String, item: PathItem, operation: Operation): List<ParameterPlan> {
        val declared = item.parameters + operation.parameters
        val resolved = declared.mapNotNull { referable -> scope.document.resolve(referable) }
        val effective = resolved.associateBy { parameter -> parameter.placement to parameter.name }
        val (whole, single) = effective.values.partition { parameter ->
            parameter.placement == ParameterPlacement.QueryString
        }
        whole.forEach(::refuse)
        val (ignored, kept) = single.partition(::isOwned)
        ignored.forEach(::ignore)
        return kept.map { parameter -> parameter(context, parameter) }
    }

    private fun isOwned(parameter: Parameter): Boolean =
        parameter.placement == ParameterPlacement.Header && parameter.name.lowercase() in owned

    private fun refuse(parameter: Parameter) {
        scope.findings.error(
            CodegenCodes.OPERATION_UNSUPPORTED,
            "The parameter ${parameter.name} takes the whole query string, which the generator does not support yet",
            parameter.location,
        )
    }

    private fun ignore(parameter: Parameter) {
        scope.findings.warn(
            CodegenCodes.PARAMETER_RESERVED_HEADER,
            "The generated client sets the header ${parameter.name} itself and takes no parameter for it",
            parameter.location,
        )
    }

    private fun parameter(context: String, parameter: Parameter): ParameterPlan {
        val name = nameOf(parameter)
        val style = parameter.style ?: ParameterStyle.defaultFor(parameter.placement) ?: ParameterStyle.Simple
        val type = typeOf(context + Identifiers.type(name), parameter)
        names.audit(name, "The parameter ${parameter.name}", parameter.location)
        return ParameterPlan(
            name = name,
            wireName = parameter.name,
            placement = places.getValue(parameter.placement),
            style = style.name,
            isExploded = parameter.isExploded ?: (style == ParameterStyle.Form),
            type = type,
            isQuoted = types.isQuoted(type.shape),
        )
    }

    private fun nameOf(parameter: Parameter): String {
        val declared = parameter.extensions.text(Keywords.NAME) as? ExtensionReading.Present
            ?: return Identifiers.member(parameter.name)
        return declared.value
    }

    private fun typeOf(context: String, parameter: Parameter): TypeUse {
        val use = parameter.schema?.let { schema -> models.typeOf(schema, context) }
        val supported = use?.takeIf { type -> isSupported(type.shape) }
        if (supported == null) {
            scope.findings.error(
                CodegenCodes.OPERATION_UNSUPPORTED,
                "The generator has no Kotlin for the parameter ${parameter.name} yet, which is no text, list or " +
                    "flat object",
                parameter.location,
            )
        }
        val type = supported ?: TypeUse(TypeShape.Scalar(ScalarKind.Text))
        return type.copy(isNullable = type.isNullable || !parameter.isRequired)
    }

    private fun isSupported(shape: TypeShape): Boolean =
        types.isText(shape) || types.isTexts(shape) || types.isFields(shape)
}
