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

import com.davils.kapi.codegen.core.CatalogModel
import com.davils.kapi.codegen.core.ModelCatalog
import com.davils.kapi.codegen.core.ModelKind
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.spec.core.model.Reference

private const val COMPONENT_DEPTH: Int = 3

private const val WRITE: String = "Write"

private val componentPath: List<String> = listOf("components", "schemas")

internal object ModelCatalogs {
    fun of(scope: PlanScope, packageName: String, plans: List<ModelPlan>): ModelCatalog {
        val byName = plans.associateBy(ModelPlan::name)
        val modelled = scope.names.filterValues { name -> name in byName }
        val models = modelled.mapValues { (_, name) -> model(byName.getValue(name), byName) }
        return ModelCatalog(scope.documentUri, packageName, models)
    }

    fun foreign(catalogs: List<ModelCatalog>, reference: Reference): TypeShape.Named? {
        val tokens = reference.target.pointer.tokens
        val isComponent = tokens.size == COMPONENT_DEPTH && tokens.take(2) == componentPath
        val key = tokens.lastOrNull().orEmpty()
        val catalog = catalogs.firstOrNull { catalog -> catalog.document == reference.target.document.value }
        val model = catalog?.models.orEmpty()[key]
        val isModel = isComponent && catalog != null && model != null
        if (!isModel) return null
        return TypeShape.Named(
            name = model.type,
            kind = model.kind,
            packageName = catalog.packageName,
            hasForeignWrite = model.hasWriteShape,
        )
    }

    private fun model(plan: ModelPlan, byName: Map<String, ModelPlan>): CatalogModel =
        CatalogModel(plan.name, kindOf(plan), hasWriteShape = plan.name + WRITE in byName)

    private fun kindOf(plan: ModelPlan): ModelKind = when (plan) {
        is ModelPlan.ObjectPlan -> ModelKind.Object
        is ModelPlan.EnumPlan -> ModelKind.Enum
        is ModelPlan.UnionPlan -> ModelKind.Union
    }
}
