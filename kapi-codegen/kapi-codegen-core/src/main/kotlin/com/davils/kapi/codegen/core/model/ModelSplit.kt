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
import com.davils.kapi.codegen.core.model.plan.DependencyPlan
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.PropertyAccess
import com.davils.kapi.codegen.core.model.plan.PropertyPlan
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse

private const val WRITE: String = "Write"

internal class ModelSplit(private val scope: PlanScope) {
    private var writes: Set<String> = emptySet()

    fun split(plans: List<ModelPlan>): List<ModelPlan> {
        val objects = plans.filterIsInstance<ModelPlan.ObjectPlan>()
        val direct = objects.filter { plan -> hasOneWay(plan) || uses(plan).any(TypeShape.Named::hasForeignWrite) }
        val split = grown(plans, direct.map(ModelPlan::name).toSet())
        split.forEach { name -> scope.claimName(name = name + WRITE, origin = "The write shape of $name") }
        writes = split
        val all = plans.flatMap { plan -> shapes(plan, split) }
        return all.sortedBy(ModelPlan::name)
    }

    fun written(use: TypeUse): TypeUse = renamed(use, writes)

    private fun hasOneWay(plan: ModelPlan.ObjectPlan): Boolean =
        plan.properties.any { property -> property.access != PropertyAccess.Both }

    private tailrec fun grown(plans: List<ModelPlan>, split: Set<String>): Set<String> {
        val reaching = plans.filter { plan -> uses(plan).any { named -> isSplit(named, split) } }
        val next = split + reaching.map(ModelPlan::name)
        if (next.size == split.size) return split
        return grown(plans, next)
    }

    private fun isSplit(named: TypeShape.Named, split: Set<String>): Boolean {
        val isLocal = named.packageName == null && named.name in split
        return isLocal || named.hasForeignWrite
    }

    private fun uses(plan: ModelPlan): List<TypeShape.Named> = when (plan) {
        is ModelPlan.ObjectPlan -> plan.properties.flatMap { property -> named(property.type) }
        is ModelPlan.UnionPlan -> plan.members.map { member -> local(member.type) }
        is ModelPlan.EnumPlan -> emptyList()
    }

    private fun local(name: String): TypeShape.Named = TypeShape.Named(name, ModelKind.Object)

    private fun named(use: TypeUse): List<TypeShape.Named> = when (val shape = use.shape) {
        is TypeShape.Named -> listOf(shape)
        is TypeShape.Items -> named(shape.item)
        is TypeShape.Entries -> named(shape.value)
        is TypeShape.Scalar, is TypeShape.External -> emptyList()
    }

    private fun shapes(plan: ModelPlan, split: Set<String>): List<ModelPlan> {
        if (plan.name !in split) return listOf(shared(plan, split))
        return when (plan) {
            is ModelPlan.ObjectPlan -> listOf(read(plan), write(plan, split))
            is ModelPlan.UnionPlan -> listOf(plan, writeUnion(plan, split))
            is ModelPlan.EnumPlan -> listOf(plan)
        }
    }

    private fun shared(plan: ModelPlan, split: Set<String>): ModelPlan {
        val member = plan as? ModelPlan.ObjectPlan ?: return plan
        val writes = member.unions.filter { union -> union in split }
        return member.copy(unions = member.unions + writes.map { union -> union + WRITE })
    }

    private fun read(plan: ModelPlan.ObjectPlan): ModelPlan.ObjectPlan {
        val kept = plan.properties.filter { property -> property.access != PropertyAccess.Write }
        return plan.copy(properties = kept, dependencies = kept(plan.dependencies, kept))
    }

    private fun write(plan: ModelPlan.ObjectPlan, split: Set<String>): ModelPlan.ObjectPlan {
        val kept = plan.properties.filter { property -> property.access != PropertyAccess.Read }
        val renamed = kept.map { property -> property.copy(type = renamed(property.type, split)) }
        return plan.copy(
            name = plan.name + WRITE,
            properties = renamed,
            dependencies = kept(plan.dependencies, kept),
            unions = plan.unions.map { union -> renamed(union, split) },
        )
    }

    private fun writeUnion(plan: ModelPlan.UnionPlan, split: Set<String>): ModelPlan.UnionPlan {
        val members = plan.members.map { member -> member.copy(type = renamed(member.type, split)) }
        return plan.copy(name = plan.name + WRITE, members = members)
    }

    private fun kept(dependencies: List<DependencyPlan>, properties: List<PropertyPlan>): List<DependencyPlan> =
        dependencies.filter { dependency -> dependency.trigger in properties && dependency.dependent in properties }

    private fun renamed(name: String, split: Set<String>): String = (name + WRITE).takeIf { name in split } ?: name

    private fun renamed(named: TypeShape.Named, split: Set<String>): TypeShape.Named {
        if (!isSplit(named, split)) return named
        return named.copy(name = named.name + WRITE)
    }

    private fun renamed(use: TypeUse, split: Set<String>): TypeUse = when (val shape = use.shape) {
        is TypeShape.Named -> use.copy(shape = renamed(shape, split))
        is TypeShape.Items -> use.copy(shape = TypeShape.Items(renamed(shape.item, split)))
        is TypeShape.Entries -> use.copy(shape = TypeShape.Entries(renamed(shape.value, split)))
        is TypeShape.Scalar, is TypeShape.External -> use
    }
}
