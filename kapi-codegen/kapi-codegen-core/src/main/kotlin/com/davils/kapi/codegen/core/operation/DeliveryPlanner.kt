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

import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.operation.plan.DeliveryPlan
import com.davils.kapi.codegen.core.operation.plan.TimeoutPlan
import com.davils.kapi.codegen.core.pipeline.valueOrNull
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.path.Operation
import kotlin.time.Duration

private const val REQUEST: String = "request"

private val repeatable: Set<String> = setOf("get", "head", "put", "delete", "options")

internal object DeliveryPlanner {
    fun plan(method: String, operation: Operation, path: Element): DeliveryPlan {
        val declared = listOf(operation, path).mapNotNull { element -> element.extensions[Keywords.TIMEOUT] }
        val timeouts = declared.map(::timeout)
        return DeliveryPlan(timeouts.firstOrNull(), isIdempotent(method, operation))
    }

    private fun isIdempotent(method: String, operation: Operation): Boolean {
        val isDeclared = operation.extensions.flag(Keywords.IDEMPOTENT).valueOrNull()
        return isDeclared ?: (method.lowercase() in repeatable)
    }

    private fun timeout(value: Value): TimeoutPlan {
        val mapping = value as? Value.Mapping
            ?: return TimeoutPlan(millis(value), connect = null, socket = null)
        val entries = mapping.entries
        return TimeoutPlan(
            request = millis(entries[REQUEST]),
            connect = millis(entries["connect"]),
            socket = millis(entries["socket"]),
        )
    }

    private fun millis(value: Value?): Long? {
        val text = value as? Value.Text ?: return null
        return Duration.parseIsoString(text.value).inWholeMilliseconds
    }
}
