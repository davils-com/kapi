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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

private const val MESSAGE: String = "message"

private const val ELEMENT: String = "element"

private const val JSON: String = "json"

private const val STEP: String = "step"

private const val TAG: String = "tagOf"

private const val ID: String = "idAt"

private const val TOKENS: String = "tokens"

private const val RETURN: String = "return %L"

internal class CorrelationEmitter(private val emission: Emission) {
    fun typeName(plan: ChannelPlan): ClassName = ClassName(emission.channelPackage, plan.base + "Correlation")

    fun emit(plan: ChannelPlan): List<GeneratedFile> {
        if (!plan.hasCorrelation) return emptyList()
        return listOf(emission.file(emission.channelPackage, type(plan), GeneratedPlatform.Common))
    }

    fun idOf(plan: ChannelPlan, side: Side): CodeBlock =
        CodeBlock.of("{ %N -> %T.%N(%N) }", ELEMENT, typeName(plan), idName(side), ELEMENT)

    fun messageIdOf(plan: ChannelPlan, side: Side): CodeBlock =
        CodeBlock.of("%T::%N", typeName(plan), messageIdName(side))

    private fun type(plan: ChannelPlan): TypeSpec {
        val type = TypeSpec.objectBuilder(typeName(plan))
        type.addModifiers(KModifier.INTERNAL)
        type.addAnnotation(emission.generated(plan))
        val json = PropertySpec.builder(JSON, Runtime.json, KModifier.PRIVATE)
        json.initializer("%T { encodeDefaults = true }", Runtime.json)
        type.addProperty(json.build())
        val correlated = Side.entries.associateWith { side -> correlated(plan, side) }
        val isTagged = Side.entries.any { side -> side.of(plan).isSealed && correlated.getValue(side).isNotEmpty() }
        Side.entries.forEach { side ->
            val messages = correlated.getValue(side)
            if (side.of(plan).isSealed && messages.isNotEmpty()) type.addProperty(paths(side, messages))
            type.addFunction(id(plan, side, messages))
            if (!side.of(plan).isEmpty) type.addFunction(messageId(plan, side))
        }
        if (isTagged) type.addFunction(tag())
        if (correlated.values.any(List<MessagePlan>::isNotEmpty)) type.addFunction(idAt())
        type.addFunction(step())
        return type.build()
    }

    private fun correlated(plan: ChannelPlan, side: Side): List<MessagePlan> {
        val keys = plan.requests.flatMap { request -> keysOf(request, side) }.toSet()
        return side.of(plan).messages.filter { message -> message.key in keys && message.correlation != null }
    }

    private fun keysOf(request: RequestPlan, side: Side): List<String> {
        if (request.sender == side) return request.messages
        return request.replies
    }

    private fun paths(side: Side, messages: List<MessagePlan>): PropertySpec {
        val entries = messages.map { message ->
            CodeBlock.of("%L to %L", Texts.literal(message.tag.orEmpty()), path(message))
        }
        val type = MAP.parameterizedBy(STRING, LIST.parameterizedBy(STRING))
        val property = PropertySpec.builder(pathsName(side), type, KModifier.PRIVATE)
        property.initializer("mapOf(%L)", entries.joinToCode(", "))
        return property.build()
    }

    private fun path(message: MessagePlan): CodeBlock {
        val tokens = message.correlation.orEmpty().map(Texts::literal)
        return CodeBlock.of("listOf(%L)", tokens.joinToCode(", "))
    }

    private fun id(plan: ChannelPlan, side: Side, messages: List<MessagePlan>): FunSpec {
        val direction = side.of(plan)
        val function = FunSpec.builder(idName(side))
        function.addParameter(ELEMENT, Runtime.jsonElement)
        function.returns(STRING.copy(nullable = true))
        val single = messages.singleOrNull()?.takeIf { !direction.isSealed }
        val tagged = direction.discriminator?.takeIf { direction.isSealed && messages.isNotEmpty() }
        val plain = single?.let { message -> CodeBlock.of("%N(%N, %L)", ID, ELEMENT, path(message)) }
        val chosen = tagged?.let { property ->
            CodeBlock.of(
                "%N[%N(%N, %L)]?.let { %N -> %N(%N, %N) }",
                pathsName(side),
                TAG,
                ELEMENT,
                Texts.literal(property),
                TOKENS,
                ID,
                ELEMENT,
                TOKENS,
            )
        }
        function.addStatement(RETURN, plain ?: chosen ?: CodeBlock.of("null"))
        return function.build()
    }

    private fun messageId(plan: ChannelPlan, side: Side): FunSpec {
        val function = FunSpec.builder(messageIdName(side))
        function.addParameter(MESSAGE, emission.directionType(plan, side))
        function.returns(STRING.copy(nullable = true))
        val element = CodeBlock.of("%N.encodeToJsonElement(%L, %N)", JSON, emission.serializer(plan, side), MESSAGE)
        function.addStatement("return %N(%L)", idName(side), element)
        return function.build()
    }

    private fun tag(): FunSpec {
        val function = FunSpec.builder(TAG).addModifiers(KModifier.PRIVATE)
        function.returns(STRING.copy(nullable = true))
        function.addParameter(ELEMENT, Runtime.jsonElement)
        function.addParameter("property", STRING)
        function.addStatement("val fields = %N as? %T", ELEMENT, Runtime.jsonObject)
        function.addStatement("val tag = fields?.get(property) as? %T", Runtime.jsonPrimitive)
        function.addStatement("return tag?.takeIf { primitive -> primitive.isString }?.content")
        return function.build()
    }

    private fun idAt(): FunSpec {
        val function = FunSpec.builder(ID).addModifiers(KModifier.PRIVATE)
        function.returns(STRING.copy(nullable = true))
        function.addParameter(ELEMENT, Runtime.jsonElement)
        function.addParameter(TOKENS, LIST.parameterizedBy(STRING))
        function.addStatement(
            "val found = %N.fold<%T, %T>(%N) { inner, token -> %N(inner, token) }",
            TOKENS,
            STRING,
            Runtime.jsonElement.copy(nullable = true),
            ELEMENT,
            STEP,
        )
        function.addStatement("return (found as? %T)?.%M", Runtime.jsonPrimitive, Runtime.contentOrNull)
        return function.build()
    }

    private fun step(): FunSpec {
        val element = Runtime.jsonElement.copy(nullable = true)
        val function = FunSpec.builder(STEP).addModifiers(KModifier.PRIVATE)
        function.returns(element)
        function.addParameter(ELEMENT, element)
        function.addParameter("token", STRING)
        function.addStatement("val fields = %N as? %T", ELEMENT, Runtime.jsonObject)
        function.addStatement("val items = %N as? %T", ELEMENT, Runtime.jsonArray)
        function.addStatement("val index = token.toIntOrNull()")
        function.addStatement("return fields?.get(token) ?: index?.let { position -> items?.getOrNull(position) }")
        return function.build()
    }

    private fun idName(side: Side): String = side.label.lowercase() + "Id"

    private fun messageIdName(side: Side): String = side.label.lowercase() + "MessageId"

    private fun pathsName(side: Side): String = side.label.lowercase() + "Paths"
}
