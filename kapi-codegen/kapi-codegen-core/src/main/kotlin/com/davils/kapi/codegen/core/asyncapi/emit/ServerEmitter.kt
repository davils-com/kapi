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
import com.davils.kapi.codegen.core.asyncapi.plan.Side
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

private const val SESSION: String = "session"

private const val MESSAGE: String = "message"

private const val PAYLOAD: String = "payload"

private const val CODE: String = "code"

private const val REASON: String = "reason"

private const val CLOSED: String =
    "@throws kotlinx.coroutines.channels.ClosedSendChannelException if the channel is closed."

internal class ServerEmitter(private val emission: Emission) {
    fun serviceType(plan: ChannelPlan): ClassName = ClassName(emission.serverPackage, plan.base + "Service")

    fun sessionType(plan: ChannelPlan): ClassName = ClassName(emission.serverPackage, plan.base + "ServerSession")

    fun runtimeSession(plan: ChannelPlan): TypeName = Runtime.serverSession.parameterizedBy(
        emission.directionType(plan, Side.Client),
        emission.directionType(plan, Side.Server),
    )

    fun emit(plan: ChannelPlan): List<GeneratedFile> = listOf(
        emission.file(emission.serverPackage, service(plan), GeneratedPlatform.Jvm),
        emission.file(emission.serverPackage, session(plan), GeneratedPlatform.Jvm),
    )

    private fun service(plan: ChannelPlan): TypeSpec {
        val type = TypeSpec.interfaceBuilder(serviceType(plan))
        type.addModifiers(emission.modifier)
        type.addKdoc(
            "%L",
            Specs.kdoc(
                Kdocs.channel(plan, "Serves"),
                "",
                "The server receives what the client sends and sends what the client receives, as the operations of " +
                    "the document describe it. The routes call [serve] once per channel that opened, after the " +
                    "handshake was authorized and its parameters were read.",
            ),
        )
        type.addAnnotation(emission.generated(plan))
        val serve = FunSpec.builder("serve").addModifiers(KModifier.PUBLIC, KModifier.SUSPEND, KModifier.ABSTRACT)
        serve.addKdoc(
            "%L",
            "Serves one open channel until it closes; a failure closes the channel with 1011.\n" +
                "@param session the open session.",
        )
        serve.addParameter(SESSION, sessionType(plan))
        type.addFunction(serve.build())
        return type.build()
    }

    private fun session(plan: ChannelPlan): TypeSpec {
        val received = emission.directionType(plan, Side.Client)
        val sent = emission.directionType(plan, Side.Server)
        val type = TypeSpec.classBuilder(sessionType(plan))
        type.addModifiers(emission.modifier)
        type.addAnnotation(emission.generated(plan))
        val parameters = plan.parameters.map { parameter ->
            Triple(parameter.member, STRING as TypeName, "The parameter ${Kdocs.code(parameter.name)} of the address.")
        }
        val fields = plan.handshake.map { field ->
            val type = Scalars.typeOf(field.type).copy(nullable = !field.isRequired)
            Triple(field.member, type, "The value of ${Kdocs.code(field.name)} in the handshake.")
        }
        type.addKdoc(
            "%L",
            Specs.kdoc(
                Kdocs.channel(plan, "An open session of"),
                "",
                "It is safe to send from several coroutines; receive from one.",
            ),
        )
        val constructor = FunSpec.constructorBuilder().addModifiers(KModifier.INTERNAL)
        constructor.addParameter(SESSION, runtimeSession(plan))
        type.addProperty(Specs.stored(SESSION, runtimeSession(plan), KModifier.PRIVATE))
        (parameters + fields).forEach { (name, value, kdoc) ->
            constructor.addParameter(name, value)
            val property = PropertySpec.builder(name, value, KModifier.PUBLIC).initializer(name)
            property.addKdoc("%L", kdoc)
            type.addProperty(property.build())
        }
        type.primaryConstructor(constructor.build())
        type.addProperty(Specs.delegated("request", Runtime.request, "The request that opened the channel."))
        val protocol = STRING.copy(nullable = true)
        type.addProperty(Specs.delegated("protocol", protocol, "The subprotocol both sides agreed on, or `null`."))
        type.addProperty(
            Specs.delegated("incoming", Runtime.flow.parameterizedBy(received), "The messages as they arrive."),
        )
        type.addFunction(receive(received))
        if (!Side.Server.of(plan).isEmpty) {
            type.addFunction(send(sent))
            sendHelpers(plan).forEach(type::addFunction)
        }
        type.addFunction(close())
        return type.build()
    }

    private fun receive(received: TypeName): FunSpec {
        val function = FunSpec.builder("receive").addModifiers(KModifier.PUBLIC, KModifier.SUSPEND)
        function.addKdoc(
            "%L",
            Specs.kdoc("Receives the next message.", "@return the message, or `null` once the channel is closed."),
        )
        function.returns(received.copy(nullable = true))
        function.addStatement("return %N.receive()", SESSION)
        return function.build()
    }

    private fun send(sent: TypeName): FunSpec {
        val function = FunSpec.builder("send").addModifiers(KModifier.PUBLIC, KModifier.SUSPEND)
        function.addKdoc(
            "%L",
            Specs.kdoc("Sends [message].", "@param message the message.", CLOSED),
        )
        function.addParameter(MESSAGE, sent)
        function.addStatement("%N.send(%N)", SESSION, MESSAGE)
        return function.build()
    }

    private fun sendHelpers(plan: ChannelPlan): List<FunSpec> {
        val direction = Side.Server.of(plan)
        if (!direction.isSealed) return emptyList()
        return direction.messages.map { message ->
            val function = FunSpec.builder("send" + message.member)
            function.addModifiers(KModifier.PUBLIC, KModifier.SUSPEND)
            function.addKdoc(
                "%L",
                Specs.kdoc(
                    "Sends the message ${Kdocs.code(message.key)} with [payload].",
                    "@param payload the payload.",
                    CLOSED,
                ),
            )
            function.addParameter(PAYLOAD, emission.payloadType(message))
            val wrapped = emission.wrapped(plan = plan, side = Side.Server, message = message, value = PAYLOAD)
            function.addStatement("%N.send(%L)", SESSION, wrapped)
            function.build()
        }
    }

    private fun close(): FunSpec {
        val function = FunSpec.builder("close").addModifiers(KModifier.PUBLIC, KModifier.SUSPEND)
        function.addKdoc(
            "%L",
            Specs.kdoc(
                "Closes the channel with [code] and [reason].",
                "@param code the close code of RFC 6455; 1000 by default.",
                "@param reason the reason, at most 123 bytes of UTF-8 without control characters.",
                "@throws IllegalArgumentException if the reason is too long or holds a control character.",
            ),
        )
        function.addParameter(Specs.parameter(CODE, SHORT, CodeBlock.of("%T.NORMAL", Runtime.serverSession)))
        function.addParameter(Specs.parameter(REASON, STRING, CodeBlock.of("%S", "")))
        function.addStatement("%N.close(%N, %N)", SESSION, CODE, REASON)
        return function.build()
    }
}
