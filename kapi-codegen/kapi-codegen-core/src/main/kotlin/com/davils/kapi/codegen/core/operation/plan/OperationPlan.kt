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

package com.davils.kapi.codegen.core.operation.plan

import com.davils.kapi.spec.core.diagnostic.Location

internal data class OperationPlan(
    val name: String,
    val group: String,
    val result: String,
    val method: String,
    val path: String,
    val parameters: List<ParameterPlan>,
    val body: BodyPlan?,
    val responses: List<ResponsePlan>,
    val security: List<List<SecurityPlan>>,
    val delivery: DeliveryPlan,
    val pages: PagePlan? = null,
    val origin: Location? = null,
    val isDelivery: Boolean = false,
    val upgrade: String? = null,
    val summary: String? = null,
    val description: String? = null,
) {
    companion object {
        const val INPUT: String = "input"
    }
}
