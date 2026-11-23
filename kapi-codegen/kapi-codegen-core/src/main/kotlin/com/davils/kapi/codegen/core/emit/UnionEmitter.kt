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

import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName

private const val SUFFIX: String = "Serializer"

private const val MEMBERS: String = "members"

private const val ELEMENT: String = "element"

internal class UnionEmitter(private val types: TypeNames, private val visibility: KModifier) {
    fun types(plan: ModelPlan.UnionPlan): Map<String, TypeSpec> =
        mapOf(plan.name to union(plan), plan.name + SUFFIX to serializer(plan))

    private fun union(plan: ModelPlan.UnionPlan): TypeSpec {
        val builder = TypeSpec.interfaceBuilder(plan.name)
        builder.addModifiers(visibility, KModifier.SEALED)
        builder.addAnnotation(Annotations.serializedWith(types.model(plan.name + SUFFIX)))
        builder.addAnnotation(Annotations.generated(plan.origin))
        builder.addSuperinterface(Kotlin.validatable)
        return builder.build()
    }

    private fun serializer(plan: ModelPlan.UnionPlan): TypeSpec {
        val self = types.model(plan.name)
        val builder = TypeSpec.objectBuilder(plan.name + SUFFIX)
        builder.addModifiers(KModifier.INTERNAL)
        builder.addAnnotation(Annotations.generated(plan.origin))
        builder.superclass(Kotlin.contentPolymorphic.parameterizedBy(self))
        builder.addSuperclassConstructorParameter("%T::class", self)
        builder.addProperty(members(plan, self))
        builder.addFunction(select(plan, self))
        return builder.build()
    }

    private fun members(plan: ModelPlan.UnionPlan, self: ClassName): PropertySpec {
        val serializer = Kotlin.serializer.parameterizedBy(WildcardTypeName.producerOf(self))
        val entries = CodeBlock.builder()
        plan.members.forEach { member ->
            entries.add("%S to %T.serializer(),\n", member.value, types.model(member.type))
        }
        val property = PropertySpec.builder(MEMBERS, MAP.parameterizedBy(STRING, serializer), KModifier.PRIVATE)
        property.initializer("mapOf(\n⇤%L⇤)⇥⇥", entries.build())
        return property.build()
    }

    private fun select(plan: ModelPlan.UnionPlan, self: ClassName): FunSpec {
        val function = FunSpec.builder("selectDeserializer")
        function.addModifiers(KModifier.OVERRIDE)
        function.addParameter(ELEMENT, Kotlin.jsonElement)
        function.returns(Kotlin.deserializationStrategy.parameterizedBy(self))
        function.addStatement("val fields = %N as? %T", ELEMENT, Kotlin.jsonObject)
        function.addCode(selection(plan))
        return function.build()
    }

    private fun selection(plan: ModelPlan.UnionPlan): CodeBlock {
        val property = plan.discriminator ?: return deduced(plan)
        return tagged(plan, property)
    }

    private fun tagged(plan: ModelPlan.UnionPlan, property: String): CodeBlock {
        val code = CodeBlock.builder()
        code.addStatement("val tag = fields?.get(%S) as? %T", property, Kotlin.jsonPrimitive)
        code.addStatement("val value = tag?.takeIf { primitive -> primitive.isString }?.content.orEmpty()")
        code.add(chosen(key = "value", message = "The property $property names none of ${valuesOf(plan)}"))
        return code.build()
    }

    private fun deduced(plan: ModelPlan.UnionPlan): CodeBlock {
        val pick = "singleOrNull".takeIf { plan.isExclusive } ?: "firstOrNull"
        val amount = "not exactly one".takeIf { plan.isExclusive } ?: "none"
        val code = CodeBlock.builder()
        code.addStatement("val present = %N.keys.filter { name -> fields?.containsKey(name) == true }", MEMBERS)
        code.addStatement("val key = present.%L().orEmpty()", pick)
        code.add(chosen(key = "key", message = "The object has $amount of ${valuesOf(plan)}"))
        return code.build()
    }

    private fun chosen(key: String, message: String): CodeBlock {
        val code = CodeBlock.builder()
        code.addStatement("return %N[%L] ?: throw %T(%S)", MEMBERS, key, Kotlin.serializationException, message)
        return code.build()
    }

    private fun valuesOf(plan: ModelPlan.UnionPlan): String = plan.members.joinToString(", ") { member -> member.value }
}
