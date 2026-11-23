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


package com.davils.kapi.codegen.core.emit.server

import com.davils.kapi.codegen.core.emit.client.SerializerCode
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.operation.plan.BodyKind
import com.davils.kapi.codegen.core.operation.plan.BodyPlan
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ParameterPlan
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.joinToCode

private const val BODY: String = "body"

internal class InputCode(private val serializers: SerializerCode) {
    fun of(operation: OperationPlan): CodeBlock {
        val code = CodeBlock.builder()
        code.add("val input = server.input(request)\n")
        operation.parameters.forEach { parameter -> code.add("val %N = %L\n", parameter.name, read(parameter)) }
        operation.body?.let { body -> code.add("val %N = %L\n", BODY, read(body)) }
        code.add("if (%L) return input.refused()\n", refusal(operation))
        return code.build()
    }

    private fun refusal(operation: OperationPlan): CodeBlock {
        val parameters = operation.parameters.filter { parameter -> !parameter.type.isNullable }
        val bodies = listOfNotNull(operation.body).filter { body -> !body.type.isNullable }
        val required = parameters.map(ParameterPlan::name) + bodies.map { BODY }
        val checks = listOf(CodeBlock.of("!input.isValid")) + required.map { name -> CodeBlock.of("%N == null", name) }
        return checks.joinToCode(" || ")
    }

    private fun read(parameter: ParameterPlan): CodeBlock {
        val items = parameter.type.shape as? TypeShape.Items ?: return read("value", parameter, parameter.type)
        return read("items", parameter, items.item)
    }

    private fun read(function: String, parameter: ParameterPlan, single: TypeUse): CodeBlock = CodeBlock.of(
        "input.%L(%T.%L, %L, %T(%L, %L), %L)",
        function,
        ServerTypes.place,
        parameter.placement.name,
        format(parameter),
        ServerTypes.text,
        serializers.of(single.copy(isNullable = false)),
        parameter.isQuoted,
        !parameter.type.isNullable,
    )

    private fun format(parameter: ParameterPlan): CodeBlock = CodeBlock.of(
        "%T(%S, %T.%L, %L)",
        ServerTypes.format,
        parameter.wireName,
        ServerTypes.style,
        parameter.style,
        parameter.isExploded,
    )

    private fun read(body: BodyPlan): CodeBlock {
        val isRequired = !body.type.isNullable
        if (body.kind != BodyKind.Json) return CodeBlock.of("input.bytes(%L)", isRequired)
        return CodeBlock.of("input.body(%L, %L)", serializers.of(body.type.copy(isNullable = false)), isRequired)
    }
}
