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
import com.davils.kapi.codegen.core.asyncapi.plan.HandshakeField
import com.davils.kapi.codegen.core.asyncapi.plan.HandshakePlace
import com.davils.kapi.codegen.core.asyncapi.plan.PathPart
import com.davils.kapi.codegen.core.asyncapi.plan.RequestPlan
import com.davils.kapi.codegen.core.asyncapi.plan.ScalarType
import com.davils.kapi.codegen.core.asyncapi.plan.SchemeKind
import com.davils.kapi.codegen.core.asyncapi.plan.SchemeUsePlan
import com.davils.kapi.codegen.core.asyncapi.plan.Side
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.joinToCode

private const val CLIENT: String = "client"

private const val SPEC: String = "spec"

private const val SESSION: String = "session"

private const val CONFIGURE: String = "configure"

private const val BLOCK: String = "block"

private const val MESSAGE: String = "message"

private const val PAYLOAD: String = "payload"

private const val TIMEOUT: String = "timeout"

private const val CODE: String = "code"

private const val REASON: String = "reason"

private const val WRITTEN: String = "@return `false` if it cannot be written or the channel is closed."

internal class ClientEmitter(private val emission: Emission, private val correlations: CorrelationEmitter) {
    fun emit(plan: ChannelPlan): List<GeneratedFile> {
        val files = listOf(
            emission.file(emission.clientPackage, connector(plan), GeneratedPlatform.Common),
            emission.file(emission.clientPackage, session(plan), GeneratedPlatform.Common),
        )
        return correlations.emit(plan) + files
    }

    private fun sessionType(plan: ChannelPlan): ClassName =
        ClassName(emission.clientPackage, plan.base + "ClientSession")

    private fun runtimeSession(plan: ChannelPlan): TypeName = Runtime.clientSession.parameterizedBy(
        emission.directionType(plan, Side.Client),
        emission.directionType(plan, Side.Server),
    )

    private fun connector(plan: ChannelPlan): TypeSpec {
        val type = TypeSpec.classBuilder(ClassName(emission.clientPackage, plan.base + "Channel"))
        type.addModifiers(emission.modifier)
        type.addKdoc("%L", connectorKdoc(plan))
        type.addAnnotation(emission.generated(plan))
        val settings = LambdaTypeName.get(receiver = Runtime.channelSettingsBuilder, returnType = UNIT)
        val constructor = FunSpec.constructorBuilder()
        constructor.addParameter(CLIENT, Runtime.apiClient)
        constructor.addParameter(Specs.parameter(CONFIGURE, settings, CodeBlock.of("{}")))
        type.primaryConstructor(constructor.build())
        type.addProperty(Specs.stored(CLIENT, Runtime.apiClient, KModifier.PRIVATE))
        type.addProperty(spec(plan))
        type.addFunction(connect(plan))
        return type.build()
    }

    private fun connectorKdoc(plan: ChannelPlan): String {
        val lines = listOf(
            Kdocs.channel(plan, "Opens"),
            "The client sends what the server receives and receives what the server sends: the operations of the " +
                "document describe the server. [connect] opens the channel with its parameters, the credentials " +
                "of its security requirement and the subprotocols of the document, runs the block with the open " +
                "session and closes the channel with 1000 when the block returns.",
            "@param client the client the channel opens on, with its base URL, credentials and destination policy.",
            "@param configure changes the settings of the channel, such as its limits, after the subprotocols of " +
                "the document are set.",
        )
        return lines.joinToString(separator = "\n\n")
    }

