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
import com.davils.kapi.codegen.core.ModelKind
import com.davils.kapi.codegen.core.emit.client.SignatureNames
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.operation.plan.BodyKind
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.pipeline.Findings

private val taken: Set<String> = setOf("input", "request", "response", "service", "server")

internal class ServerSupport(private val findings: Findings) {
    fun audit(operations: List<OperationPlan>) {
        operations.forEach { operation ->
            unsupported(operation).forEach { what -> refuse(operation, what) }
            val names = SignatureNames.of(operation).filter { name -> name in taken }
            names.forEach { name -> collide(operation, name) }
        }
    }

    private fun unsupported(operation: OperationPlan): List<String> {
        val objects = operation.parameters.filter { parameter -> isObject(parameter.type.shape) }
        val fields = listOfNotNull(operation.body).filter { body -> body.fields.isNotEmpty() }
        val streams = operation.responses.filter { response -> response.stream != null }
        val uploads = listOfNotNull(operation.body).filter { body -> body.kind == BodyKind.Upload }
        val upgrades = listOfNotNull(operation.upgrade)
        return objects.map { parameter -> "the parameter ${parameter.wireName}, which is an object, on a server" } +
            fields.map { "a form or a multipart body on a server" } +
            streams.map { "a response that is streamed on a server" } +
            uploads.map { "a body that is streamed on a server" } +
            upgrades.map { "a connection that is upgraded on a server" }
    }

    private fun isObject(shape: TypeShape): Boolean =
        shape is TypeShape.Entries || (shape as? TypeShape.Named)?.kind == ModelKind.Object

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
}
