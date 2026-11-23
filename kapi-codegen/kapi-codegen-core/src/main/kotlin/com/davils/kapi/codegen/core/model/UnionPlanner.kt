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

import com.davils.kapi.codegen.core.ModelKind
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.UnionMemberPlan
import com.davils.kapi.spec.openapi.model.schema.Discriminator
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition

private const val SEPARATOR: Char = '/'

internal class UnionPlanner(private val scope: PlanScope, private val composition: Composition) {
    fun plan(name: String, definition: SchemaDefinition): ModelPlan.UnionPlan {
        val alternatives = SchemaKinds.alternativesOf(definition)
        val chosen = alternatives.mapNotNull(::member)
        val shapes = chosen.takeIf { alternatives.isNotEmpty() } ?: extending(definition)
        return ModelPlan.UnionPlan(
            name = name,
            origin = scope.originOf(definition),
            discriminator = definition.discriminator?.propertyName,
            members = members(definition, shapes.toMap()),
            isExclusive = definition.oneOf.isNotEmpty(),
        )
    }

    private fun members(definition: SchemaDefinition, shapes: Map<String, ObjectShape>): List<UnionMemberPlan> {
        val discriminator = definition.discriminator ?: return deduced(definition, shapes)
        return tagged(definition, shapes, discriminator)
    }

    private fun extending(definition: SchemaDefinition): List<Pair<String, ObjectShape>> {
        val children = scope.extending(definition).mapNotNull(::shaped)
        if (children.isEmpty()) {
            scope.unsupported(definition, "a discriminator on a schema that nothing extends or chooses from")
        }
        return children
    }

    private fun member(alternative: Schema): Pair<String, ObjectShape>? {
        val reference = (alternative as? SchemaDefinition)?.reference
        val shaped = reference?.let(scope::keyOf)?.let(::shaped)
        if (shaped == null) scope.unsupported(alternative, "a union with a member that is no object in the components")
        return shaped
    }

    private fun shaped(key: String): Pair<String, ObjectShape>? {
        val target = scope.component(key) as? SchemaDefinition
        val isObject = target != null && SchemaKinds.kindOf(target) == ModelKind.Object
        val model = target?.takeIf { isObject } ?: return null
        return key to composition.shapeOf(model)
    }

    private fun tagged(
        definition: SchemaDefinition,
        shapes: Map<String, ObjectShape>,
        discriminator: Discriminator,
    ): List<UnionMemberPlan> {
        val untagged = shapes.filterValues { shape -> discriminator.propertyName !in shape.required }
        untagged.keys.forEach { key ->
            scope.unsupported(definition, "a union whose member $key does not require ${discriminator.propertyName}")
        }
        val mapped = discriminator.mapping.mapValues { (_, target) -> target.substringAfterLast(SEPARATOR) }
        val foreign = mapped.filterValues { key -> key !in shapes }
        foreign.keys.forEach { value -> scope.unsupported(definition, "a discriminator that maps $value to no member") }
        return shapes.keys.flatMap { key -> membersOf(key, mapped) }
    }

    private fun membersOf(key: String, mapped: Map<String, String>): List<UnionMemberPlan> {
        val mappedValues = mapped.filterValues { target -> target == key }
        val values = mappedValues.keys.ifEmpty { setOf(key) }
        return values.map { value -> UnionMemberPlan(value, scope.names.getValue(key)) }
    }

    private fun deduced(definition: SchemaDefinition, shapes: Map<String, ObjectShape>): List<UnionMemberPlan> =
        shapes.keys.mapNotNull { key ->
            val others = (shapes - key).values.flatMap { shape -> shape.properties.keys }
            val own = shapes.getValue(key).required.firstOrNull { property -> property !in others }
            deducedMember(definition, key, own)
        }

    private fun deducedMember(definition: SchemaDefinition, key: String, own: String?): UnionMemberPlan? {
        if (own == null) {
            scope.unsupported(definition, "a union whose member $key has no required property the others lack")
            return null
        }
        return UnionMemberPlan(value = own, type = scope.names.getValue(key))
    }
}
