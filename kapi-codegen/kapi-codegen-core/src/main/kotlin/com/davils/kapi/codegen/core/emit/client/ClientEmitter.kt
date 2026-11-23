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

package com.davils.kapi.codegen.core.emit.client

import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.GeneratedFileKind
import com.davils.kapi.codegen.core.emit.Annotations
import com.davils.kapi.codegen.core.emit.Conditionals
import com.davils.kapi.codegen.core.emit.Sources
import com.davils.kapi.codegen.core.emit.TypeNames
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ResponsePlan
import com.davils.kapi.codegen.core.operation.plan.StreamPlan
import com.davils.kapi.codegen.core.options.GeneratorOptions
import com.davils.kapi.codegen.core.options.Visibility
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec

private const val CLIENT: String = "client"

private val modifiers: Map<Visibility, KModifier> = mapOf(
    Visibility.Public to KModifier.PUBLIC,
    Visibility.Internal to KModifier.INTERNAL,
)

internal class ClientEmitter(
    options: GeneratorOptions,
    private val origin: String,
    private val kind: ClientKind = ClientKind.Client,
) {
    private val clientPackage: String = options.packageName + "." + kind.packageName

    private val operationPackage: String = options.packageName + ".operation"

    private val visibility: KModifier = modifiers.getValue(options.visibility)

    private val types: TypeNames = TypeNames(options.packageName + ".model")

    private val serializers: SerializerCode = SerializerCode(types)

    private val calls: CallCode = CallCode(serializers)

    private val signatures: SignatureCode = SignatureCode(types)

    private val pages: PageCode = PageCode(signatures, visibility)

    private val streams: StreamCode = StreamCode(types, serializers)

    private val parts: StreamParts = StreamParts(visibility, signatures, calls)

    private val answers: AnswerCode = AnswerCode(serializers, ::resultOf)

    private val results: ResultEmitter = ResultEmitter(types, visibility)

    fun emit(operations: List<OperationPlan>): List<GeneratedFile> {
        val groups = operations.groupBy(OperationPlan::group).toSortedMap()
        val clients = groups.map { (group, members) ->
            file(packageName = clientPackage, name = group + kind.suffix, type = client(group + kind.suffix, members))
        }
        val outcomes = operations.sortedBy(OperationPlan::result).map { operation ->
            file(
                packageName = operationPackage,
                name = operation.result,
                type = results.type(operation, resultOf(operation), origin),
            )
        }
        return clients + outcomes
    }

    private fun resultOf(operation: OperationPlan): ClassName = ClassName(operationPackage, operation.result)

    private fun client(name: String, operations: List<OperationPlan>): TypeSpec {
        val constructor = FunSpec.constructorBuilder()
        constructor.addParameter(CLIENT, ClientTypes.client)
        val property = PropertySpec.builder(CLIENT, ClientTypes.client, KModifier.PRIVATE)
        property.initializer("%N", CLIENT)
        val builder = TypeSpec.classBuilder(name)
        builder.addModifiers(visibility)
        builder.addAnnotation(Annotations.generated(origin))
        builder.primaryConstructor(constructor.build())
        builder.addProperty(property.build())
        operations.sortedBy(OperationPlan::name).forEach { operation ->
            builder.addProperties(answers.properties(operation))
            builder.addFunction(function(operation))
            builder.addFunction(answers.function(operation, resultOf(operation)))
            builder.addFunctions(answers.readers(operation, resultOf(operation)))
            operation.pages?.let { plan -> builder.addFunction(pages.function(operation, plan, resultOf(operation))) }
            streamed(operation).forEach { plan -> builder.addFunction(streams.function(operation, plan, parts)) }
        }
        return builder.build()
    }

    private fun streamed(operation: OperationPlan): List<StreamPlan> =
        operation.responses.mapNotNull(ResponsePlan::stream).take(1)

    private fun function(operation: OperationPlan): FunSpec {
        val result = resultOf(operation)
        val function = FunSpec.builder(operation.name)
        function.addModifiers(visibility, KModifier.SUSPEND)
        if (operation.isDelivery) function.addParameter(CallCode.DESTINATION, STRING)
        signatures.taken(operation).forEach { (name, type) ->
            function.addParameter(signatures.parameter(name, type))
        }
        if (Conditionals.isConditional(operation)) function.addParameter(validators())
        function.returns(result)
        function.addCode(calls.of(operation))
        function.addCode("return when (val exchange = %N.execute(call)) {\n", CLIENT)
        function.addCode("⇥is %T -> %T.Failed(exchange.failure)\n", ClientTypes.failed, result)
        function.addCode("is %T -> %N(exchange)\n⇤}\n", ClientTypes.answered, operation.name + AnswerCode.RESULT)
        return function.build()
    }

    private fun validators(): ParameterSpec {
        val parameter = ParameterSpec.builder(Conditionals.VALIDATORS, ClientTypes.validators.copy(nullable = true))
        parameter.defaultValue("null")
        return parameter.build()
    }

    private fun file(packageName: String, name: String, type: TypeSpec): GeneratedFile {
        val file = FileSpec.builder(packageName = packageName, fileName = name)
        file.addType(type)
        return GeneratedFile(
            path = packageName.replace(oldChar = '.', newChar = '/') + "/" + name + ".kt",
            kind = GeneratedFileKind.Kotlin,
            content = Sources.of(file),
        )
    }
}
