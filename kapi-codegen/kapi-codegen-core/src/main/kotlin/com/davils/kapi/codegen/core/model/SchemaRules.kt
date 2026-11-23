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

import com.davils.kapi.spec.openapi.model.schema.Bound
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.codegen.core.model.plan.LimitKind
import com.davils.kapi.codegen.core.model.plan.RulePlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind

internal object SchemaRules {
    fun scalar(definition: SchemaDefinition, kind: ScalarKind): List<RulePlan> {
        if (kind == ScalarKind.Text) return text(definition)
        val rules = number(definition)
        return rules.takeIf { kind.isNumeric }.orEmpty()
    }

    private fun text(definition: SchemaDefinition): List<RulePlan> = listOfNotNull(
        limit(LimitKind.MinLength, definition.minLength),
        limit(LimitKind.MaxLength, definition.maxLength),
        definition.pattern?.let(RulePlan::Pattern),
    )

    private fun number(definition: SchemaDefinition): List<RulePlan> = listOfNotNull(
        bound(definition.minimum, isMinimum = true),
        bound(definition.maximum, isMinimum = false),
        definition.multipleOf?.let(RulePlan::MultipleOf),
    )

    fun items(definition: SchemaDefinition): List<RulePlan> = listOfNotNull(
        limit(LimitKind.MinItems, definition.minItems),
        limit(LimitKind.MaxItems, definition.maxItems),
        RulePlan.Unique.takeIf { definition.hasUniqueItems },
    )

    fun entries(definition: SchemaDefinition): List<RulePlan> = listOfNotNull(
        limit(LimitKind.MinProperties, definition.minProperties),
        limit(LimitKind.MaxProperties, definition.maxProperties),
    )

    private fun limit(kind: LimitKind, value: Int?): RulePlan? = value?.let { limit -> RulePlan.Limit(kind, limit) }

    private fun bound(bound: Bound?, isMinimum: Boolean): RulePlan? =
        bound?.let { limit -> RulePlan.Bound(isMinimum, limit.value, limit.isExclusive) }
}
