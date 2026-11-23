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

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.model.PlanScope
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.PropertyPlan
import com.davils.kapi.codegen.core.naming.NameClaim
import com.davils.kapi.codegen.core.naming.NameTable
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.codegen.core.sample.plan.SamplePlan
import com.davils.kapi.codegen.core.sample.plan.SampleValue

internal class SamplePlanner(private val scope: PlanScope, private val prefix: String) {
    fun plan(models: List<ModelPlan>): Samples {
        val enums = SampleEnums(models)
        val objects = SampleObjects(models)
        val ranks = SampleRanks(models, objects, enums)
        val ranked = ranks.settle()
        val components = SampleComponents(SampleGraph.of(models)).of()
        val built = models.filterIsInstance<ModelPlan.ObjectPlan>().filter { plan -> plan.name in ranked }
        val chosen = models.filterIsInstance<ModelPlan.UnionPlan>().filter { plan -> plan.name in ranked }
        val values = built.flatMap { plan ->
            val linked = SampleValues(enums) { model, _ ->
                val isApart = components[model] != components[plan.name]
                val isEarlier = ranked.getOrDefault(model, Int.MAX_VALUE) < ranked.getValue(plan.name)
                SampleValue.Model(model).takeIf { model in ranked && (isApart || isEarlier) }
            }
            listOfNotNull(objects.built(plan, linked)).map { value -> plan to value }
        }
        val members = chosen.flatMap { plan ->
            listOfNotNull(ranks.memberOf(plan.name)).map { member -> plan to SampleValue.Model(member) }
        }
        val reachable = SampleValues(enums) { model, _ -> SampleValue.Model(model).takeIf { model in ranked } }
        report(models.filterNot { plan -> plan.name in ranked }, objects, reachable)
        return Samples(named(values + members), reachable)
    }

    private fun named(values: List<Pair<ModelPlan, SampleValue>>): List<SamplePlan> {
        val table = NameTable(scope.findings)
        val plans = values.map { (plan, value) ->
            SamplePlan(model = plan.name, name = nameOf(plan.name), value = value)
        }
        values.zip(plans).forEach { (value, plan) ->
            val origin = "The sample of ${plan.model}"
            table.claim(NameClaim(plan.name, origin, scope.locationOf(value.first.origin)))
        }
        return plans.sortedBy(SamplePlan::name)
    }

    private fun nameOf(model: String): String {
        val short = model.removePrefix(prefix)
        return short.take(1).lowercase() + short.drop(1)
    }

    private fun report(left: List<ModelPlan>, objects: SampleObjects, values: SampleValues) {
        left.filterIsInstance<ModelPlan.ObjectPlan>().forEach { plan ->
            val missing = objects.missing(plan, values).map(PropertyPlan::jsonName)
            val what = missing.ifEmpty { listOf("what its dependentRequired names") }
            warn(plan, "its property " + what.joinToString(transform = ::shown))
        }
        left.filterIsInstance<ModelPlan.UnionPlan>().forEach { plan -> warn(plan, "any of its members") }
    }

    private fun warn(plan: ModelPlan, what: String) {
        scope.findings.warn(
            CodegenCodes.SAMPLE_UNAVAILABLE,
            "No sample of ${plan.name} is generated, because the generator can make up no value for $what; " +
                "give the schema an example, a default or a constant",
            scope.locationOf(plan.origin),
        )
    }
}
