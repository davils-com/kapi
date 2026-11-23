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

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.emit.client.SignatureNames
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.operation.plan.BodyKind
import com.davils.kapi.codegen.core.operation.plan.FieldKind
import com.davils.kapi.codegen.core.operation.plan.FieldPlan
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ParameterPlace
import com.davils.kapi.codegen.core.operation.plan.ParameterPlan
import com.davils.kapi.codegen.core.pipeline.Findings

private val taken: Set<String> = setOf("input", "request", "response", "service", "form", "parts", "reply")

private val members: Set<String> = setOf("operations", "channels", "service")

internal class ServerSupport(private val findings: Findings) {
    fun audit(operations: List<OperationPlan>) {
        operations.forEach { operation ->
            unsupported(operation).forEach { what -> refuse(operation, what) }
            val names = SignatureNames.of(operation).filter { name -> name in taken }
            names.forEach { name -> collide(operation, name) }
            if (operation.name in members) clash(operation)
        }
    }

    private fun unsupported(operation: OperationPlan): List<String> {
        val maps = operation.parameters.filter(::isSpreadMap)
        val body = operation.body
        val parts = body?.takeIf { plan -> plan.kind == BodyKind.Multipart }?.fields.orEmpty()
        val partTexts = parts.filter { field -> field.kind == FieldKind.Texts && !isText(field) }
        val upgrades = listOfNotNull(operation.upgrade)
        val headers = operation.responses
            .flatMap { response -> response.headers }
            .filterNot { header -> isToken(header.wireName) }
        return maps.map { parameter ->
            "the parameter ${parameter.wireName}, a map spread over the query, on a server"
        } +
            partTexts.map { field -> "the parts ${field.wireName}, which are texts but no strings, on a server" } +
            upgrades.map { "a connection that is upgraded on a server" } +
            headers.map { header ->
                "the response header ${header.wireName}, whose name is no token of HTTP, on a server"
            }
    }

    private fun isSpreadMap(parameter: ParameterPlan): Boolean {
        val isQuery = parameter.placement == ParameterPlace.Query || parameter.placement == ParameterPlace.Cookie
        val isSpread = parameter.isExploded && parameter.style != DEEP_OBJECT
        return parameter.type.shape is TypeShape.Entries && isQuery && isSpread
    }

    private fun isToken(name: String): Boolean =
        name.isNotEmpty() && name.all { char -> char in TOKEN_MARKS || (char.code < ASCII && char.isLetterOrDigit()) }

    private fun isText(field: FieldPlan): Boolean {
        val shape = field.type.shape
        val single = (shape as? TypeShape.Items)?.run { item.shape } ?: shape
        return (single as? TypeShape.Scalar)?.kind == ScalarKind.Text
    }

    private fun refuse(operation: OperationPlan, what: String) {
        findings.error(
            CodegenCodes.OPERATION_UNSUPPORTED,
            "The generator has no Kotlin for $what yet",
            operation.origin,
        )
    }

    private fun collide(operation: OperationPlan, name: String) {
        findings.error(
            CodegenCodes.NAME_COLLISION,
            "A parameter of the operation ${operation.name} becomes $name, which the generated server uses itself; " +
                "name it with ${Keywords.NAME}",
            operation.origin,
        )
    }

    private fun clash(operation: OperationPlan) {
        findings.error(
            CodegenCodes.NAME_COLLISION,
            "The operation ${operation.name} has the name of a member of the generated routes; " +
                "name it with ${Keywords.NAME}",
            operation.origin,
        )
    }

    private companion object {
        const val DEEP_OBJECT: String = "DeepObject"

        const val TOKEN_MARKS: String = "!#$%&'*+-.^_`|~"

        const val ASCII: Int = 128
    }
}
