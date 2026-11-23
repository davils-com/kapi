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

package com.davils.kapi.codegen.core.emit.mock

import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.GeneratedFileKind
import com.davils.kapi.codegen.core.GeneratedPlatform
import com.davils.kapi.codegen.core.emit.Annotations
import com.davils.kapi.codegen.core.emit.Sources
import com.davils.kapi.codegen.core.emit.TypeNames
import com.davils.kapi.codegen.core.emit.server.ServerNames
import com.davils.kapi.codegen.core.emit.server.ServerSignature
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.options.GeneratorOptions
import com.davils.kapi.codegen.core.options.Visibility
import com.davils.kapi.codegen.core.sample.MockPlanner
import com.davils.kapi.codegen.core.sample.Samples
import com.davils.kapi.codegen.core.sample.plan.MockAnswer
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

private const val MOCK_SUFFIX: String = "MockService"
private const val SAMPLES_SUFFIX: String = "Samples"

private val modifiers: Map<Visibility, KModifier> = mapOf(
    Visibility.Public to KModifier.PUBLIC,
    Visibility.Internal to KModifier.INTERNAL,
)

internal class MockEmitter(
    options: GeneratorOptions,
    private val origin: String,
    private val samples: Samples,
    private val planner: MockPlanner,
) {
    private val testingPackage: String = options.packageName + ".testing"

    private val serverPackage: String = options.packageName + ".server"

    private val operationPackage: String = options.packageName + ".operation"

    private val visibility: KModifier = modifiers.getValue(options.visibility)

    private val types: TypeNames = TypeNames(options.packageName + ".model")

    private val samplesName: ClassName = ClassName(
        testingPackage,
        options.typePrefix + options.apiName + SAMPLES_SUFFIX,
    )

    private val signatures: ServerSignature = ServerSignature(types)

    private val own: SampleCode = SampleCode(types) { model -> CodeBlock.of("%N", samples.nameOf(model)) }

    private val foreign: SampleCode = SampleCode(types) { model ->
        CodeBlock.of("%T.%N", samplesName, samples.nameOf(model))
    }

    fun emit(operations: List<OperationPlan>): List<GeneratedFile> {
        val groups = operations.groupBy(OperationPlan::group).toSortedMap()
        val services = groups.map { (group, members) -> file(group + MOCK_SUFFIX, service(group, members)) }
        return services + file(samplesName.simpleName, samples())
    }

    private fun samples(): TypeSpec {
        val builder = TypeSpec.objectBuilder(samplesName)
        builder.addModifiers(visibility)
        builder.addAnnotation(Annotations.generated(origin))
        samples.plans.forEach { plan ->
            val getter = FunSpec.getterBuilder()
            getter.addStatement("return %L", own.of(plan.value))
            val property = PropertySpec.builder(plan.name, types.model(plan.model), visibility)
            property.getter(getter.build())
            builder.addProperty(property.build())
        }
        return builder.build()
    }

    private fun service(group: String, operations: List<OperationPlan>): TypeSpec {
        val sorted = operations.sortedBy(OperationPlan::name)
        val answered = sorted.flatMap { operation ->
            listOfNotNull(planner.answer(operation)).map { answer -> operation to answer }
        }
        val openness = KModifier.OPEN.takeIf { answered.size == sorted.size } ?: KModifier.ABSTRACT
        val builder = TypeSpec.classBuilder(group + MOCK_SUFFIX)
        builder.addModifiers(visibility, openness)
        builder.addAnnotation(Annotations.generated(origin))
        builder.addSuperinterface(ClassName(serverPackage, group + ServerNames.SERVICE_SUFFIX))
        answered.forEach { (operation, answer) -> builder.addFunction(answering(operation, answer)) }
        return builder.build()
    }

    private fun answering(operation: OperationPlan, answer: MockAnswer): FunSpec {
        val response = ServerNames.response(operationPackage, operation)
        val variant = CodeBlock.of("%T", response.nestedClass(answer.response.name))
        val function = FunSpec.builder(operation.name)
        function.addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
        function.addParameters(signatures.parameters(operation))
        function.returns(response)
        function.addStatement("return %L", answered(variant, answer))
        return function.build()
    }

    private fun answered(variant: CodeBlock, answer: MockAnswer): CodeBlock {
        if (answer.arguments.isEmpty()) return variant
        return foreign.called(variant, answer.arguments)
    }

    private fun file(name: String, type: TypeSpec): GeneratedFile {
        val file = FileSpec.builder(packageName = testingPackage, fileName = name)
        file.addType(type)
        return GeneratedFile(
            path = testingPackage.replace(oldChar = '.', newChar = '/') + "/" + name + ".kt",
            kind = GeneratedFileKind.Kotlin,
            content = Sources.of(file),
            platform = GeneratedPlatform.Jvm,
        )
    }
}
