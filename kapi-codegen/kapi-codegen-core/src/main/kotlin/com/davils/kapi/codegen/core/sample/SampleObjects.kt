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

package com.davils.kapi.codegen.core.sample

import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.PropertyPlan
import com.davils.kapi.codegen.core.sample.plan.SampleValue

internal class SampleObjects(models: List<ModelPlan>) {
    private val unions: List<ModelPlan.UnionPlan> = models.filterIsInstance<ModelPlan.UnionPlan>()

    private val tags: Map<String, Map<String, String>> = tagsOf(unions)

    fun built(plan: ModelPlan.ObjectPlan, values: SampleValues): SampleValue.Built? {
        val properties = plan.properties.filterNot(PropertyPlan::isPatch)
        val made = properties.flatMap { property ->
            listOfNotNull(valueOf(plan, property, values)).map { value -> property to value }
        }
        val valued = made.map { (property, _) -> property.name }
        val isComplete = properties.all { property -> property.isOptional || property.name in valued }
        val kept = made.filterNot { (property, value) -> property.isOptional && value == SampleValue.Null }
        val arguments = kept.associate { (property, value) -> property.name to value }
        return settled(plan, arguments).takeIf { isComplete }
    }

    fun missing(plan: ModelPlan.ObjectPlan, values: SampleValues): List<PropertyPlan> {
        val required = plan.properties.filterNot { property -> property.isOptional || property.isPatch }
        return required.filter { property -> valueOf(plan, property, values) == null }
    }

    private fun valueOf(plan: ModelPlan.ObjectPlan, property: PropertyPlan, values: SampleValues): SampleValue? {
        val tag = tags[plan.name].orEmpty()[property.jsonName]
        val tagged = listOfNotNull(tag).mapNotNull { value -> values.tagged(property.type, value) }
        return tagged.firstOrNull() ?: values.of(property.type, isHard = !property.isOptional)
    }

    private tailrec fun settled(plan: ModelPlan.ObjectPlan, arguments: Map<String, SampleValue>): SampleValue.Built? {
        val filled = arguments.filterValues { value -> value != SampleValue.Null }
        val broken = plan.dependencies.filter { dependency ->
            dependency.trigger.name in filled && dependency.dependent.name !in filled
        }
        val isStuck = broken.any { dependency -> !dependency.trigger.isOptional }
        if (broken.isEmpty() || isStuck) return SampleValue.Built(plan.name, arguments).takeIf { broken.isEmpty() }
        return settled(plan, arguments - broken.map { dependency -> dependency.trigger.name }.toSet())
    }

    private fun tagsOf(unions: List<ModelPlan.UnionPlan>): Map<String, Map<String, String>> {
        val tagged = unions.flatMap { union ->
            val property = listOfNotNull(union.discriminator)
            property.flatMap { name -> union.members.map { member -> member.type to (name to member.value) } }
        }
        val byMember = tagged.groupBy({ (member, _) -> member }, { (_, tag) -> tag })
        return byMember.mapValues { (_, pairs) -> pairs.reversed().toMap() }
    }
}
