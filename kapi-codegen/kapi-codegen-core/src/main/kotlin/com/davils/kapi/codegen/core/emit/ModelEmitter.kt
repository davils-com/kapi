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

package com.davils.kapi.codegen.core.emit

import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.GeneratedFileKind
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.options.GeneratorOptions
import com.davils.kapi.codegen.core.options.Visibility
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

private val modifiers: Map<Visibility, KModifier> = mapOf(
    Visibility.Public to KModifier.PUBLIC,
    Visibility.Internal to KModifier.INTERNAL,
)

internal class ModelEmitter(options: GeneratorOptions) {
    val modelPackage: String = options.packageName + ".model"

    private val types: TypeNames = TypeNames(modelPackage)

    private val objects: ObjectEmitter = ObjectEmitter(types, modifiers.getValue(options.visibility))

    private val enums: EnumEmitter = EnumEmitter(types, modifiers.getValue(options.visibility))

    private val unions: UnionEmitter = UnionEmitter(types, modifiers.getValue(options.visibility))

    fun emit(plans: List<ModelPlan>): List<GeneratedFile> {
        val types = plans.flatMap { plan -> types(plan).entries }
        return types.map { (name, type) -> file(name, type) }
    }

    private fun types(plan: ModelPlan): Map<String, TypeSpec> = when (plan) {
        is ModelPlan.ObjectPlan -> mapOf(plan.name to objects.type(plan))
        is ModelPlan.EnumPlan -> mapOf(plan.name to enums.type(plan))
        is ModelPlan.UnionPlan -> unions.types(plan)
    }

    private fun file(name: String, type: TypeSpec): GeneratedFile {
        val file = FileSpec.builder(packageName = modelPackage, fileName = name)
        file.addType(type)
        return GeneratedFile(
            path = modelPackage.replace(oldChar = '.', newChar = '/') + "/" + name + ".kt",
            kind = GeneratedFileKind.Kotlin,
            content = Sources.of(file),
        )
    }
}
