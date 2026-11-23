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

import com.davils.kapi.codegen.core.model.plan.EnumEntryPlan
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec

private const val VALUE: String = "value"

private const val ENTRIES: String = "entries"

private const val TEXT: String = "%S"

private val literals: Map<ScalarKind, String> =
    mapOf(ScalarKind.Text to TEXT, ScalarKind.Long to "%LL", ScalarKind.Int to "%L")

internal class EnumEmitter(private val types: TypeNames, private val visibility: KModifier) {
    fun type(plan: ModelPlan.EnumPlan): TypeSpec {
        val base = types.of(TypeUse(TypeShape.Scalar(plan.base)))
        val constructor = FunSpec.constructorBuilder()
        constructor.addParameter(VALUE, base)
        val value = PropertySpec.builder(VALUE, base, visibility)
        value.initializer("%N", VALUE)
        val builder = TypeSpec.classBuilder(plan.name)
        builder.addModifiers(visibility, KModifier.VALUE)
        builder.addAnnotation(Kotlin.serializable)
        builder.addAnnotation(Kotlin.jvmInline)
        builder.addAnnotation(Annotations.generated(plan.origin))
        builder.primaryConstructor(constructor.build())
        builder.addProperty(value.build())
        builder.addSuperinterface(Kotlin.transferObject)
        builder.addProperty(known())
        builder.addFunction(rendered(plan.base))
        builder.addType(companion(plan, types.model(plan.name)))
        return builder.build()
    }

    private fun known(): PropertySpec {
        val getter = FunSpec.getterBuilder()
        getter.addStatement("return this in %N", ENTRIES)
        val property = PropertySpec.builder("isKnown", BOOLEAN, visibility)
        property.getter(getter.build())
        return property.build()
    }

    private fun rendered(base: ScalarKind): FunSpec {
        val text = CodeBlock.of("%N", VALUE).takeIf { base == ScalarKind.Text } ?: CodeBlock.of("%N.toString()", VALUE)
        val function = FunSpec.builder("toString")
        function.addModifiers(KModifier.OVERRIDE)
        function.returns(STRING)
        function.addStatement("return %L", text)
        return function.build()
    }

    private fun companion(plan: ModelPlan.EnumPlan, self: ClassName): TypeSpec {
        val format = literals.getValue(plan.base)
        val names = plan.entries.joinToString(", ") { entry -> CodeBlock.of("%N", entry.name).toString() }
        val entries = PropertySpec.builder(ENTRIES, LIST.parameterizedBy(self), visibility)
        entries.initializer("listOf(%L)", names)
        val companion = TypeSpec.companionObjectBuilder()
        companion.addModifiers(visibility)
        companion.addAnnotation(Annotations.generated(plan.origin))
        plan.entries.forEach { entry -> companion.addProperty(constant(entry, self, format)) }
        companion.addProperty(entries.build())
        return companion.build()
    }

    private fun constant(entry: EnumEntryPlan, self: ClassName, format: String): PropertySpec {
        val literal = entry.literal.toLongOrNull().takeUnless { format == TEXT } ?: entry.literal
        val property = PropertySpec.builder(entry.name, self, visibility)
        property.initializer("%T($format)", self, literal)
        return property.build()
    }
}
