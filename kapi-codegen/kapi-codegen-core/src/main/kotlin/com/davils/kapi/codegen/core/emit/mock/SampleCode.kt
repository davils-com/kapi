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

package com.davils.kapi.codegen.core.emit.mock

import com.davils.kapi.codegen.core.emit.Kotlin
import com.davils.kapi.codegen.core.emit.TypeNames
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.sample.plan.SampleValue
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.joinToCode
import java.math.BigDecimal

private val literals: Map<ScalarKind, (BigDecimal) -> String> = mapOf(
    ScalarKind.Int to { value -> value.toBigInteger().toString() },
    ScalarKind.Long to { value -> value.toBigInteger().toString() + "L" },
    ScalarKind.UnsignedLong to { value -> value.toBigInteger().toString() + "UL" },
    ScalarKind.Float to { value -> value.toFloat().toString() + "f" },
    ScalarKind.Double to { value -> value.toDouble().toString() },
)

private val flowOf: MemberName = MemberName(packageName = "kotlinx.coroutines.flow", simpleName = "flowOf")

internal class SampleCode(private val types: TypeNames, private val reference: (String) -> CodeBlock) {
    fun of(value: SampleValue): CodeBlock = when (value) {
        is SampleValue.Text -> CodeBlock.of("%S", value.value)
        is SampleValue.Number -> CodeBlock.of("%L", literals.getValue(value.kind)(value.value))
        is SampleValue.Flag -> CodeBlock.of("%L", value.isTrue)
        is SampleValue.Moment -> CodeBlock.of("%T.parse(%S)", Kotlin.instant, value.text)
        SampleValue.Bytes -> CodeBlock.of("%T()", Kotlin.byteString)
        SampleValue.Json -> CodeBlock.of("%T(emptyMap())", Kotlin.jsonObject)
        SampleValue.Null -> CodeBlock.of("null")
        is SampleValue.Entry -> CodeBlock.of("%T.%N", types.model(value.type), value.name)
        is SampleValue.Model -> reference(value.name)
        is SampleValue.Items -> items(value)
        is SampleValue.Entries -> entries(value)
        is SampleValue.Built -> called(CodeBlock.of("%T", types.model(value.type)), value.arguments)
        is SampleValue.Stream -> CodeBlock.of("%M(%L)", flowOf, value.values.map(::of).joinToCode(", "))
    }

    fun called(callee: CodeBlock, arguments: Map<String, SampleValue>): CodeBlock {
        if (arguments.isEmpty()) return CodeBlock.of("%L()", callee)
        val code = CodeBlock.builder()
        code.add("%L(\n⇥", callee)
        arguments.forEach { (name, argument) -> code.add("%N = %L,\n", name, of(argument)) }
        code.add("⇤)")
        return code.build()
    }

    private fun items(value: SampleValue.Items): CodeBlock {
        if (value.values.isEmpty()) return CodeBlock.of("emptyList()")
        return CodeBlock.of("listOf(%L)", value.values.map(::of).joinToCode(", "))
    }

    private fun entries(value: SampleValue.Entries): CodeBlock {
        if (value.values.isEmpty()) return CodeBlock.of("emptyMap()")
        val pairs = value.values.map { (key, entry) -> CodeBlock.of("%S to %L", key, of(entry)) }
        return CodeBlock.of("mapOf(%L)", pairs.joinToCode(", "))
    }
}
