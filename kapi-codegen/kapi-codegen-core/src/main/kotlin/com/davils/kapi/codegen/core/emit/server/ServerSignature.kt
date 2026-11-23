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

import com.davils.kapi.codegen.core.emit.TypeNames
import com.davils.kapi.codegen.core.emit.client.SignatureCode
import com.davils.kapi.codegen.core.operation.plan.BodyKind
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName

private const val BODY: String = "body"

internal class ServerSignature(private val types: TypeNames) {
    private val signatures: SignatureCode = SignatureCode(types)

    fun taken(operation: OperationPlan): List<Pair<String, TypeName>> {
        val isUpload = operation.body?.kind == BodyKind.Upload
        return signatures.taken(operation).map { (name, use) ->
            val type = ServerTypes.upload.copy(nullable = use.isNullable).takeIf { isUpload && name == BODY }
            name to (type ?: types.of(use))
        }
    }

    fun parameters(operation: OperationPlan): List<ParameterSpec> =
        taken(operation).map { (name, type) -> ParameterSpec.builder(name, type).build() }
}
