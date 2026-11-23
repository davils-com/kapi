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

package com.davils.kapi.codegen.core.asyncapi.emit

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName

private const val NEWLINE: String = "\n"

private const val SESSION: String = "session"

internal object Specs {
    fun stored(name: String, type: TypeName, modifier: KModifier): PropertySpec =
        initialized(name = name, type = type, initializer = CodeBlock.of("%N", name), modifier = modifier)

    fun initialized(name: String, type: TypeName, initializer: CodeBlock, modifier: KModifier): PropertySpec {
        val property = PropertySpec.builder(name, type, modifier)
        property.initializer(initializer)
        return property.build()
    }

    fun parameter(name: String, type: TypeName, default: CodeBlock): ParameterSpec {
        val parameter = ParameterSpec.builder(name, type)
        parameter.defaultValue(default)
        return parameter.build()
    }

    fun delegated(name: String, type: TypeName, kdoc: String): PropertySpec {
        val getter = FunSpec.getterBuilder()
        getter.addStatement("return %N.%N", SESSION, name)
        val property = PropertySpec.builder(name, type, KModifier.PUBLIC)
        property.addKdoc("%L", kdoc)
        property.getter(getter.build())
        return property.build()
    }

    fun kdoc(vararg lines: String): String = lines.joinToString(NEWLINE)
}