    private fun spec(plan: ChannelPlan): PropertySpec {
        val sent = emission.directionType(plan, Side.Client)
        val received = emission.directionType(plan, Side.Server)
        val type = Runtime.channelSpec.parameterizedBy(sent, received)
        val code = CodeBlock.builder()
        code.add("%T(\n⇤", Runtime.channelSpec)
        code.add("sendSerializer = %L,\n", emission.serializer(plan, Side.Client))
        code.add("receiveDeserializer = %L,\n", emission.serializer(plan, Side.Server))
        code.add("settings = %L,\n", settings(plan))
        if (plan.hasCorrelation) {
            val correlation = correlations.typeName(plan)
            code.add(
                "correlation = %T(sent = %T::sent, received = %T::received),\n",
                Runtime.correlation,
                correlation,
                correlation,
            )
        }
        code.add("⇤)⇥⇥")
        return Specs.initialized(name = SPEC, type = type, initializer = code.build(), modifier = KModifier.PRIVATE)
    }

    private fun settings(plan: ChannelPlan): CodeBlock {
        if (plan.subprotocols.isEmpty()) return CodeBlock.of("%M(%N)", Runtime.channelSettings, CONFIGURE)
        val code = CodeBlock.builder()
        code.add("%M {\n", Runtime.channelSettings).indent()
        code.add("subprotocols = listOf(%L)\n", literals(plan.subprotocols))
        code.add("%N()\n", CONFIGURE)
        code.unindent().add("}")
        return code.build()
    }

    private fun connect(plan: ChannelPlan): FunSpec {
        val result = TypeVariableName("T")
        val block = LambdaTypeName.get(receiver = sessionType(plan), returnType = result).copy(suspending = true)
        val function = FunSpec.builder("connect")
        function.addModifiers(KModifier.PUBLIC, KModifier.SUSPEND)
        function.addTypeVariable(result)
        function.addKdoc("%L", connectKdoc(plan))
        function.returns(Runtime.channelOutcome.parameterizedBy(result))
        plan.parameters.forEach { parameter -> function.addParameter(pathParameter(parameter)) }
        val fields = plan.handshake.sortedBy { field -> !field.isRequired }
        fields.forEach { field -> function.addParameter(fieldParameter(field)) }
        function.addParameter(BLOCK, block)
        val call = CodeBlock.builder()
        call.add("val call = %M(operation = %L, method = %S) {\n", Runtime.apiCall, Texts.literal(plan.key), "GET")
        call.indent()
        plan.path.forEach { part -> call.add(pathCode(part)) }
        fields.forEach { field -> call.add(fieldCode(field)) }
        plan.security.forEach { alternative -> call.add(securityCode(alternative)) }
        call.unindent().add("}\n")
        function.addCode(call.build())
        function.addStatement(
            "return %N.%M(call, %N) { %T(this).%N() }",
            CLIENT,
            Runtime.channel,
            SPEC,
            sessionType(plan),
            BLOCK,
        )
        return function.build()
    }

    private fun connectKdoc(plan: ChannelPlan): String {
        val parameters = plan.parameters.map { parameter ->
            "@param ${parameter.member} the parameter ${Kdocs.code(parameter.name)} of the address."
        }
        val fields = plan.handshake.map { field ->
            val place = "query parameter".takeIf { field.place == HandshakePlace.Query } ?: "header"
            "@param ${field.member} the $place ${Kdocs.code(field.name)} of the handshake."
        }
        val head = "Opens the channel, runs [block] with the open session and closes the channel."
        val tail = listOf(
            "@param block what to do with the open channel.",
            "@return the value of the block with how the channel closed, or why it did not open.",
        )
        val lines = listOf(head) + parameters + fields + tail
        return lines.joinToString(separator = "\n")
    }

    private fun pathParameter(parameter: PathPart.Parameter): ParameterSpec {
        val spec = ParameterSpec.builder(parameter.member, STRING)
        parameter.default?.let { value -> spec.defaultValue("%L", Texts.literal(value)) }
        return spec.build()
    }

    private fun fieldParameter(field: HandshakeField): ParameterSpec {
        val type = Scalars.typeOf(field.type).copy(nullable = !field.isRequired)
        val spec = ParameterSpec.builder(field.member, type)
        if (!field.isRequired) spec.defaultValue("null")
        return spec.build()
    }

