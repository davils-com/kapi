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

import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.sample.plan.SamplePlan
import com.davils.kapi.codegen.core.sample.plan.SampleValue

internal class Samples(val plans: List<SamplePlan>, private val values: SampleValues) {
    private val names: Map<String, String> = plans.associate { plan -> plan.model to plan.name }

    fun of(type: TypeUse): SampleValue? = values.of(type, isHard = true)

    fun nameOf(model: String): String = names.getValue(model)
}
