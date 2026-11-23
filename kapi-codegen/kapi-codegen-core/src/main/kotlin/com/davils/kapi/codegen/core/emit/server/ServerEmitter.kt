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
import com.davils.kapi.codegen.core.GeneratedPlatform
import com.davils.kapi.codegen.core.emit.Annotations
import com.davils.kapi.codegen.core.emit.Docs
import com.davils.kapi.codegen.core.emit.Kotlin
import com.davils.kapi.codegen.core.emit.PatternTable
import com.davils.kapi.codegen.core.emit.Sources
import com.davils.kapi.codegen.core.emit.TypeNames
import com.davils.kapi.codegen.core.emit.ValidationCode
import com.davils.kapi.codegen.core.emit.client.SerializerCode
import com.davils.kapi.codegen.core.operation.plan.BodyKind
import com.davils.kapi.codegen.core.operation.plan.FieldPlan
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.SecurityPlan
import com.davils.kapi.codegen.core.options.GeneratorOptions
import com.davils.kapi.codegen.core.options.Visibility
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

private const val SERVICE: String = "service"

private const val OPERATIONS: String = "operations"

private const val ROUTES_SUFFIX: String = "Routes"

private const val FORM_SUFFIX: String = "Form"

private val modifiers: Map<Visibility, KModifier> = mapOf(
    Visibility.Public to KModifier.PUBLIC,
    Visibility.Internal to KModifier.INTERNAL,
)

