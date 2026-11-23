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

import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition

private val empty: ObjectShape = ObjectShape(emptyMap(), emptySet())

internal class Composition(private val scope: PlanScope) {
    private val composing: MutableSet<SchemaDefinition> = linkedSetOf()

    fun shapeOf(definition: SchemaDefinition): ObjectShape {
        if (!composing.add(definition)) return refused(definition, "allOf that leads back to itself")
        val own = ObjectShape(definition.properties, definition.required.toSet(), definition.dependentRequired)
        val shape = definition.allOf.fold(own) { shape, member ->
            merged(owner = definition, first = shape, second = member(member))
        }
        composing.remove(definition)
        return shape
    }

    private fun member(schema: Schema): ObjectShape {
        if (SchemaKinds.isUnconstrained(schema)) return empty
        val member = resolved(schema)?.takeIf(SchemaKinds::isComposable)
        return member?.let(::audited) ?: refused(schema, "allOf with a member that is no object of this document")
    }

    private fun audited(member: SchemaDefinition): ObjectShape {
        SchemaSupport.audit(scope, member)
        return shapeOf(member)
    }

    private fun resolved(schema: Schema): SchemaDefinition? {
        val definition = schema as? SchemaDefinition
        val reference = definition?.reference ?: return definition
        return scope.componentOf(reference) as? SchemaDefinition
    }

    private fun merged(owner: SchemaDefinition, first: ObjectShape, second: ObjectShape): ObjectShape {
        val clashing = second.properties.filter { (name, schema) -> first.properties[name]?.equals(schema) == false }
        clashing.keys.forEach { name -> refused(owner, "allOf that declares the property $name in two ways") }
        return ObjectShape(
            properties = first.properties + second.properties,
            required = first.required + second.required,
            dependencies = first.dependencies + second.dependencies,
        )
    }

    private fun refused(schema: Schema, what: String): ObjectShape {
        scope.unsupported(schema, what)
        return empty
    }
}
