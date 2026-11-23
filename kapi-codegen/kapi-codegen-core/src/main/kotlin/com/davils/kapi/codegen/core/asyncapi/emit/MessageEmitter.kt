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

import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.GeneratedPlatform
import com.davils.kapi.codegen.core.asyncapi.plan.ChannelPlan
import com.davils.kapi.codegen.core.asyncapi.plan.MessagePlan
import com.davils.kapi.codegen.core.asyncapi.plan.Side
import com.davils.kapi.codegen.core.emit.Texts
import com.davils.kapi.codegen.core.naming.Identifiers
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec

private const val PAYLOAD: String = "payload"

private const val OTHER: String = "other"

internal class MessageEmitter(private val emission: Emission) {
    fun emit(plan: ChannelPlan): List<GeneratedFile> =
        Side.entries.filter { side -> side.of(plan).isSealed }.flatMap { side -> files(plan, side) }

    private fun files(plan: ChannelPlan, side: Side): List<GeneratedFile> = listOf(
        emission.file(emission.channelPackage, sealed(plan, side), GeneratedPlatform.Common),
        emission.file(emission.channelPackage, serializer(plan, side), GeneratedPlatform.Common),
    )

    private fun sealed(plan: ChannelPlan, side: Side): TypeSpec {
        val direction = side.of(plan)
        val name = emission.messageType(plan, side)
        val type = TypeSpec.interfaceBuilder(name)
        type.addModifiers(emission.modifier, KModifier.SEALED)
        type.addKdoc("%L", Kdocs.direction(plan, side))
        val serializable = AnnotationSpec.builder(Runtime.serializable)
        serializable.addMember("with = %T::class", emission.serializerType(plan, side))
        type.addAnnotation(serializable.build())
        type.addAnnotation(emission.generated(plan))
        direction.messages.forEach { message ->
            type.addType(member(plan = plan, side = side, sealed = name, message = message))
        }
        return type.build()
    }

    private fun member(plan: ChannelPlan, side: Side, sealed: ClassName, message: MessagePlan): TypeSpec {
        val name = sealed.nestedClass(message.member)
        val payload = emission.payloadType(message)
        val type = TypeSpec.classBuilder(name)
        type.addModifiers(KModifier.PUBLIC)
        type.addKdoc("%L", Kdocs.message(side.of(plan), message))
        type.addSuperinterface(sealed)
        val constructor = FunSpec.constructorBuilder().addParameter(PAYLOAD, payload)
        type.primaryConstructor(constructor.build())
        type.addProperty(Specs.stored(PAYLOAD, payload, KModifier.PUBLIC))
        val equals = FunSpec.builder("equals")
        equals.addModifiers(KModifier.OVERRIDE)
        equals.addParameter(OTHER, ANY.copy(nullable = true))
        equals.returns(BOOLEAN)
        equals.addStatement("return %N is %T && %N == %N.%N", OTHER, name, PAYLOAD, OTHER, PAYLOAD)
        type.addFunction(equals.build())
        val hash = FunSpec.builder("hashCode").addModifiers(KModifier.OVERRIDE)
        hash.returns(INT)
        hash.addStatement("return %N.hashCode()", PAYLOAD)
        type.addFunction(hash.build())
        val shown = FunSpec.builder("toString").addModifiers(KModifier.OVERRIDE)
        shown.returns(STRING)
        shown.addStatement("return %S + %N + %S", message.member + "(", PAYLOAD, ")")
        type.addFunction(shown.build())
        return type.build()
    }

    private fun serializer(plan: ChannelPlan, side: Side): TypeSpec {
        val direction = side.of(plan)
        val sealed = emission.messageType(plan, side)
        val type = TypeSpec.objectBuilder(emission.serializerType(plan, side))
        type.addModifiers(KModifier.INTERNAL)
        type.addSuperinterface(Runtime.kSerializer.parameterizedBy(sealed))
        type.addAnnotation(emission.generated(plan))
        direction.messages.forEach { message ->
            val payload = emission.payloadType(message)
            val property = PropertySpec.builder(serializerName(message), Runtime.kSerializer.parameterizedBy(payload))
            property.addModifiers(KModifier.PRIVATE)
            property.initializer("%M()", Runtime.serializer)
            type.addProperty(property.build())
        }
        type.addProperty(members(sealed, direction.messages))
        val descriptor = PropertySpec.builder("descriptor", Runtime.serialDescriptor, KModifier.OVERRIDE)
        descriptor.initializer("%M(%S)", Runtime.buildClassSerialDescriptor, sealed.canonicalName)
        type.addProperty(descriptor.build())
        type.addFunction(serialize(sealed, direction.messages))
        type.addFunction(deserialize(sealed, direction.discriminator.orEmpty()))
        return type.build()
    }

    private fun members(sealed: ClassName, messages: List<MessagePlan>): PropertySpec {
        val reader = LambdaTypeName.get(null, Runtime.json, Runtime.jsonElement, returnType = sealed)
        val property = PropertySpec.builder(MEMBERS, MAP.parameterizedBy(STRING, reader), KModifier.PRIVATE)
        val code = CodeBlock.builder()
        code.add("mapOf(\n⇤")
        messages.forEach { message ->
            code.add(
                "%L to { json, element -> %T(json.decodeFromJsonElement(%N, element)) },\n",
                Texts.literal(message.tag.orEmpty()),
                sealed.nestedClass(message.member),
                serializerName(message),
            )
        }
        code.add("⇤)⇥⇥")
        property.initializer(code.build())
        return property.build()
    }

    private fun serialize(sealed: ClassName, messages: List<MessagePlan>): FunSpec {
        val function = FunSpec.builder("serialize").addModifiers(KModifier.OVERRIDE)
        function.addParameter(ENCODER, Runtime.encoder)
        function.addParameter(VALUE, sealed)
        function.beginControlFlow("when (%N)", VALUE)
        messages.forEach { message ->
            function.addStatement(
                "is %T -> %N.encodeSerializableValue(%N, %N.%N)",
                sealed.nestedClass(message.member),
                ENCODER,
                serializerName(message),
                VALUE,
                PAYLOAD,
            )
        }
        function.endControlFlow()
        return function.build()
    }

    private fun deserialize(sealed: ClassName, discriminator: String): FunSpec {
        val function = FunSpec.builder("deserialize").addModifiers(KModifier.OVERRIDE)
        function.returns(sealed)
        function.addParameter(DECODER, Runtime.decoder)
        function.addStatement(
            "val json = %N as? %T ?: throw %T(%S)",
            DECODER,
            Runtime.jsonDecoder,
            Runtime.serializationException,
            "The messages of ${sealed.simpleName} are read from JSON only",
        )
        function.addStatement("val element = json.decodeJsonElement()")
        function.addStatement(
            "val tag = (element as? %T)?.get(%L) as? %T",
            Runtime.jsonObject,
            Texts.literal(discriminator),
            Runtime.jsonPrimitive,
        )
        function.addStatement("val name = tag?.takeIf { primitive -> primitive.isString }?.content")
        function.addStatement(
            "val member = name?.let(%N::get) ?: throw %T(%S)",
            MEMBERS,
            Runtime.serializationException,
            "The message is none of ${sealed.simpleName}",
        )
        function.addStatement("return member(json.json, element)")
        return function.build()
    }

    private fun serializerName(message: MessagePlan): String = Identifiers.member(message.member) + "Serializer"

    private companion object {
        const val MEMBERS: String = "members"
        const val ENCODER: String = "encoder"
        const val DECODER: String = "decoder"
        const val VALUE: String = "value"
    }
}
