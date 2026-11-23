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

package com.davils.kapi.codegen.core.model.plan

internal sealed interface ModelPlan {
    val name: String

    val origin: String

    data class ObjectPlan(
        override val name: String,
        override val origin: String,
        val properties: List<PropertyPlan>,
        val dependencies: List<DependencyPlan> = emptyList(),
        val unions: List<String> = emptyList(),
    ) : ModelPlan

    data class EnumPlan(
        override val name: String,
        override val origin: String,
        val base: ScalarKind,
        val entries: List<EnumEntryPlan>,
    ) : ModelPlan

    data class UnionPlan(
        override val name: String,
        override val origin: String,
        val discriminator: String?,
        val members: List<UnionMemberPlan>,
        val isExclusive: Boolean,
    ) : ModelPlan
}
