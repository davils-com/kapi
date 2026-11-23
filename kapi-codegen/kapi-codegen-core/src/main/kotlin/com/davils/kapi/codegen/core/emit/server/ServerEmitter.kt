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


package com.davils.kapi.codegen.core.emit.server

import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.GeneratedFileKind
import com.davils.kapi.codegen.core.emit.Annotations
import com.davils.kapi.codegen.core.emit.TypeNames
import com.davils.kapi.codegen.core.emit.client.SerializerCode
import com.davils.kapi.codegen.core.emit.client.SignatureCode
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.SecurityPlan
import com.davils.kapi.codegen.core.options.GeneratorOptions
import com.davils.kapi.codegen.core.options.Visibility
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

private const val INDENT: String = "    "

private const val SERVICE: String = "service"

private const val SERVER: String = "server"

private const val ROUTES_SUFFIX: String = "Routes"

private val modifiers: Map<Visibility, KModifier> = mapOf(
    Visibility.Public to KModifier.PUBLIC,
    Visibility.Internal to KModifier.INTERNAL,
)

internal class ServerEmitter(
    options: GeneratorOptions,
    private val origin: String,
    private val kind: ServerKind = ServerKind.Server,
) {
    private val serverPackage: String = options.packageName + "." + kind.packageName

    private val operationPackage: String = options.packageName + ".operation"

    private val visibility: KModifier = modifiers.getValue(options.visibility)

    private val types: TypeNames = TypeNames(options.packageName + ".model")

    private val serializers: SerializerCode = SerializerCode(types)

    private val signatures: SignatureCode = SignatureCode(types)

    private val responses: ResponseEmitter = ResponseEmitter(types, visibility)

    private val inputs: InputCode = InputCode(serializers)

    private val replies: ReplyCode = ReplyCode(serializers)

    fun emit(operations: List<OperationPlan>): List<GeneratedFile> {
        val groups = operations.groupBy(OperationPlan::group).toSortedMap()
        val services = groups.map { (group, members) ->
            file(
                serverPackage,
                group + kind.service,
                service(group, members),
            )
        }
        val routes = groups.map { (group, members) ->
            file(
                serverPackage,
                group + ROUTES_SUFFIX,
                routes(group, members),
            )
        }
        val answers = operations.sortedBy(OperationPlan::result).map { operation ->
            val response = responseOf(operation)
            file(operationPackage, response.simpleName, responses.type(operation, response, origin))
        }
        return (routes + services).sortedBy(GeneratedFile::path) + answers
    }

    private fun responseOf(operation: OperationPlan): ClassName = ServerNames.response(operationPackage, operation)

    private fun service(group: String, operations: List<OperationPlan>): TypeSpec {
        val builder = TypeSpec.interfaceBuilder(group + kind.service)
        builder.addModifiers(visibility)
        builder.addAnnotation(Annotations.generated(origin))
        operations.sortedBy(OperationPlan::name).forEach { operation ->
            val function = FunSpec.builder(operation.name)
            function.addModifiers(listOf(KModifier.PUBLIC).filter { open -> open == visibility })
            function.addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
            signatures.taken(operation).forEach { (name, type) ->
                function.addParameter(signatures.parameter(name, type))
            }
            function.returns(responseOf(operation))
            builder.addFunction(function.build())
        }
        return builder.build()
    }

    private fun routes(group: String, operations: List<OperationPlan>): TypeSpec {
        val service = ClassName(serverPackage, group + kind.service)
        val sorted = operations.sortedBy(OperationPlan::name)
        val constructor = FunSpec.constructorBuilder()
        constructor.addParameter(SERVICE, service)
        constructor.addParameter(SERVER, ServerTypes.server)
        val builder = TypeSpec.classBuilder(group + ROUTES_SUFFIX)
        builder.addModifiers(visibility)
        builder.addAnnotation(Annotations.generated(origin))
        builder.primaryConstructor(constructor.build())
        builder.addProperty(held(SERVICE, service))
        builder.addProperty(held(SERVER, ServerTypes.server))
        builder.addFunction(install(sorted))
        sorted.forEach { operation -> builder.addFunction(handler(operation)) }
        return builder.build()
    }

    private fun held(name: String, type: ClassName): PropertySpec {
        val property = PropertySpec.builder(name, type, KModifier.PRIVATE)
        property.initializer("%N", name)
        return property.build()
    }

    private fun install(operations: List<OperationPlan>): FunSpec {
        val function = FunSpec.builder("install")
        function.addModifiers(visibility)
        function.addParameter("route", ServerTypes.route)
        operations.forEach { operation ->
            function.addCode("route.%M(server, %T(\n⇥", ServerTypes.bind, ServerTypes.operation)
            function.addCode("name = %S,\n", operation.name)
            function.addCode("method = %S,\n", operation.method)
            function.addCode("path = %S,\n", operation.path)
            function.addCode("security = listOf(%L),\n", operation.security.map(::alternative).joinToCode(", "))
            if (operation.isDelivery) function.addCode("isDelivery = true,\n")
            function.addCode("handler = ::%N,\n⇤))\n", operation.name)
        }
        return function.build()
    }

    private fun alternative(uses: List<SecurityPlan>): CodeBlock =
        CodeBlock.of("listOf(%L)", uses.map(::use).joinToCode(", "))

    private fun use(plan: SecurityPlan): CodeBlock {
        val scopes = plan.scopes.map { scope -> CodeBlock.of("%S", scope) }
        val all = scopes.joinToCode(", ")
        return CodeBlock.of("%T(%S, %L, setOf(%L))", ServerTypes.schemeUse, plan.scheme, source(plan), all)
    }

    private fun source(plan: SecurityPlan): CodeBlock {
        val name = plan.name ?: return CodeBlock.of("%T.%L", ServerTypes.source, plan.placement)
        return CodeBlock.of("%T.%L(%S)", ServerTypes.source, plan.placement, name)
    }

    private fun handler(operation: OperationPlan): FunSpec {
        val names = signatures.taken(operation).map { (name, _) -> CodeBlock.of("%N = %N", name, name) }
        val function = FunSpec.builder(operation.name)
        function.addModifiers(KModifier.PRIVATE, KModifier.SUSPEND)
        function.addParameter("request", ServerTypes.request)
        function.returns(ServerTypes.reply)
        function.addCode(inputs.of(operation))
        function.addCode(replies.of(operation, responseOf(operation), names.joinToCode(", ")))
        return function.build()
    }

    private fun file(packageName: String, name: String, type: TypeSpec): GeneratedFile {
        val file = FileSpec.builder(packageName, name)
        file.indent(INDENT)
        file.addType(type)
        return GeneratedFile(
            path = packageName.replace('.', '/') + "/" + name + ".kt",
            kind = GeneratedFileKind.Kotlin,
            content = file.build().toString(),
        )
    }
}
