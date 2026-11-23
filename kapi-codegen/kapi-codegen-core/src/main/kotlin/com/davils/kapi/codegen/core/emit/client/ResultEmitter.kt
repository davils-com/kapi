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

import com.davils.kapi.codegen.core.emit.Annotations
import com.davils.kapi.codegen.core.emit.Kotlin
import com.davils.kapi.codegen.core.emit.TypeNames
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ResponsePlan
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal class ResultEmitter(private val types: TypeNames, private val visibility: KModifier) {
    fun type(operation: OperationPlan, self: ClassName, origin: String): TypeSpec {
        val declared = operation.responses.map { response -> response.name to fields(response) }
        val unexpected = listOf(STATUS to INT, HEADERS to ClientTypes.headers, CONTENT to Kotlin.byteString)
        val malformed = listOf(STATUS to INT, "reason" to STRING)
        val failed = listOf("failure" to ClientTypes.failure)
        val variants = declared + listOf(UNEXPECTED to unexpected, MALFORMED to malformed, FAILED to failed)
        val builder = TypeSpec.interfaceBuilder(self)
        builder.addModifiers(visibility, KModifier.SEALED)
        builder.addAnnotation(Annotations.generated(origin))
        variants.forEach { (name, fields) ->
            builder.addType(Annotations.annotated(variant(self, name, fields), origin))
        }
        return builder.build()
    }

    private fun fields(response: ResponsePlan): List<Pair<String, TypeName>> {
        val status = listOf(STATUS to INT).filter { response.status == null }
        val body = response.body?.let { type -> listOf(BODY to types.of(type)) }
        val typed = response.headers.map { header -> header.name to types.of(header.type) }
        return status + (body ?: listOf(CONTENT to Kotlin.byteString)) + typed + (HEADERS to ClientTypes.headers)
    }

    private fun variant(self: ClassName, name: String, fields: List<Pair<String, TypeName>>): TypeSpec {
        val constructor = FunSpec.constructorBuilder()
        val builder = TypeSpec.classBuilder(name)
        builder.addModifiers(visibility, KModifier.DATA)
        builder.addSuperinterface(self)
        fields.forEach { (field, type) ->
            constructor.addParameter(field, type)
            val property = PropertySpec.builder(field, type, visibility)
            property.initializer("%N", field)
            builder.addProperty(property.build())
        }
        builder.primaryConstructor(constructor.build())
        return builder.build()
    }

    companion object {
        const val UNEXPECTED: String = "Unexpected"

        const val MALFORMED: String = "Malformed"

        const val FAILED: String = "Failed"

        const val STATUS: String = "status"

        const val HEADERS: String = "headers"

        const val CONTENT: String = "content"

        const val BODY: String = "body"
    }
}
