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

package com.davils.kapi.codegen.core.operation

import com.davils.kapi.codegen.core.ModelKind
import com.davils.kapi.codegen.core.model.PlanScope
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape

private val opaque: Set<ScalarKind> = setOf(ScalarKind.Bytes, ScalarKind.Json)

private val raw: Set<ScalarKind> = setOf(
    ScalarKind.Int,
    ScalarKind.Long,
    ScalarKind.UnsignedLong,
    ScalarKind.Float,
    ScalarKind.Double,
    ScalarKind.Flag,
)

internal class ParameterTypes(private val scope: PlanScope) {
    fun isText(shape: TypeShape): Boolean {
        val scalar = shape as? TypeShape.Scalar
        val named = shape as? TypeShape.Named
        val isPlain = scalar != null && scalar.kind !in opaque
        return isPlain || named?.kind == ModelKind.Enum || shape is TypeShape.External
    }

    fun isTexts(shape: TypeShape): Boolean =
        shape is TypeShape.Items && !shape.item.isNullable && isText(shape.item.shape)

    fun isQuoted(shape: TypeShape): Boolean {
        val items = shape as? TypeShape.Items
        val inner = listOfNotNull(items).map { list -> list.item.shape }
        val single = inner.firstOrNull() ?: shape
        val enums = scope.plans.filterIsInstance<ModelPlan.EnumPlan>()
        val named = enums.filter { plan -> plan.name == (single as? TypeShape.Named)?.name }
        val kinds = named.map(ModelPlan.EnumPlan::base) + listOfNotNull((single as? TypeShape.Scalar)?.kind)
        return kinds.none { kind -> kind in raw }
    }

    fun isFields(shape: TypeShape): Boolean {
        val entries = shape as? TypeShape.Entries
        val named = shape as? TypeShape.Named
        val objects = scope.plans.filterIsInstance<ModelPlan.ObjectPlan>()
        val plan = objects.firstOrNull { candidate -> candidate.name == named?.name }
        val isFlat = plan != null && plan.properties.all { property -> isText(property.type.shape) }
        val isMap = entries != null && isText(entries.value.shape)
        return isFlat || isMap
    }
}
