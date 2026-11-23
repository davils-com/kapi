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


package com.davils.kapi.codegen.core.model

import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse

private val json: TypeUse = TypeUse(TypeShape.Scalar(ScalarKind.Json))

internal class SchemaReferences(private val scope: PlanScope, private val types: SchemaTypes) {
    private val following: MutableSet<String> = linkedSetOf()

    fun referenced(definition: SchemaDefinition): TypeUse? {
        val reference = definition.reference ?: return null
        return resolved(reference)
    }

    private fun resolved(reference: Reference): TypeUse {
        val located = scope.located(reference) ?: return foreign(reference)
        return component(reference, located.first, located.second)
    }

    private fun foreign(reference: Reference): TypeUse {
        val named = scope.foreign(reference) ?: return refused(reference, "points at something")
        return TypeUse(named)
    }

    private fun component(reference: Reference, key: String, schema: Schema): TypeUse {
        val name = scope.names.getValue(key)
        val kind = SchemaKinds.kindOf(schema)
        if (kind != null) return TypeUse(TypeShape.Named(name, kind))
        return followed(reference, key) { types.typeOf(schema, name) }
    }

    private fun followed(reference: Reference, key: String, resolve: () -> TypeUse): TypeUse {
        if (!following.add(key)) return refused(reference, "leads back to itself and never reaches a type, as does")
        val use = resolve()
        following.remove(key)
        return use
    }

    private fun refused(reference: Reference, problem: String): TypeUse {
        scope.findings.error(
            CodegenCodes.REFERENCE_UNSUPPORTED,
            "The reference ${reference.text} $problem other than a schema in the components of this document",
            reference.location,
        )
        return json
    }
}
