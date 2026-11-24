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
import com.davils.kapi.codegen.core.GeneratedFileKind
import com.davils.kapi.codegen.core.GeneratedPlatform
import com.davils.kapi.codegen.core.asyncapi.GeneratorOptions
import com.davils.kapi.codegen.core.asyncapi.plan.ChannelPlan
import com.davils.kapi.codegen.core.asyncapi.plan.MessagePlan
import com.davils.kapi.codegen.core.asyncapi.plan.Side
import com.davils.kapi.codegen.core.asyncapi.schema.PayloadTypes
import com.davils.kapi.codegen.core.emit.Texts
import com.davils.kapi.codegen.core.options.Visibility
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.NOTHING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

private const val INDENT: String = "    "

private const val GENERATOR: String = "kapi-codegen-asyncapi"

private val modifiers: Map<Visibility, KModifier> = mapOf(
    Visibility.Public to KModifier.PUBLIC,
    Visibility.Internal to KModifier.INTERNAL,
)

internal class Emission(options: GeneratorOptions, private val origin: String, private val payloads: PayloadTypes) {
    val modifier: KModifier = modifiers.getValue(options.visibility)

    val channelPackage: String = options.packageName + ".channel"

    val clientPackage: String = options.packageName + ".client"

    val serverPackage: String = options.packageName + ".server"

    fun file(packageName: String, type: TypeSpec, platform: GeneratedPlatform): GeneratedFile {
        val name = type.name.orEmpty()
        val file = FileSpec.builder(packageName, name)
        file.indent(INDENT)
        file.addType(type)
        return GeneratedFile(
            path = packageName.replace(oldChar = '.', newChar = '/') + "/" + name + ".kt",
            kind = GeneratedFileKind.Kotlin,
            content = file.build().toString(),
            platform = platform,
        )
    }

    fun generated(plan: ChannelPlan): AnnotationSpec = generated(plan.location.pointer.text)

    fun generated(pointer: String): AnnotationSpec {
        val annotation = AnnotationSpec.builder(Runtime.generated)
        annotation.addMember("by = %S", GENERATOR)
        annotation.addMember("from = %L", Texts.literal("$origin#$pointer"))
        return annotation.build()
    }

    fun messageType(plan: ChannelPlan, side: Side): ClassName =
        ClassName(channelPackage, plan.base + side.label + "Message")

    fun serializerType(plan: ChannelPlan, side: Side): ClassName =
        ClassName(channelPackage, plan.base + side.label + "MessageSerializer")

    fun payloadType(message: MessagePlan): TypeName = payloads.typeOf(message)

    fun directionType(plan: ChannelPlan, side: Side): TypeName {
        val direction = side.of(plan)
        val single = direction.messages.singleOrNull()?.let(::payloadType)
        if (direction.isSealed) return messageType(plan, side)
        return single ?: NOTHING
    }

    fun serializer(plan: ChannelPlan, side: Side): CodeBlock {
        val direction = side.of(plan)
        val single = direction.messages.singleOrNull()?.let(::payloadType)
        if (direction.isSealed) return CodeBlock.of("%T", serializerType(plan, side))
        return single?.let { type -> CodeBlock.of("%M<%T>()", Runtime.serializer, type) }
            ?: CodeBlock.of("%M()", Runtime.nothingSerializer)
    }

    fun wrapped(plan: ChannelPlan, side: Side, message: MessagePlan, value: String): CodeBlock {
        val isSealed = side.of(plan).isSealed
        val member = messageType(plan, side).nestedClass(message.member)
        return CodeBlock.of("%T(%N)", member, value).takeIf { isSealed } ?: CodeBlock.of("%N", value)
    }
}
