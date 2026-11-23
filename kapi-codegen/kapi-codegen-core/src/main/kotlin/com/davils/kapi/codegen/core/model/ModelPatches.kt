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

import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse

private const val WRITE: String = "Write"

private const val PATCH: String = "Patch"

internal class ModelPatches(private val scope: PlanScope) {
    fun plan(models: List<ModelPlan>, bodies: List<TypeUse>): List<ModelPlan> {
        val names = bodies.mapNotNull { body -> (body.shape as? TypeShape.Named)?.name }.toSet()
        val sources = models.filterIsInstance<ModelPlan.ObjectPlan>().filter { plan -> plan.name in names }
        sources.forEach { plan -> scope.claimName(nameOf(plan.name), "The patch shape of ${plan.name}") }
        return sources.map(::patch)
    }

    fun patched(use: TypeUse): TypeUse {
        val named = use.shape as? TypeShape.Named ?: return use
        return use.copy(shape = named.copy(name = nameOf(named.name)))
    }

    private fun patch(plan: ModelPlan.ObjectPlan): ModelPlan.ObjectPlan = plan.copy(
        name = nameOf(plan.name),
        properties = plan.properties.map { property -> property.copy(isPatch = true) },
        dependencies = emptyList(),
        unions = emptyList(),
    )

    private fun nameOf(name: String): String = name.removeSuffix(WRITE) + PATCH
}
