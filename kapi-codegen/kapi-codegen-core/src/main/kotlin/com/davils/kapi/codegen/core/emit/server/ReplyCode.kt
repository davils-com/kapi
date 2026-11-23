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

import com.davils.kapi.codegen.core.emit.Conditionals
import com.davils.kapi.codegen.core.emit.client.SerializerCode
import com.davils.kapi.codegen.core.operation.plan.HeaderPlan
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ResponsePlan
import com.davils.kapi.codegen.core.operation.plan.StreamKind
import com.davils.kapi.codegen.core.operation.plan.StreamPlan
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.joinToCode

private const val RESPONSE: String = "response"

private const val OCTETS: String = "application/octet-stream"

private const val EVENTS: String = "text/event-stream"

private const val NDJSON: String = "application/x-ndjson"

internal class ReplyCode(private val serializers: SerializerCode) {
    fun of(operation: OperationPlan, response: ClassName, arguments: List<Pair<String, CodeBlock>>): CodeBlock {
        val passed = arguments.map { (name, value) -> CodeBlock.of("%N = %L", name, value) }.joinToCode(", ")
        val isConditional = Conditionals.isConditional(operation)
        val code = CodeBlock.builder()
        code.add("return when (val %N = service.%N(%L)) {\n", RESPONSE, operation.name, passed)
        code.indent()
        operation.responses.forEach { plan ->
            val reply = reply(plan)
            val shown = conditional(plan, reply).takeIf { isConditional && Conditionals.isValidated(plan) } ?: reply
            code.add("is %T.%L -> %L\n", response, plan.name, shown)
        }
        code.unindent()
        code.add("}\n")
        return code.build()
    }

    private fun conditional(plan: ResponsePlan, reply: CodeBlock): CodeBlock {
        val tag = Conditionals.entityTagOf(plan)?.let { header -> validator(header, ServerTypes.entityTag) }
        val date = Conditionals.lastModifiedOf(plan)?.let { header -> validator(header, ServerTypes.httpDate) }
        val state = CodeBlock.of(
            "%T(entityTag = %L, lastModified = %L)",
            ServerTypes.state,
            tag ?: CodeBlock.of("null"),
            date ?: CodeBlock.of("null"),
        )
        return CodeBlock.of("request.%M(%L) {\n⇥%L\n⇤}", ServerTypes.conditional, state, reply)
    }

    private fun validator(header: HeaderPlan, type: ClassName): CodeBlock {
        val value = CodeBlock.of("%N.%N", RESPONSE, header.name)
        if (!header.type.isNullable) return CodeBlock.of("%T.parseOrNull(%L)", type, value)
        return CodeBlock.of("%L?.let { text -> %T.parseOrNull(text) }", value, type)
    }

    private fun reply(plan: ResponsePlan): CodeBlock {
        val status = statusOf(plan)
        val parts = plan.headers.map(::header) + content(plan)
        if (parts.isEmpty()) return CodeBlock.of("%M(%L)", ServerTypes.replying, status)
        val code = CodeBlock.builder()
        code.add("%M(%L) {\n", ServerTypes.replying, status)
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
        val stream = plan.stream
        if (stream != null) return listOf(streamed(plan, stream))
        val types = plan.accept.take(1)
        return types.map { type -> body(plan, type) }
    }

    private fun streamed(plan: ResponsePlan, stream: StreamPlan): CodeBlock {
        val items = CodeBlock.of("%N.%N", RESPONSE, ResponseEmitter.ITEMS)
        return when (stream.kind) {
            StreamKind.Bytes -> CodeBlock.of("stream(%L, %L)", mediaOf(plan, OCTETS), items)

            StreamKind.Records -> CodeBlock.of(
                "lines(%L, %L, %L)",
                serializers.of(stream.item),
                items,
                mediaOf(plan, NDJSON),
            )

            StreamKind.Events -> events(stream, items)
        }
    }

    private fun events(stream: StreamPlan, items: CodeBlock): CodeBlock {
        val data = CodeBlock.of(
            "%T.Default.json.encodeToString(%L, item)",
            ServerTypes.codecs,
            serializers.of(stream.item),
        )
        return CodeBlock.of(
            "events {\n⇥%L.collect { item -> send(%T(data = %L)) }\n⇤}",
            items,
            ServerTypes.event,
            data,
        )
    }

    private fun mediaOf(plan: ResponsePlan, fallback: String): CodeBlock {
        val type = plan.accept.firstOrNull { found -> found != EVENTS } ?: fallback
        return ServerTypes.mediaType(type)
    }

    private fun body(plan: ResponsePlan, type: String): CodeBlock {
        val body = plan.body ?: return CodeBlock.of("bytes(%L, %N.content)", ServerTypes.mediaType(type), RESPONSE)
        return CodeBlock.of("body(%L, %L, %N.body)", ServerTypes.mediaType(type), serializers.of(body), RESPONSE)
    }
}
