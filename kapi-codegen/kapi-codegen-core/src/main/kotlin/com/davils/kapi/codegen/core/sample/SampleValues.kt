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

import com.davils.kapi.codegen.core.model.plan.LimitKind
import com.davils.kapi.codegen.core.model.plan.RulePlan
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.sample.plan.SampleSpan
import com.davils.kapi.codegen.core.sample.plan.SampleValue
import com.davils.kapi.spec.core.value.Value

private const val KEY: String = "key"

internal class SampleValues(
    private val enums: SampleEnums,
    private val link: (model: String, isHard: Boolean) -> SampleValue?,
) {
    fun of(type: TypeUse, isHard: Boolean): SampleValue? {
        if (!type.isNullable) return shaped(type, isHard)
        return shaped(type, isHard = false) ?: SampleValue.Null
    }

    fun tagged(type: TypeUse, tag: String): SampleValue? {
        val named = type.shape as? TypeShape.Named
        val entries = enums.entriesOf(named?.name.orEmpty())
        return entries[tag] ?: of(type.copy(hints = listOf(Value.Text(tag))), isHard = true)
    }

    private fun shaped(type: TypeUse, isHard: Boolean): SampleValue? = when (val shape = type.shape) {
        is TypeShape.Scalar -> SampleScalars.of(shape.kind, type)
        is TypeShape.Named -> named(shape, isHard)
        is TypeShape.External -> null
        is TypeShape.Items -> items(shape, type.rules, isHard)
        is TypeShape.Entries -> entries(shape, type.rules, isHard)
    }

    private fun named(shape: TypeShape.Named, isHard: Boolean): SampleValue? {
        if (shape.packageName != null) return null
        return enums.firstOf(shape.name) ?: link(shape.name, isHard)
    }

    private fun items(shape: TypeShape.Items, rules: List<RulePlan>, isHard: Boolean): SampleValue? {
        val span = SampleSpan.of(rules = rules, least = LimitKind.MinItems, most = LimitKind.MaxItems)
        val item = listOfNotNull(of(shape.item, isHard && span.least > 0))
        val sample = SampleValue.Items(item.flatMap { value -> List(amount(span)) { value } })
        return sample.takeIf { items -> items.meets(rules) }
    }

    private fun entries(shape: TypeShape.Entries, rules: List<RulePlan>, isHard: Boolean): SampleValue? {
        val span = SampleSpan.of(rules = rules, least = LimitKind.MinProperties, most = LimitKind.MaxProperties)
        val value = listOfNotNull(of(shape.value, isHard && span.least > 0))
        val names = (1..amount(span)).map { index -> KEY + index }
        val keys = value.flatMap { entry -> names.map { name -> name to entry } }
        val sample = SampleValue.Entries(keys.toMap())
        return sample.takeIf { entries -> entries.meets(rules) }
    }

    private fun amount(span: SampleSpan): Int = maxOf(a = span.least, b = 1).coerceAtMost(span.most)
}
