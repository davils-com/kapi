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
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse

internal object SampleGraph {
    fun of(models: List<ModelPlan>): Map<String, List<String>> = models.associate { plan -> plan.name to uses(plan) }

    private fun uses(plan: ModelPlan): List<String> = when (plan) {
        is ModelPlan.ObjectPlan -> plan.properties.flatMap { property -> uses(property.type) }
        is ModelPlan.UnionPlan -> plan.members.map { member -> member.type }
        is ModelPlan.EnumPlan -> emptyList()
    }

    private fun uses(use: TypeUse): List<String> = when (val shape = use.shape) {
        is TypeShape.Named -> listOf(shape.name).filter { shape.packageName == null }
        is TypeShape.Items -> uses(shape.item)
        is TypeShape.Entries -> uses(shape.value)
        is TypeShape.Scalar, is TypeShape.External -> emptyList()
    }
}
