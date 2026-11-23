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

import com.davils.kapi.codegen.core.emit.TypeNames
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.squareup.kotlinpoet.ParameterSpec

internal class SignatureCode(private val types: TypeNames) {
    fun taken(operation: OperationPlan): List<Pair<String, TypeUse>> {
        val parameters = operation.parameters.map { parameter -> parameter.name to parameter.type }
        val body = listOfNotNull(operation.body)
        val fields = body.flatMap { plan -> plan.fields.map { field -> field.name to field.type } }
        val whole = body.filter { plan -> plan.fields.isEmpty() }.map { plan -> "body" to plan.type }
        return (parameters + fields + whole).sortedBy { (_, type) -> type.isNullable }
    }

    fun parameter(name: String, type: TypeUse): ParameterSpec {
        val parameter = ParameterSpec.builder(name, types.of(type))
        if (type.isNullable) parameter.defaultValue("null")
        return parameter.build()
    }
}
