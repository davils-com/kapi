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
import com.davils.kapi.codegen.core.operation.plan.HeaderPlan
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ResponsePlan
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock

private const val RESPONSE: String = "response"

internal class ReplyCode(private val serializers: SerializerCode) {
    fun of(operation: OperationPlan, response: ClassName, arguments: CodeBlock): CodeBlock {
        val code = CodeBlock.builder()
        code.add("return when (val %N = service.%N(%L)) {\n", RESPONSE, operation.name, arguments)
        code.indent()
        operation.responses.forEach { plan -> code.add("is %T.%L -> %L\n", response, plan.name, reply(plan)) }
        code.unindent()
        code.add("}\n")
        return code.build()
    }

    private fun reply(plan: ResponsePlan): CodeBlock {
        val status = statusOf(plan)
        val parts = plan.headers.map(::header) + content(plan)
        if (parts.isEmpty()) return CodeBlock.of("server.reply(%L)", status)
        val code = CodeBlock.builder()
        code.add("server.reply(%L) {\n", status)
        code.indent()
        parts.forEach { part -> code.add("%L\n", part) }
        code.unindent()
        code.add("}")
        return code.build()
    }

    private fun statusOf(plan: ResponsePlan): CodeBlock {
        val code = plan.status ?: return CodeBlock.of("%N.status", RESPONSE)
        return CodeBlock.of("%L", code)
    }

    private fun header(header: HeaderPlan): CodeBlock {
        if (header.reader == HeaderPlan.TEXT) {
            return CodeBlock.of(
                "header(%S, %N.%N)",
                header.wireName,
                RESPONSE,
                header.name,
            )
        }
        val access = "?.".takeIf { header.type.isNullable } ?: "."
        return CodeBlock.of("header(%S, %N.%N%LtoString())", header.wireName, RESPONSE, header.name, access)
    }

    private fun content(plan: ResponsePlan): List<CodeBlock> {
        val types = plan.accept.take(1)
        return types.map { type -> body(plan, type) }
    }

    private fun body(plan: ResponsePlan, type: String): CodeBlock {
        val body = plan.body ?: return CodeBlock.of("bytes(%S, %N.content)", type, RESPONSE)
        return CodeBlock.of("json(%S, %L, %N.body)", type, serializers.of(body), RESPONSE)
    }
}
