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


package com.davils.kapi.codegen.core.emit.client

import com.davils.kapi.codegen.core.ModelKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.operation.plan.DeliveryPlan
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ParameterPlace
import com.davils.kapi.codegen.core.operation.plan.ParameterPlan
import com.davils.kapi.codegen.core.operation.plan.ResponsePlan
import com.davils.kapi.codegen.core.operation.plan.SecurityPlan
import com.davils.kapi.codegen.core.operation.plan.TimeoutPlan
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.joinToCode

private val template: Regex = Regex("""\{([^{}/]+)}""")

internal class CallCode(private val serializers: SerializerCode) {
    private val bodies: BodyCode = BodyCode(serializers)

    fun of(operation: OperationPlan, isStreamed: Boolean = false): CodeBlock {
        val accepted = operation.responses.flatMap(ResponsePlan::accept).distinct()
        val code = CodeBlock.builder()
        code.add("val call = %M(%S, %S) {\n", ClientTypes.apiCall, operation.name, operation.method)
        code.indent()
        code.add(path(operation))
        operation.parameters.forEach { parameter -> code.add(placed(parameter)) }
        code.add(delivery(operation.delivery))
        accepted.sorted().forEach { type -> code.add("accept(%S)\n", type) }
        operation.body?.let { body -> code.add(bodies.of(body)) }
        operation.upgrade?.takeIf { isStreamed }?.let { protocol -> code.add("upgrade(%S, %N)\n", protocol, INPUT) }
        operation.security.forEach { alternative -> code.add(security(alternative)) }
        code.unindent()
        code.add("}\n")
        return code.build()
    }

    private fun path(operation: OperationPlan): CodeBlock {
        if (operation.isDelivery) return CodeBlock.of("destination(%N)\n", DESTINATION)
        val code = CodeBlock.builder()
        val matches = template.findAll(operation.path).toList()
        val names = matches.map { match -> match.groupValues[1] }
        val literals = operation.path.split(template)
        literals.forEachIndexed { index, literal ->
            if (literal.isNotEmpty()) code.add("path(%S)\n", literal)
            val segment = operation.parameters.firstOrNull { plan -> isSegment(plan, names.getOrNull(index)) }
            if (segment != null) code.add("path(%L, %L)\n", format(segment), value(segment))
        }
        return code.build()
    }

    private fun isSegment(plan: ParameterPlan, name: String?): Boolean =
        plan.placement == ParameterPlace.Path && plan.wireName == name

    private fun placed(parameter: ParameterPlan): CodeBlock {
        if (parameter.placement == ParameterPlace.Path) return CodeBlock.of("")
        val guard = CodeBlock.of("if (%N != null) ", parameter.name).takeIf { parameter.type.isNullable }
        val function = parameter.placement.function
        return CodeBlock.of("%L%L(%L, %L)\n", guard ?: "", function, format(parameter), value(parameter))
    }

    private fun format(parameter: ParameterPlan): CodeBlock = CodeBlock.of(
        "%T(%S, %T.%L, %L)",
        ClientTypes.format,
        parameter.wireName,
        ClientTypes.style,
        parameter.style,
        parameter.isExploded,
    )

    private fun value(parameter: ParameterPlan): CodeBlock {
        val shape = parameter.type.shape
        val items = shape as? TypeShape.Items ?: return single(parameter)
        return CodeBlock.of("%T.Items(%L)", ClientTypes.value, ValueCode.texts(items, parameter.name))
    }

    private fun single(parameter: ParameterPlan): CodeBlock {
        val shape = parameter.type.shape
        val isObject = shape is TypeShape.Entries || (shape as? TypeShape.Named)?.kind == ModelKind.Object
        if (!isObject) return CodeBlock.of("%T.Text(%L)", ClientTypes.value, ValueCode.text(shape, parameter.name))
        val serializer = serializers.of(parameter.type.copy(isNullable = false))
        return CodeBlock.of("client.fields(%L, %N)", serializer, parameter.name)
    }

    private fun delivery(plan: DeliveryPlan): CodeBlock {
        val code = CodeBlock.builder()
        plan.timeout?.let { timeout -> code.add("timeout(%L)\n", limits(timeout)) }
        if (plan.isIdempotent) code.add("idempotent()\n")
        return code.build()
    }

    private fun limits(plan: TimeoutPlan): CodeBlock {
        val parts = listOf("request" to plan.request, "connect" to plan.connect, "socket" to plan.socket)
        val set = parts.mapNotNull { (name, millis) ->
            millis?.let { value -> CodeBlock.of("%L = %L.%M", name, value, ClientTypes.milliseconds) }
        }
        return set.joinToCode(", ")
    }

    private fun security(alternative: List<SecurityPlan>): CodeBlock {
        val presented = alternative.filterNot(SecurityPlan::isOfConnection)
        val uses = presented.map { use ->
            CodeBlock.of("%T(%S, %L)", ClientTypes.securityUse, use.scheme, placement(use))
        }
        return CodeBlock.of("security(listOf(%L))\n", uses.joinToCode(", "))
    }

    private fun placement(use: SecurityPlan): CodeBlock {
        val name = use.name ?: return CodeBlock.of("%T.%L", ClientTypes.placement, use.placement)
        return CodeBlock.of("%T.%L(%S)", ClientTypes.placement, use.placement, name)
    }

    companion object {
        const val DESTINATION: String = "destination"

        const val INPUT: String = OperationPlan.INPUT
    }
}
