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

import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ResponsePlan
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.joinToCode

private const val EXCHANGE: String = "exchange"

private const val READERS: String = "Readers"

private const val RANGES: String = "Ranges"

private const val RANGE_SIZE: Int = 100

internal class AnswerCode(private val serializers: SerializerCode, private val results: (OperationPlan) -> ClassName) {
    fun properties(operation: OperationPlan): List<PropertySpec> {
        val exact = operation.responses.mapNotNull { response -> response.status?.let { status -> status to response } }
        val ranges = operation.responses.mapNotNull { response -> response.range?.let { range -> range to response } }
        val readers = table(operation, READERS, exact)
        if (ranges.isEmpty()) return listOf(readers)
        return listOf(readers, table(operation, RANGES, ranges))
    }

    fun function(operation: OperationPlan, result: ClassName): FunSpec {
        val function = FunSpec.builder(operation.name + RESULT)
        function.addModifiers(KModifier.PRIVATE)
        function.addParameter(EXCHANGE, ClientTypes.answered)
        function.returns(result)
        function.addCode("val read = %L\n", lookup(operation))
        function.addCode("if (read != null) return read(%N)\n", EXCHANGE)
        function.addCode("return %L\n", fallback(operation, result))
        return function.build()
    }

    fun readers(operation: OperationPlan, result: ClassName): List<FunSpec> {
        val typed = operation.responses.filter { response -> response.headers.isNotEmpty() }
        return typed.map { response -> reader(operation, result, response) }
    }

    private fun reader(operation: OperationPlan, result: ClassName, response: ResponsePlan): FunSpec {
        val headers = HeaderCode(result)
        val function = FunSpec.builder(operation.name + response.name)
        function.addModifiers(KModifier.PRIVATE)
        function.addParameter(EXCHANGE, ClientTypes.answered)
        function.returns(result)
        response.headers.forEach { header -> function.addCode(headers.read(header)) }
        function.addCode("return %L\n", reading(result, response))
        return function.build()
    }

    private fun lookup(operation: OperationPlan): CodeBlock {
        val exact = CodeBlock.of("%N[%N.status]", operation.name + READERS, EXCHANGE)
        if (operation.responses.all { response -> response.range == null }) return exact
        return CodeBlock.of("%L ?: %N[%N.status / %L]", exact, operation.name + RANGES, EXCHANGE, RANGE_SIZE)
    }

    private fun fallback(operation: OperationPlan, result: ClassName): CodeBlock {
        val response = operation.responses.firstOrNull { plan -> plan.status == null && plan.range == null }
            ?: return CodeBlock.of(
                "%T.%L(%N.status, %N.headers, %N.content)",
                result,
                ResultEmitter.UNEXPECTED,
                EXCHANGE,
                EXCHANGE,
                EXCHANGE,
            )
        return read(operation, result, response)
    }

    private fun read(operation: OperationPlan, result: ClassName, response: ResponsePlan): CodeBlock {
        if (response.headers.isEmpty()) return reading(result, response)
        return CodeBlock.of("%N(%N)", operation.name + response.name, EXCHANGE)
    }

    private fun table(operation: OperationPlan, suffix: String, entries: List<Pair<Int, ResponsePlan>>): PropertySpec {
        val result = results(operation)
        val name = operation.name + suffix
        val reader = LambdaTypeName.get(parameters = arrayOf(ClientTypes.answered), returnType = result)
        val code = CodeBlock.builder()
        entries.forEach { (key, response) -> code.add("%L to %L,\n", key, entry(operation, result, response)) }
        val property = PropertySpec.builder(name, MAP.parameterizedBy(INT, reader), KModifier.PRIVATE)
        property.initializer("mapOf(\n⇤%L⇤)⇥⇥", code.build())
        return property.build()
    }

    private fun entry(operation: OperationPlan, result: ClassName, response: ResponsePlan): CodeBlock {
        if (response.headers.isNotEmpty()) return CodeBlock.of("::%N", operation.name + response.name)
        return lambda(result, response)
    }

    private fun lambda(result: ClassName, response: ResponsePlan): CodeBlock {
        if (response.body == null) return CodeBlock.of("{ %N -> %L }", EXCHANGE, reading(result, response))
        return CodeBlock.of("{ %N ->\n⇥%L\n⇤}", EXCHANGE, reading(result, response))
    }

    private fun reading(result: ClassName, response: ResponsePlan): CodeBlock {
        val status = CodeBlock.of("%N.status, ", EXCHANGE).takeIf { response.status == null }
        val variant = CodeBlock.of("%T.%L(%L", result, response.name, status ?: "")
        val typed = response.headers.map { header -> CodeBlock.of("%N, ", header.name) }
        val tail = CodeBlock.of("%L%N.headers)", typed.joinToCode(""), EXCHANGE)
        val body = response.body ?: return CodeBlock.of("%L%N.content, %L", variant, EXCHANGE, tail)
        val malformed = CodeBlock.of("%T.%L(%N.status, decoding.reason)", result, ResultEmitter.MALFORMED, EXCHANGE)
        val code = CodeBlock.builder()
        code.add("when (val decoding = client.decode(%L, %N)) {\n", serializers.of(body), EXCHANGE)
        code.add("⇥is %T.Decoded -> %Ldecoding.value, %L\n", ClientTypes.decoding, variant, tail)
        code.add("is %T.Failed -> %L\n⇤}", ClientTypes.decoding, malformed)
        return code.build()
    }

    companion object {
        const val RESULT: String = "Result"
    }
}
