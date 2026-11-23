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

import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.core.value.ValueKind
import com.davils.kapi.codegen.core.model.plan.RulePlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind

private val plain: Map<ValueKind, ScalarKind> = mapOf(
    ValueKind.Text to ScalarKind.Text,
    ValueKind.Flag to ScalarKind.Flag,
)

internal class SchemaConstants(private val scope: PlanScope) {
    fun kindOf(definition: SchemaDefinition): ScalarKind? {
        val value = definition.constant ?: return null
        return kindOf(value)
    }

    fun rules(definition: SchemaDefinition, kind: ScalarKind): List<RulePlan> {
        val value = definition.constant ?: return emptyList()
        if (!fits(value, kind)) scope.unsupported(definition, "const that does not fit its type")
        return listOf(RulePlan.Constant(value))
    }

    fun refuseStructured(definition: SchemaDefinition) {
        if (definition.constant != null) scope.unsupported(definition, "const that is no text, number or flag")
    }

    private fun kindOf(value: Value): ScalarKind? {
        val number = value as? Value.Number ?: return plain[value.kind]
        return ScalarKind.Long.takeIf { number.value.isIntegral } ?: ScalarKind.Double
    }

    private fun fits(value: Value, kind: ScalarKind): Boolean {
        val natural = kindOf(value) ?: return false
        val isNumber = natural.isNumeric && kind.isNumeric && (natural.isIntegral || !kind.isIntegral)
        return natural == kind || isNumber
    }
}