    private fun pathCode(part: PathPart): CodeBlock = when (part) {
        is PathPart.Literal -> CodeBlock.of("path(%L)\n", Texts.literal(part.text))

        is PathPart.Parameter -> CodeBlock.of(
            "path(%T(name = %L, style = %T.Simple, isExploded = false), %T.Text(%N))\n",
            Runtime.parameterFormat,
            Texts.literal(part.name),
            Runtime.parameterStyle,
            Runtime.parameterValue,
            part.member,
        )
    }

    private fun fieldCode(field: HandshakeField): CodeBlock {
        val function = "query".takeIf { field.place == HandshakePlace.Query } ?: "header"
        val variable = field.member.takeIf { field.isRequired } ?: VALUE
        val text = CodeBlock.of("%N", variable).takeIf { field.type == ScalarType.Text }
            ?: CodeBlock.of("%N.toString()", variable)
        val written = CodeBlock.of("%N(%L, %L)", function, Texts.literal(field.name), text)
        if (field.isRequired) return CodeBlock.of("%L\n", written)
        return CodeBlock.of("%N?.let { %N -> %L }\n", field.member, VALUE, written)
    }

    private fun securityCode(alternative: List<SchemeUsePlan>): CodeBlock {
        val uses = alternative.mapNotNull { use ->
            val place = placement(use)
            place?.let { found -> CodeBlock.of("%T(%L, %L)", Runtime.securityUse, Texts.literal(use.scheme), found) }
        }
        return CodeBlock.of("security(listOf(%L))\n", uses.joinToCode(", "))
    }

    private fun placement(use: SchemeUsePlan): CodeBlock? = when (use.kind) {
        SchemeKind.Bearer -> CodeBlock.of("%T.Bearer", Runtime.credentialPlacement)
        SchemeKind.Basic -> CodeBlock.of("%T.Basic", Runtime.credentialPlacement)
        SchemeKind.Header -> CodeBlock.of("%T.Header(%L)", Runtime.credentialPlacement, Texts.literal(use.field))
        SchemeKind.Query -> CodeBlock.of("%T.Query(%L)", Runtime.credentialPlacement, Texts.literal(use.field))
        SchemeKind.Cookie -> CodeBlock.of("%T.Cookie(%L)", Runtime.credentialPlacement, Texts.literal(use.field))
        SchemeKind.Certificate -> null
    }

    private fun session(plan: ChannelPlan): TypeSpec {
        val sent = emission.directionType(plan, Side.Client)
        val received = emission.directionType(plan, Side.Server)
        val type = TypeSpec.classBuilder(sessionType(plan))
        type.addModifiers(emission.modifier)
        type.addKdoc("%L", sessionKdoc(plan))
        type.addAnnotation(emission.generated(plan))
        val constructor = FunSpec.constructorBuilder().addModifiers(KModifier.INTERNAL)
        constructor.addParameter(SESSION, runtimeSession(plan))
        type.primaryConstructor(constructor.build())
        type.addProperty(Specs.stored(SESSION, runtimeSession(plan), KModifier.PRIVATE))
        val subprotocol = STRING.copy(nullable = true)
        type.addProperty(Specs.delegated("subprotocol", subprotocol, "The subprotocol the server chose."))
        type.addProperty(
            Specs.delegated(
                "incoming",
                Runtime.flow.parameterizedBy(received),
                "The messages the server sends that are no reply to a request, in their order; collected once.",
            ),
        )
        if (!Side.Client.of(plan).isEmpty) {
            type.addFunction(send(sent))
            sendHelpers(plan).forEach(type::addFunction)
        }
        plan.requests.forEach { request -> type.addFunction(request(plan, request, received)) }
        type.addFunction(close())
        return type.build()
    }

    private fun sessionKdoc(plan: ChannelPlan): String = Specs.kdoc(
        Kdocs.channel(plan, "An open session of"),
        "",
        "It is safe to use from several coroutines inside the block of `connect` and must not be used after it.",
    )

