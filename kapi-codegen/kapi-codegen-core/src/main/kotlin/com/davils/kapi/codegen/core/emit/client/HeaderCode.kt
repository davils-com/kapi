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

import com.davils.kapi.codegen.core.operation.plan.HeaderPlan
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName

private const val RESPONSE: String = "com.davils.kapi.runtime.core.response"

private val kinds: Map<String, String> = mapOf(
    "int" to "a whole number",
    "long" to "a whole number",
    "double" to "a number",
    "float" to "a number",
    "flag" to "true or false",
)

internal class HeaderCode(private val result: ClassName) {
    fun read(header: HeaderPlan): CodeBlock {
        val code = CodeBlock.builder()
        val failure = CodeBlock.of("%T.%L(exchange.status, %S)", result, ResultEmitter.MALFORMED, problemOf(header))
        if (header.type.isNullable) {
            code.add("val %N = %L\n", header.name, value(header))
            code.add(guard(header, failure))
            return code.build()
        }
        code.add("val %N = %L\n⇥?: return %L\n⇤", header.name, value(header), failure)
        return code.build()
    }

    private fun guard(header: HeaderPlan, failure: CodeBlock): CodeBlock {
        if (header.reader == HeaderPlan.TEXT) return CodeBlock.of("")
        val broken = CodeBlock.of("%N == null && %S in exchange.headers", header.name, header.wireName)
        return CodeBlock.of("if (%L) return %L\n", broken, failure)
    }

    private fun value(header: HeaderPlan): CodeBlock {
        if (header.reader == HeaderPlan.TEXT) return CodeBlock.of("exchange.headers[%S]", header.wireName)
        val reader = MemberName(RESPONSE, header.reader, isExtension = true)
        return CodeBlock.of("exchange.headers.%M(%S)", reader, header.wireName)
    }

    private fun problemOf(header: HeaderPlan): String {
        val kind = kinds[header.reader]
        if (kind == null) return "The header ${header.wireName} is missing"
        val absence = "missing or ".takeIf { !header.type.isNullable }.orEmpty()
        return "The header ${header.wireName} is ${absence}not $kind"
    }
}
