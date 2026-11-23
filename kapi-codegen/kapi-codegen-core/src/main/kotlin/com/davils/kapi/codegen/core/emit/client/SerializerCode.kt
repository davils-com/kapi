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
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.STRING

private const val GENERATED: String = "%T.serializer()"

private val opaque: Map<ScalarKind, CodeBlock> = mapOf(
    ScalarKind.Bytes to CodeBlock.of("%T", Kotlin.base64),
    ScalarKind.Json to CodeBlock.of(GENERATED, Kotlin.jsonElement),
)

internal class SerializerCode(private val types: TypeNames) {
    fun of(use: TypeUse): CodeBlock {
        val base = shape(use.shape)
        if (!use.isNullable) return base
        return CodeBlock.of("%L.%M", base, ClientTypes.nullable)
    }

    private fun shape(shape: TypeShape): CodeBlock = when (shape) {
        is TypeShape.Scalar -> scalar(shape.kind)
        is TypeShape.Named -> CodeBlock.of(GENERATED, types.of(TypeUse(shape)))
        is TypeShape.External -> external(shape)
        is TypeShape.Items -> CodeBlock.of("%M(%L)", ClientTypes.listSerializer, of(shape.item))
        is TypeShape.Entries -> entries(shape)
    }

    private fun entries(shape: TypeShape.Entries): CodeBlock =
        CodeBlock.of("%M(%T.%M(), %L)", ClientTypes.mapSerializer, STRING, ClientTypes.serializer, of(shape.value))

    private fun scalar(kind: ScalarKind): CodeBlock =
        opaque[kind] ?: CodeBlock.of("%T.%M()", types.of(TypeUse(TypeShape.Scalar(kind))), ClientTypes.serializer)

    private fun external(shape: TypeShape.External): CodeBlock {
        val serializer = shape.serializer ?: return CodeBlock.of(GENERATED, types.external(shape.type))
        return CodeBlock.of("%T", types.external(serializer))
    }
}
