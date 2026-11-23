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

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.model.plan.EnumEntryPlan
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.naming.Identifiers
import com.davils.kapi.codegen.core.naming.NameClaim
import com.davils.kapi.codegen.core.naming.NameTable
import com.davils.kapi.spec.core.value.ExtensionReading
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition

private const val INT32: String = "int32"

private const val NUMBER_PREFIX: String = "VALUE_"

private const val NEGATIVE: String = "MINUS_"

internal class EnumPlanner(private val scope: PlanScope) {
    fun plan(name: String, definition: SchemaDefinition): ModelPlan.EnumPlan {
        val enumeration = definition.enumeration.orEmpty()
        val values = enumeration.filterNot { value -> value is Value.Null }
        val texts = values.filterIsInstance<Value.Text>().map { text -> text.value }
        val whole = values.filterIsInstance<Value.Number>()
        val numbers = whole.mapNotNull { number -> number.value.toLongOrNull() }
        val isNumbered = values.isNotEmpty() && numbers.size == values.size
        val isUniform = isNumbered || texts.size == values.size
        if (values.isEmpty() || !isUniform) refuse(definition)
        val literals = numbers.map(Long::toString).takeIf { isNumbered } ?: texts
        return ModelPlan.EnumPlan(
            name = name,
            origin = scope.originOf(definition),
            base = baseOf(definition, isNumbered),
            entries = entries(definition, literals, isNumbered),
        )
    }

    private fun refuse(definition: SchemaDefinition) {
        scope.findings.error(
            CodegenCodes.ENUM_VALUES,
            "An enumeration needs at least one value, and its values are either all texts or all whole numbers",
            definition.location,
        )
    }

    private fun baseOf(definition: SchemaDefinition, isNumbered: Boolean): ScalarKind {
        if (!isNumbered) return ScalarKind.Text
        return ScalarKind.Int.takeIf { definition.format == INT32 } ?: ScalarKind.Long
    }

    private fun entries(
        definition: SchemaDefinition,
        literals: List<String>,
        isNumbered: Boolean,
    ): List<EnumEntryPlan> {
        val declared = declaredNames(definition).takeIf { names -> names.size == literals.size }
        val names = declared ?: literals.map { literal -> derived(literal, isNumbered) }
        val table = NameTable(scope.findings)
        val entries = literals.zip(names) { literal, name -> EnumEntryPlan(name, literal) }
        entries.forEach { entry ->
            val origin = "The entry ${entry.literal}"
            table.claim(NameClaim(name = entry.name, origin = origin, location = definition.location))
        }
        return entries
    }

    private fun declaredNames(definition: SchemaDefinition): List<String> {
        val readings = listOf(
            definition.extensions.texts(Keywords.ENUM_NAMES),
            definition.extensions.texts(Keywords.ENUM_NAMES_ALIAS),
        )
        val present = readings.filterIsInstance<ExtensionReading.Present<List<String>>>()
        return present.firstOrNull()?.value.orEmpty()
    }

    private fun derived(literal: String, isNumbered: Boolean): String {
        if (!isNumbered) return Identifiers.constant(literal)
        return NUMBER_PREFIX + literal.replace(oldValue = "-", newValue = NEGATIVE)
    }
}