internal class ServerEmitter(
    options: GeneratorOptions,
    private val origin: String,
    private val fields: Map<String, List<String>>,
    private val kind: ServerKind = ServerKind.Server,
) {
    private val serverPackage: String = options.packageName + "." + kind.packageName

    private val operationPackage: String = options.packageName + ".operation"

    private val visibility: KModifier = modifiers.getValue(options.visibility)

    private val types: TypeNames = TypeNames(options.packageName + ".model")

    private val serializers: SerializerCode = SerializerCode(types)

    private val signatures: ServerSignature = ServerSignature(types)

    private val responses: ResponseEmitter = ResponseEmitter(types, visibility)

    private val replies: ReplyCode = ReplyCode(serializers)

    fun emit(operations: List<OperationPlan>): List<GeneratedFile> {
        val groups = operations.groupBy(OperationPlan::group).toSortedMap()
        val services = groups.map { (group, members) ->
            file(packageName = serverPackage, name = group + kind.service, type = service(group, members))
        }
        val routes = groups.map { (group, members) ->
            val patterns = PatternTable()
            val type = routes(group, members, InputCode(serializers, fields, ValidationCode(patterns)))
            file(
                packageName = serverPackage,
                name = group + ROUTES_SUFFIX,
                type = type,
                properties = patterns.properties,
            )
        }
        val answers = operations.sortedBy(OperationPlan::result).map { operation ->
            val response = responseOf(operation)
            file(
                packageName = operationPackage,
                name = response.simpleName,
                type = responses.type(operation, response, origin),
            )
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
            Docs.on(function, operation.summary, operation.description)
            function.addParameters(signatures.parameters(operation))
            function.returns(responseOf(operation))
            builder.addFunction(function.build())
        }
        return builder.build()
    }

    private fun routes(group: String, operations: List<OperationPlan>, inputs: InputCode): TypeSpec {
        val name = group + ROUTES_SUFFIX
        val service = ClassName(serverPackage, group + kind.service)
        val self = ClassName(serverPackage, name)
        val sorted = operations.sortedBy(OperationPlan::name)
        val constructor = FunSpec.constructorBuilder()
        constructor.addParameter(SERVICE, service)
        val builder = TypeSpec.classBuilder(name)
        builder.addModifiers(visibility)
        builder.addAnnotation(Annotations.generated(origin))
        builder.addSuperinterface(ServerTypes.routes)
        builder.primaryConstructor(constructor.build())
        builder.addProperty(held(service))
        builder.addProperty(operations(sorted))
        sorted.forEach { operation ->
            val form = formOf(self, operation)
            builder.addFunction(handler(operation, form, inputs))
            val body = operation.body?.takeIf { plan -> plan.kind == BodyKind.Form }
            if (body != null) builder.addType(form(form, body.fields))
        }
        return builder.build()
    }

    private fun held(type: ClassName): PropertySpec {
        val property = PropertySpec.builder(SERVICE, type, KModifier.PRIVATE)
        property.initializer("%N", SERVICE)
        return property.build()
    }

    private fun operations(operations: List<OperationPlan>): PropertySpec {
        val code = CodeBlock.builder()
        code.add("listOf(\n⇤")
        operations.forEach { operation -> code.add("%L,\n", operation(operation)) }
        code.add("⇤)⇥⇥")
        val property = PropertySpec.builder(OPERATIONS, LIST.parameterizedBy(ServerTypes.operation))
        property.addModifiers(KModifier.OVERRIDE)
        property.initializer(code.build())
        return property.build()
    }

    private fun operation(operation: OperationPlan): CodeBlock {
        val code = CodeBlock.builder()
        code.add("%T(\n⇥", ServerTypes.operation)
        code.add("name = %S,\n", operation.name)
        code.add("method = %S,\n", operation.method)
        code.add("path = %S,\n", operation.path)
        code.add("security = listOf(%L),\n", operation.security.map(::alternative).joinToCode(", "))
        if (operation.isDelivery) code.add("isDelivery = true,\n")
        code.add("handler = ::%N,\n⇤)", operation.name)
        return code.build()
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

    private fun formOf(self: ClassName, operation: OperationPlan): ClassName =
        self.nestedClass(ServerNames.form(operation.name) + FORM_SUFFIX)

    private fun form(name: ClassName, fields: List<FieldPlan>): TypeSpec {
        val constructor = FunSpec.constructorBuilder()
        fields.forEach { field -> constructor.addParameter(parameter(field)) }
        val builder = TypeSpec.classBuilder(name)
        builder.addModifiers(KModifier.PRIVATE)
        builder.addAnnotation(Kotlin.serializable)
        builder.primaryConstructor(constructor.build())
        fields.forEach { field ->
            val property = PropertySpec.builder(field.name, formType(field))
            property.initializer("%N", field.name)
            property.addAnnotation(Annotations.serialName(field.wireName))
            builder.addProperty(property.build())
        }
        return builder.build()
    }

    private fun formType(field: FieldPlan): TypeName {
        if (!InputCode.isDecoded(field)) return types.of(field.type)
        return STRING.copy(nullable = field.type.isNullable)
    }

    private fun parameter(field: FieldPlan): ParameterSpec {
        val parameter = ParameterSpec.builder(field.name, formType(field))
        if (field.type.isNullable) parameter.defaultValue("null")
        return parameter.build()
    }

    private fun handler(operation: OperationPlan, form: ClassName, inputs: InputCode): FunSpec {
        val reading = inputs.of(operation, form)
        val function = FunSpec.builder(operation.name)
        function.addModifiers(KModifier.PRIVATE, KModifier.SUSPEND)
        function.addParameter("request", ServerTypes.request)
        function.returns(ServerTypes.reply)
        function.addCode(reading.code)
        function.addCode(replies.of(operation, responseOf(operation), reading.arguments))
        return function.build()
    }

    private fun file(
        packageName: String,
        name: String,
        type: TypeSpec,
        properties: List<PropertySpec> = emptyList(),
    ): GeneratedFile {
        val file = FileSpec.builder(packageName = packageName, fileName = name)
        properties.forEach(file::addProperty)
        file.addType(type)
        return GeneratedFile(
            path = packageName.replace(oldChar = '.', newChar = '/') + "/" + name + ".kt",
            kind = GeneratedFileKind.Kotlin,
            content = Sources.of(file),
            platform = GeneratedPlatform.Jvm,
        )
    }
}
