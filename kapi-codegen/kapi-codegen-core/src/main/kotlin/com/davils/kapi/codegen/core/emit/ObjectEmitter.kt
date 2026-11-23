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
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal class ObjectEmitter(private val types: TypeNames, private val visibility: KModifier) {
    fun type(plan: ModelPlan.ObjectPlan): TypeSpec {
        val patterns = PatternTable()
        val validate = validation(plan, ValidationCode(patterns))
        val builder = TypeSpec.classBuilder(plan.name)
        builder.addModifiers(visibility)
        if (plan.properties.isNotEmpty()) builder.addModifiers(KModifier.DATA)
        builder.addAnnotation(Kotlin.serializable)
        if (plan.properties.any(PropertyPlan::isPatch)) builder.addAnnotation(Annotations.experimentalSerialization())
        builder.addAnnotation(Annotations.generated(plan.origin))
        builder.primaryConstructor(constructor(plan))
        builder.addProperties(plan.properties.map(::property))
        builder.addSuperinterface(Kotlin.transferObject)
        builder.addSuperinterface(Kotlin.validatable)
        plan.unions.forEach { union -> builder.addSuperinterface(types.model(union)) }
        builder.addFunction(validate)
        if (plan.properties.any(PropertyPlan::isSensitive)) builder.addFunction(masked(plan))
        if (patterns.patterns.isNotEmpty()) builder.addType(companion(patterns, plan.origin))
        return builder.build()
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

    private fun masked(plan: ModelPlan.ObjectPlan): FunSpec {
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
        function.addStatement("return %L", template.build())
        return function.build()
    }

    private fun shown(property: PropertyPlan): CodeBlock {
        if (property.isSensitive) return CodeBlock.of("%T.MASK", Kotlin.secret)
        return CodeBlock.of("%N", property.name)
    }

    private fun companion(patterns: PatternTable, origin: String): TypeSpec {
        val companion = TypeSpec.companionObjectBuilder()
        companion.addModifiers(visibility)
        companion.addAnnotation(Annotations.generated(origin))
        patterns.patterns.forEach { (name, pattern) ->
            val property = PropertySpec.builder(name, Kotlin.regex, KModifier.PRIVATE)
            property.initializer("%T(%S)", Kotlin.regex, pattern)
            companion.addProperty(property.build())
        }
        return companion.build()
    }
}
