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
import com.davils.kapi.codegen.core.asyncapi.plan.SchemeKind
import com.davils.kapi.codegen.core.asyncapi.plan.SchemeUsePlan
import com.davils.kapi.codegen.core.asyncapi.plan.Side
import com.davils.kapi.codegen.core.naming.Identifiers
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

private const val SESSION: String = "session"

private const val LIMITS: String = "limits"

private const val REQUEST: String = "request"

private const val TEXT_SUFFIX: String = "Text"

private const val QUERY: String = "%N.query.firstOrNull { entry -> entry.first == %L }?.second"

private const val REFUSED: String = "The handshake lacks a parameter or carries an invalid one"

internal class RoutesEmitter(private val emission: Emission, private val servers: ServerEmitter) {
    fun emit(name: String, plans: List<ChannelPlan>): List<GeneratedFile> {
        if (plans.isEmpty()) return emptyList()
        return listOf(emission.file(emission.serverPackage, routes(name, plans), GeneratedPlatform.Jvm))
    }

    private fun serviceName(plan: ChannelPlan): String = Identifiers.member(plan.base) + "Service"

    private fun routes(name: String, plans: List<ChannelPlan>): TypeSpec {
        val type = TypeSpec.classBuilder(ClassName(emission.serverPackage, name))
        type.addModifiers(emission.modifier)
        type.addSuperinterface(Runtime.routes)
        type.addAnnotation(emission.generated(""))
        type.addKdoc("%L", kdoc(plans))
        val constructor = FunSpec.constructorBuilder()
        plans.forEach { plan ->
            constructor.addParameter(serviceName(plan), servers.serviceType(plan))
            type.addProperty(Specs.stored(serviceName(plan), servers.serviceType(plan), KModifier.PRIVATE))
        }
        val limits = CodeBlock.of("%T.Default", Runtime.channelLimits)
        constructor.addParameter(Specs.parameter(LIMITS, Runtime.channelLimits, limits))
        type.addProperty(Specs.stored(LIMITS, Runtime.channelLimits, KModifier.PRIVATE))
        type.primaryConstructor(constructor.build())
        val operations = PropertySpec.builder("operations", LIST.parameterizedBy(Runtime.operation), KModifier.OVERRIDE)
        operations.initializer("emptyList()")
        type.addProperty(operations.build())
        val endpoints = LIST.parameterizedBy(Runtime.channelEndpoint.parameterizedBy(STAR, STAR))
        val channels = PropertySpec.builder("channels", endpoints, KModifier.OVERRIDE)
        val code = CodeBlock.builder()
        code.add("listOf(\n⇤")
        plans.forEach { plan -> code.add("%L,\n", endpoint(plan)) }
        code.add("⇤)⇥⇥")
        channels.initializer(code.build())
        type.addProperty(channels.build())
        plans.forEach { plan -> type.addFunction(handler(plan)) }
        return type.build()
    }

    private fun kdoc(plans: List<ChannelPlan>): String {
        val channels = plans.joinToString { plan -> Kdocs.code(plan.key) }
        val parameters = plans.map { plan ->
            "@param ${serviceName(plan)} serves the channel ${Kdocs.code(plan.key)}."
        }
        val head = listOf(
            "The WebSocket channels $channels, mounted with `routing { mount(routes) }` below the path of the server.",
            "",
            "Every handshake is authorized by the security requirement of its channel before the WebSocket opens. " +
                "Its parameters are read after it opened: a handshake that lacks a required one or carries one " +
                "that is invalid closes the channel with 1008 before the service sees it.",
            "",
        )
        val lines = head + parameters + listOf("@param limits the limits of every channel.")
        return lines.joinToString(separator = "\n")
    }

    private fun endpoint(plan: ChannelPlan): CodeBlock {
        val code = CodeBlock.builder()
        code.add("%T(\n", Runtime.channelEndpoint).indent()
        code.add("name = %L,\n", Texts.literal(plan.key))
        code.add("path = %L,\n", Texts.literal(plan.template))
        code.add("receive = %T(%L),\n", Runtime.messageCodec, emission.serializer(plan, Side.Client))
        code.add("send = %T(%L),\n", Runtime.messageCodec, emission.serializer(plan, Side.Server))
        code.add("protocols = listOf(%L),\n", plan.subprotocols.map(Texts::literal).joinToCode(", "))
        val alternatives = plan.security.map { alternative ->
            CodeBlock.of("listOf(%L)", alternative.map(::schemeUse).joinToCode(", "))
        }
        code.add("security = listOf(%L),\n", alternatives.joinToCode(", "))
        code.add("limits = %N,\n", LIMITS)
        code.add("handler = { %N(this) },\n", handlerName(plan))
        code.unindent().add(")")
        return code.build()
    }

