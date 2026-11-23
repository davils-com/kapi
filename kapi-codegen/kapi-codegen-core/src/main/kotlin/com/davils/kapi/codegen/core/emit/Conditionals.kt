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

package com.davils.kapi.codegen.core.emit

import com.davils.kapi.codegen.core.operation.plan.HeaderPlan
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ResponsePlan

private const val NOT_MODIFIED: Int = 304

private const val ETAG: String = "etag"

private const val LAST_MODIFIED: String = "last-modified"

private val safe: Set<String> = setOf("GET", "HEAD")

private val successes: IntRange = 200..299

internal object Conditionals {
    const val VALIDATORS: String = "validators"

    const val NOT_MODIFIED_NAME: String = "NotModified"

    fun isConditional(operation: OperationPlan): Boolean =
        operation.method in safe && !operation.isDelivery && operation.responses.any(::isValidated)

    fun addsNotModified(operation: OperationPlan): Boolean {
        val isDeclared = operation.responses.any { response ->
            response.status == NOT_MODIFIED || response.name == NOT_MODIFIED_NAME
        }
        return isConditional(operation) && !isDeclared
    }

    fun isValidated(response: ResponsePlan): Boolean {
        val status = response.status ?: return false
        return status in successes && (entityTagOf(response) != null || lastModifiedOf(response) != null)
    }

    fun entityTagOf(response: ResponsePlan): HeaderPlan? = text(response, ETAG)

    fun lastModifiedOf(response: ResponsePlan): HeaderPlan? = text(response, LAST_MODIFIED)

    private fun text(response: ResponsePlan, name: String): HeaderPlan? = response.headers.firstOrNull { header ->
        header.wireName.lowercase() == name && header.reader == HeaderPlan.TEXT
    }
}