    private fun send(sent: TypeName): FunSpec {
        val function = FunSpec.builder("send").addModifiers(KModifier.PUBLIC, KModifier.SUSPEND)
        function.addKdoc(
            "%L",
            Specs.kdoc(
                "Sends [message], suspending while the transport cannot take it.",
                "@param message the message.",
                WRITTEN,
            ),
        )
        function.addParameter(MESSAGE, sent)
        function.returns(BOOLEAN)
        function.addStatement("return %N.send(%N)", SESSION, MESSAGE)
        return function.build()
    }

    private fun sendHelpers(plan: ChannelPlan): List<FunSpec> {
        val direction = Side.Client.of(plan)
        if (!direction.isSealed) return emptyList()
        return direction.messages.map { message ->
            val function = FunSpec.builder("send" + message.member)
            function.addModifiers(KModifier.PUBLIC, KModifier.SUSPEND)
            function.addKdoc(
                "%L",
                Specs.kdoc(
                    "Sends the message ${Kdocs.code(message.key)} with [payload].",
                    "@param payload the payload.",
                    WRITTEN,
                ),
            )
            function.addParameter(PAYLOAD, emission.payloadType(message))
            function.returns(BOOLEAN)
            val wrapped = emission.wrapped(plan = plan, side = Side.Client, message = message, value = PAYLOAD)
            function.addStatement("return %N.send(%L)", SESSION, wrapped)
            function.build()
        }
    }

    private fun request(plan: ChannelPlan, request: RequestPlan, received: TypeName): FunSpec {
        val keys = request.messages.singleOrNull()
        val sent = Side.Client.of(plan)
        val single = sent.messages.firstOrNull { message -> message.key == keys }
        val function = FunSpec.builder(request.function).addModifiers(KModifier.PUBLIC, KModifier.SUSPEND)
        function.addKdoc(
            "%L",
            Specs.kdoc(
                "Sends the request of the operation ${Kdocs.code(request.operation)} and waits for the first reply " +
                    "with its correlation id.",
                "@param ${MESSAGE.takeIf { single == null } ?: PAYLOAD} the request.",
                "@param timeout how long to wait for the reply.",
                "@return the reply, or why there is none.",
            ),
        )
        val argument = single?.let { message ->
            emission.wrapped(plan = plan, side = Side.Client, message = message, value = PAYLOAD)
        }
        val parameter = single?.let { message -> ParameterSpec.builder(PAYLOAD, emission.payloadType(message)) }
            ?: ParameterSpec.builder(MESSAGE, emission.directionType(plan, Side.Client))
        function.addParameter(parameter.build())
        function.addParameter(TIMEOUT, Runtime.duration)
        function.returns(Runtime.channelReply.parameterizedBy(received))
        function.addStatement(
            "return %N.request(%L, %N)",
            SESSION,
            argument ?: CodeBlock.of("%N", MESSAGE),
            TIMEOUT,
        )
        return function.build()
    }

    private fun close(): FunSpec {
        val function = FunSpec.builder("close").addModifiers(KModifier.PUBLIC, KModifier.SUSPEND)
        function.addKdoc(
            "%L",
            Specs.kdoc(
                "Closes the channel with [code] and [reason].",
                "@param code the close code: 1000 or one of 3000 to 4999.",
                "@param reason the reason, at most 123 bytes of UTF-8.",
                "@throws IllegalArgumentException if the code or the reason cannot be sent.",
            ),
        )
        function.addParameter(Specs.parameter(CODE, INT, CodeBlock.of("%T.Normal.code", Runtime.closeKind)))
        function.addParameter(Specs.parameter(REASON, STRING, CodeBlock.of("%S", "")))
        function.addStatement("%N.close(%N, %N)", SESSION, CODE, REASON)
        return function.build()
    }

    private fun literals(texts: List<String>): CodeBlock = texts.map(Texts::literal).joinToCode(", ")

    private companion object {
        const val VALUE: String = "value"
    }
}
