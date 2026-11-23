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
import com.davils.kapi.codegen.core.asyncapi.plan.RequestPlan
import com.davils.kapi.codegen.core.asyncapi.plan.Side
import com.davils.kapi.codegen.core.emit.Texts
import com.davils.kapi.codegen.core.naming.Identifiers
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec

private const val PAYLOAD: String = "payload"

private const val MESSAGE: String = "message"

private const val JSON: String = "json"

private const val STEP: String = "step"

internal class CorrelationEmitter(private val emission: Emission) {
    fun typeName(plan: ChannelPlan): ClassName = ClassName(emission.clientPackage, plan.base + "Correlation")

    fun emit(plan: ChannelPlan): List<GeneratedFile> {
        if (!plan.hasCorrelation) return emptyList()
        return listOf(emission.file(emission.clientPackage, type(plan), GeneratedPlatform.Common))
    }

    private fun type(plan: ChannelPlan): TypeSpec {
        val type = TypeSpec.objectBuilder(typeName(plan))
        type.addModifiers(KModifier.INTERNAL)
        type.addAnnotation(emission.generated(plan))
        val json = PropertySpec.builder(JSON, Runtime.json, KModifier.PRIVATE)
        json.initializer("%T { encodeDefaults = true }", Runtime.json)
        type.addProperty(json.build())
        val sent = correlated(plan, Side.Client)
        val received = correlated(plan, Side.Server)
        (sent + received).distinctBy(::serializerName).forEach { message ->
            val payload = emission.payloadType(message)
            val property = PropertySpec.builder(serializerName(message), Runtime.kSerializer.parameterizedBy(payload))
            property.addModifiers(KModifier.PRIVATE)
            property.initializer("%M()", Runtime.serializer)
            type.addProperty(property.build())
        }
        type.addFunction(selector(plan = plan, side = Side.Client, name = "sent", messages = sent))
        type.addFunction(selector(plan = plan, side = Side.Server, name = "received", messages = received))
        sent.forEach { message -> type.addFunction(extractor(message, "Sent")) }
        received.forEach { message -> type.addFunction(extractor(message, "Received")) }
        type.addFunction(step())
        return type.build()
    }

    private fun correlated(plan: ChannelPlan, side: Side): List<MessagePlan> {
        val keys = plan.requests.flatMap { request -> keysOf(request, side) }.toSet()
        return side.of(plan).messages.filter { message -> message.key in keys && message.correlation != null }
    }

    private fun keysOf(request: RequestPlan, side: Side): List<String> = when (side) {
        Side.Client -> request.messages
        Side.Server -> request.replies
    }

    private fun selector(plan: ChannelPlan, side: Side, name: String, messages: List<MessagePlan>): FunSpec {
        val direction = side.of(plan)
        val function = FunSpec.builder(name).returns(STRING.copy(nullable = true))
        function.addParameter(MESSAGE, emission.directionType(plan, side))
        val suffix = name.replaceFirstChar(Char::uppercaseChar)
        val single = direction.messages.singleOrNull()
        if (!direction.isSealed) {
            val call = single?.takeIf { message -> message in messages }?.let { message ->
                CodeBlock.of("%N(%N)", extractorName(message, suffix), MESSAGE)
            }
            function.addStatement("return %L", call ?: CodeBlock.of("null"))
        }
        if (direction.isSealed) {
            val sealed = emission.messageType(plan, side)
            function.beginControlFlow("return when (%N)", MESSAGE)
            direction.messages.forEach { message ->
                val member = sealed.nestedClass(message.member)
                val call = CodeBlock.of("%N(%N.%N)", extractorName(message, suffix), MESSAGE, PAYLOAD)
                val value = call.takeIf { message in messages } ?: CodeBlock.of("null")
                function.addStatement("is %T -> %L", member, value)
            }
            function.endControlFlow()
        }
        return function.build()
    }

    private fun extractor(message: MessagePlan, suffix: String): FunSpec {
        val function = FunSpec.builder(extractorName(message, suffix)).addModifiers(KModifier.PRIVATE)
        function.addParameter(PAYLOAD, emission.payloadType(message))
        function.returns(STRING.copy(nullable = true))
        function.addStatement("val root = %N.encodeToJsonElement(%N, %N)", JSON, serializerName(message), PAYLOAD)
        val found = message.correlation.orEmpty().fold(CodeBlock.of("root")) { inner, token ->
            CodeBlock.of("%N(%L, %L)", STEP, inner, Texts.literal(token))
        }
        function.addStatement("val found = %L", found)
        function.addStatement("return (found as? %T)?.%M", Runtime.jsonPrimitive, Runtime.contentOrNull)
        return function.build()
    }

    private fun step(): FunSpec {
        val element = Runtime.jsonElement.copy(nullable = true)
        val function = FunSpec.builder(STEP).addModifiers(KModifier.PRIVATE)
        function.returns(element)
        function.addParameter("element", element)
        function.addParameter("token", STRING)
        function.addStatement("val fields = element as? %T", Runtime.jsonObject)
        function.addStatement("val items = element as? %T", Runtime.jsonArray)
        function.addStatement("val index = token.toIntOrNull()")
        function.addStatement("return fields?.get(token) ?: index?.let { position -> items?.getOrNull(position) }")
        return function.build()
    }

    private fun serializerName(message: MessagePlan): String = Identifiers.member(message.member) + "Serializer"

    private fun extractorName(message: MessagePlan, suffix: String): String =
        Identifiers.member(message.member) + suffix
}
