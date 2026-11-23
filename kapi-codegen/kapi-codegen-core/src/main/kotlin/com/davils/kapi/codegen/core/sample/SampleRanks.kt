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
import com.davils.kapi.codegen.core.sample.plan.SampleValue

internal class SampleRanks(
    private val models: List<ModelPlan>,
    private val objects: SampleObjects,
    enums: SampleEnums,
) {
    private val ranked: MutableSet<String> = linkedSetOf()

    private val chosen: MutableMap<String, String> = mutableMapOf()

    private val values: SampleValues = SampleValues(enums) { model, isHard ->
        SampleValue.Model(model).takeIf { isHard && model in ranked }
    }

    fun settle(): Map<String, Int> {
        settle(models.sortedBy(ModelPlan::name))
        return ranked.withIndex().associate { (index, name) -> name to index }
    }

    fun memberOf(union: String): String? = chosen[union]

    private tailrec fun settle(pending: List<ModelPlan>) {
        val progressed = pending.filter(::rank)
        if (progressed.isEmpty()) return
        settle(pending - progressed.toSet())
    }

    private fun rank(plan: ModelPlan): Boolean {
        val isReady = when (plan) {
            is ModelPlan.ObjectPlan -> objects.built(plan, values) != null
            is ModelPlan.UnionPlan -> choose(plan)
            is ModelPlan.EnumPlan -> false
        }
        if (isReady) ranked += plan.name
        return isReady
    }

    private fun choose(plan: ModelPlan.UnionPlan): Boolean {
        val member = plan.members.firstOrNull { member -> member.type in ranked } ?: return false
        chosen[plan.name] = member.type
        return true
    }
}
