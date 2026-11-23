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

import com.davils.kapi.codegen.core.emit.Kotlin
import com.davils.kapi.codegen.core.emit.TypeNames
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.StreamKind
import com.davils.kapi.codegen.core.operation.plan.StreamPlan
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

private const val STREAMS: String = "com.davils.kapi.runtime.client.stream"

private val flow: ClassName = ClassName("kotlinx.coroutines.flow", "Flow")

private val emptyFlow: MemberName = MemberName("kotlinx.coroutines.flow", "emptyFlow")

private val event: ClassName = ClassName(STREAMS, "StreamEvent")

internal class StreamCode(private val types: TypeNames, private val serializers: SerializerCode) {
    fun function(operation: OperationPlan, stream: StreamPlan, parts: StreamParts): FunSpec {
        val function = FunSpec.builder(operation.name + SUFFIX)
        function.addModifiers(parts.visibility)
        parts.signatures.taken(operation).forEach { (name, type) ->
            function.addParameter(parts.signatures.parameter(name, type))
        }
        if (operation.upgrade != null) function.addParameter(input())
        function.returns(flow.parameterizedBy(event.parameterizedBy(types.of(stream.item))))
        function.addCode(parts.calls.of(operation, isStreamed = true))
        function.addCode(reading(stream))
        return function.build()
    }

    private fun input(): ParameterSpec {
        val type = flow.parameterizedBy(Kotlin.byteString)
        val parameter = ParameterSpec.builder(CallCode.INPUT, type)
        parameter.defaultValue("%M()", emptyFlow)
        return parameter.build()
    }

    private fun reading(stream: StreamPlan): CodeBlock {
        if (stream.kind == StreamKind.Bytes) return CodeBlock.of("return client.bytes(call)\n")
        val reader = MemberName(STREAMS, stream.kind.function, isExtension = true)
        return CodeBlock.of("return client.%M(%L, call)\n", reader, serializers.of(stream.item))
    }

    companion object {
        const val SUFFIX: String = "Stream"
    }
}