    private fun schemeUse(use: SchemeUsePlan): CodeBlock {
        val scopes = use.scopes.map(Texts::literal).joinToCode(", ")
        return CodeBlock.of("%T(%L, %L, setOf(%L))", Runtime.schemeUse, Texts.literal(use.scheme), source(use), scopes)
    }

    private fun source(use: SchemeUsePlan): CodeBlock = when (use.kind) {
        SchemeKind.Bearer -> CodeBlock.of("%T.Bearer", Runtime.credentialSource)
        SchemeKind.Basic -> CodeBlock.of("%T.Basic", Runtime.credentialSource)
        SchemeKind.Header -> CodeBlock.of("%T.Header(%L)", Runtime.credentialSource, Texts.literal(use.field))
        SchemeKind.Query -> CodeBlock.of("%T.Query(%L)", Runtime.credentialSource, Texts.literal(use.field))
        SchemeKind.Cookie -> CodeBlock.of("%T.Cookie(%L)", Runtime.credentialSource, Texts.literal(use.field))
        SchemeKind.Certificate -> CodeBlock.of("%T.Certificate", Runtime.credentialSource)
    }

    private fun handlerName(plan: ChannelPlan): String = "serve" + plan.base

    private fun handler(plan: ChannelPlan): FunSpec {
        val function = FunSpec.builder(handlerName(plan)).addModifiers(KModifier.PRIVATE, KModifier.SUSPEND)
        function.addParameter(SESSION, servers.runtimeSession(plan))
        function.addStatement("val %N = %N.request", REQUEST, SESSION)
        plan.parameters.forEach { parameter ->
            function.addStatement(
                "val %N = %N.pathParameters[%L].orEmpty()",
                parameter.member,
                REQUEST,
                Texts.literal(parameter.name),
            )
        }
        plan.handshake.forEach { field -> function.addCode(read(field)) }
        val refusals = refusals(plan)
        if (refusals.isNotEmpty()) {
            function.addStatement(
                "if (%L) return %N.close(%T.POLICY_VIOLATION, %S)",
                refusals.joinToCode(" || "),
                SESSION,
                Runtime.serverSession,
                REFUSED,
            )
        }
        val arguments = (plan.parameters.map { parameter -> parameter.member } + plan.handshake.map { it.member })
            .map { name -> CodeBlock.of("%N = %N", name, name) }
        val all = listOf(CodeBlock.of("%N = %N", SESSION, SESSION)) + arguments
        function.addStatement(
            "%N.serve(%T(%L))",
            serviceName(plan),
            servers.sessionType(plan),
            all.joinToCode(", "),
        )
        return function.build()
    }

    private fun read(field: HandshakeField): CodeBlock {
        val parser = Scalars.parser(field.type)
        val raw = field.member.takeIf { parser == null } ?: (field.member + TEXT_SUFFIX)
        val name = Texts.literal(field.name)
        val source = when (field.place) {
            HandshakePlace.Query -> CodeBlock.of(QUERY, REQUEST, name)
            HandshakePlace.Header -> CodeBlock.of("%N.headers[%L]", REQUEST, name)
        }
        val code = CodeBlock.builder()
        code.addStatement("val %N = %L", raw, source)
        parser?.let { found -> code.addStatement("val %N = %N?.%N()", field.member, raw, found) }
        return code.build()
    }

    private fun refusals(plan: ChannelPlan): List<CodeBlock> {
        val paths = plan.parameters.filter { parameter -> parameter.choices.isNotEmpty() }.map { parameter ->
            CodeBlock.of("%N !in setOf(%L)", parameter.member, choices(parameter.choices))
        }
        val fields = plan.handshake.flatMap { field -> refusals(field) }
        return paths + fields
    }

    private fun refusals(field: HandshakeField): List<CodeBlock> {
        val isParsed = Scalars.parser(field.type) != null
        val missing = CodeBlock.of("%N == null", field.member).takeIf { field.isRequired }
        val invalid = CodeBlock.of("%N != null && %N == null", field.member + TEXT_SUFFIX, field.member)
        val present = CodeBlock.of("%N != null && ", field.member).takeIf { !field.isRequired } ?: CodeBlock.of("")
        val chosen = CodeBlock.of("%L%N !in setOf(%L)", present, field.member, choices(field.choices))
        val isChosen = !isParsed && field.choices.isNotEmpty()
        val isUnchecked = isParsed && !field.isRequired
        return listOfNotNull(missing, invalid.takeIf { isUnchecked }, chosen.takeIf { isChosen })
    }

    private fun choices(values: List<String>): CodeBlock = values.map(Texts::literal).joinToCode(", ")
}
