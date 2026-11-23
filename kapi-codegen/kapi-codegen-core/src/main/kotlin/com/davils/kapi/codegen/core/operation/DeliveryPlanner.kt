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

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.openapi.model.path.Method
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.core.value.ExtensionReading
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.operation.plan.DeliveryPlan
import com.davils.kapi.codegen.core.operation.plan.TimeoutPlan
import kotlin.time.Duration

private const val REQUEST: String = "request"

private val repeatable: Set<String> = setOf("get", "head", "put", "delete", "options")

internal object DeliveryPlanner {
    fun plan(method: Method, operation: Operation, path: Element): DeliveryPlan {
        val declared = listOf(operation, path).mapNotNull { element -> element.extensions[Keywords.TIMEOUT] }
        val timeouts = declared.map(::timeout)
        return DeliveryPlan(timeouts.firstOrNull(), isIdempotent(method, operation))
    }

    private fun isIdempotent(method: Method, operation: Operation): Boolean {
        val declared = operation.extensions.flag(Keywords.IDEMPOTENT) as? ExtensionReading.Present
            ?: return method.keyword in repeatable
        return declared.value
    }

    private fun timeout(value: Value): TimeoutPlan {
        val mapping = value as? Value.Mapping
            ?: return TimeoutPlan(millis(value), connect = null, socket = null)
        val entries = mapping.entries
        return TimeoutPlan(millis(entries[REQUEST]), millis(entries["connect"]), millis(entries["socket"]))
    }

    private fun millis(value: Value?): Long? {
        val text = value as? Value.Text ?: return null
        return Duration.parseIsoString(text.value).inWholeMilliseconds
    }
}
