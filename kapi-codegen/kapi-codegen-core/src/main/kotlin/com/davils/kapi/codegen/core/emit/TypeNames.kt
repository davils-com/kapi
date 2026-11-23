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


package com.davils.kapi.codegen.core.emit

import com.davils.kapi.codegen.core.model.plan.Chunks
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.U_LONG

private const val DOT: Char = '.'

internal class TypeNames(private val modelPackage: String) {
    private val scalars: Map<ScalarKind, TypeName> = mapOf(
        ScalarKind.Text to STRING,
        ScalarKind.Int to INT,
        ScalarKind.Long to LONG,
        ScalarKind.UnsignedLong to U_LONG,
        ScalarKind.Float to FLOAT,
        ScalarKind.Double to DOUBLE,
        ScalarKind.Flag to BOOLEAN,
        ScalarKind.Instant to Kotlin.instant,
        ScalarKind.Bytes to Kotlin.byteString.copy(annotations = listOf(Annotations.serializedWith(Kotlin.base64))),
        ScalarKind.Json to Kotlin.jsonElement,
    )

    private val special: Map<String, TypeName> = mapOf(
        Chunks.STREAM to ClassName("kotlinx.coroutines.flow", "Flow").parameterizedBy(Kotlin.byteString),
    )

    fun of(use: TypeUse): TypeName = shape(use.shape).copy(nullable = use.isNullable)

    fun model(name: String): ClassName = ClassName(modelPackage, name)

    fun external(qualified: String): ClassName {
        val segments = qualified.split(DOT)
        val firstType = segments.indexOfFirst { segment -> segment.first().isUpperCase() }
        val index = firstType.takeIf { found -> found >= 0 } ?: segments.lastIndex
        return ClassName(segments.take(index).joinToString(DOT.toString()), segments.drop(index))
    }

    private fun external(shape: TypeShape.External): TypeName = special[shape.type] ?: declared(shape)

    private fun declared(shape: TypeShape.External): TypeName {
        val type = external(shape.type)
        val serializer = shape.serializer ?: return type
        return type.copy(annotations = listOf(Annotations.serializedWith(external(serializer))))
    }

    private fun shape(shape: TypeShape): TypeName = when (shape) {
        is TypeShape.Scalar -> scalars.getValue(shape.kind)
        is TypeShape.Named -> ClassName(shape.packageName ?: modelPackage, shape.name)
        is TypeShape.External -> external(shape)
        is TypeShape.Items -> LIST.parameterizedBy(of(shape.item))
        is TypeShape.Entries -> MAP.parameterizedBy(STRING, of(shape.value))
    }
}
