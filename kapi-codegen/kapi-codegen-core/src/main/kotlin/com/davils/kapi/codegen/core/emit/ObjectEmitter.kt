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

import com.davils.kapi.codegen.core.model.plan.DependencyPlan
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.PropertyPlan
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

private const val MASK: String = "****"

private const val OTHER: String = "other"

private const val THIS: String = "this"

private const val RETURN: String = "return %L"

private const val COPY_KDOC: String =
    "Returns a copy of this value with the given properties replaced; every property not given keeps its value."

internal class ObjectEmitter(private val types: TypeNames, private val visibility: KModifier) {
    fun type(plan: ModelPlan.ObjectPlan): ModelFile {
        val patterns = PatternTable()
        val validate = validation(plan, ValidationCode(patterns))
        val self = types.model(plan.name)
        val builder = TypeSpec.classBuilder(plan.name)
        builder.addModifiers(visibility)
        builder.addAnnotation(Kotlin.serializable)
        if (plan.properties.any(PropertyPlan::isPatch)) builder.addAnnotation(Annotations.experimentalSerialization())
        builder.addAnnotation(Annotations.generated(plan.origin))
        builder.primaryConstructor(constructor(plan))
        builder.addProperties(plan.properties.map(::property))
        builder.addSuperinterface(Kotlin.validatable)
        plan.unions.forEach { union -> builder.addSuperinterface(types.model(union)) }
        builder.addFunction(validate)
        if (plan.properties.isNotEmpty()) builder.addFunction(copy(plan, self))
        builder.addFunction(equality(plan, self))
        builder.addFunction(hash(plan))
        builder.addFunction(text(plan))
        return ModelFile(builder.build(), patterns.properties)
    }

    private fun constructor(plan: ModelPlan.ObjectPlan): FunSpec {
        val constructor = FunSpec.constructorBuilder()
        plan.properties.forEach { property -> constructor.addParameter(parameter(property)) }
        return constructor.build()
    }

    private fun parameter(property: PropertyPlan): ParameterSpec {
        val parameter = ParameterSpec.builder(property.name, typeOf(property))
        if (property.isOptional && !property.isPatch) parameter.defaultValue("null")
        if (property.isPatch) parameter.defaultValue("%T.Absent", Kotlin.patch)
        return parameter.build()
    }

    private fun property(property: PropertyPlan): PropertySpec {
        val builder = PropertySpec.builder(property.name, typeOf(property), visibility)
        builder.initializer("%N", property.name)
        builder.addAnnotation(Annotations.serialName(property.jsonName))
        if (property.isPatch) builder.addAnnotation(Annotations.neverDefault())
        return builder.build()
    }

    private fun typeOf(property: PropertyPlan): TypeName {
        if (!property.isPatch) return types.of(property.type)
        return Kotlin.patch.parameterizedBy(types.of(property.type.copy(isNullable = false)))
    }

    private fun validation(plan: ModelPlan.ObjectPlan, code: ValidationCode): FunSpec {
        val body = CodeBlock.builder()
        plan.properties.forEach { property -> body.add(code.property(property)) }
        plan.dependencies.forEach { dependency -> body.add(dependency(dependency)) }
        val function = FunSpec.builder("validate")
        function.addModifiers(KModifier.OVERRIDE)
        function.receiver(Kotlin.validationScope)
        function.addCode(body.build())
        return function.build()
    }

    private fun dependency(dependency: DependencyPlan): CodeBlock {
        val trigger = CodeBlock.of("%N != null && ", dependency.trigger.name)
        val guard = trigger.takeIf { dependency.trigger.type.isNullable } ?: CodeBlock.of("")
        return CodeBlock.of(
            "if (%L%N == null) fail(::%N, %S, %T.DEPENDENT_REQUIRED)\n",
            guard,
            dependency.dependent.name,
            dependency.dependent.name,
            "is required when ${dependency.trigger.jsonName} is given",
            Kotlin.codes,
        )
    }

    private fun copy(plan: ModelPlan.ObjectPlan, self: ClassName): FunSpec {
        val function = FunSpec.builder("copy")
        function.addModifiers(visibility)
        function.addKdoc("%L", COPY_KDOC)
        plan.properties.forEach { property ->
            val parameter = ParameterSpec.builder(property.name, typeOf(property))
            parameter.defaultValue("this.%N", property.name)
            function.addParameter(parameter.build())
        }
        function.returns(self)
        val arguments = plan.properties.map { property -> CodeBlock.of("%N = %N", property.name, property.name) }
        function.addStatement("return %T(%L)", self, arguments.joinToCode(", "))
        return function.build()
    }

    private fun equality(plan: ModelPlan.ObjectPlan, self: ClassName): FunSpec {
        val function = FunSpec.builder("equals")
        function.addModifiers(KModifier.OVERRIDE)
        function.addParameter(OTHER, ANY.copy(nullable = true))
        function.returns(BOOLEAN)
        val sameType = CodeBlock.of("%N is %T", OTHER, self)
        if (plan.properties.isEmpty()) return function.addStatement(RETURN, sameType).build()
        val own = values(plan, CodeBlock.of(THIS))
        val theirs = values(plan, CodeBlock.of("%N", OTHER))
        function.addStatement("return %L && %L == %L", sameType, own, theirs)
        return function.build()
    }

    private fun hash(plan: ModelPlan.ObjectPlan): FunSpec {
        val function = FunSpec.builder("hashCode")
        function.addModifiers(KModifier.OVERRIDE)
        function.returns(INT)
        val all = CodeBlock.of("%L.hashCode()", values(plan, CodeBlock.of(THIS)))
        function.addStatement(RETURN, all.takeIf { plan.properties.isNotEmpty() } ?: CodeBlock.of("0"))
        return function.build()
    }

    private fun values(plan: ModelPlan.ObjectPlan, owner: CodeBlock): CodeBlock {
        val read = plan.properties.map { property -> CodeBlock.of("%L.%N", owner, property.name) }
        return CodeBlock.of("listOf(%L)", read.joinToCode(", "))
    }

    private fun text(plan: ModelPlan.ObjectPlan): FunSpec {
        val template = CodeBlock.builder()
        template.add("%S", plan.name + "(")
        plan.properties.forEachIndexed { index, property ->
            val separator = ", ".takeIf { index > 0 }.orEmpty()
            template.add(" + %S + ", separator + property.name + "=")
            template.add(shown(property))
        }
        template.add(" + %S", ")")
        val function = FunSpec.builder("toString")
        function.addModifiers(KModifier.OVERRIDE)
        function.returns(STRING)
        function.addStatement(RETURN, template.build())
        return function.build()
    }

    private fun shown(property: PropertyPlan): CodeBlock {
        if (property.isSensitive) return CodeBlock.of("%S", MASK)
        return CodeBlock.of("this.%N", property.name)
    }
}
