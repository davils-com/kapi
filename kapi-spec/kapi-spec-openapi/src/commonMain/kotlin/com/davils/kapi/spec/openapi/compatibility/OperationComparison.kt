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

package com.davils.kapi.spec.openapi.compatibility

import com.davils.kapi.spec.openapi.model.path.Operation

internal class OperationComparison(private val context: Context) {
    private val parameters: ParameterComparison = ParameterComparison(context)

    private val contents: ContentComparison = ContentComparison(context)

    fun removed(name: String, operation: Operation) {
        context.report(
            rule = "operation-removed",
            impacts = clientBreaking,
            message = "The operation $name is removed",
            before = operation,
            after = null,
        )
    }

    fun added(name: String, operation: Operation) {
        context.report(
            rule = "operation-added",
            impacts = providerBreaking,
            message = "The operation $name is added",
            before = null,
            after = operation,
        )
    }

    fun compare(name: String, paths: Both<DocumentComparison.Path>, operations: Both<Operation>) {
        deprecated(name, operations)
        identity(name, operations)
        parameters.compare(name = name, paths = paths, operations = operations)
        contents.request(name, operations)
        contents.responses(name, operations)
        SecurityComparison.requirements(context = context, name = name, operations = operations)
    }

    private fun deprecated(name: String, operations: Both<Operation>) {
        val isNew = operations.after.isDeprecated && !operations.before.isDeprecated
        if (!isNew) return
        context.report(
            rule = "operation-deprecated",
            impacts = informing,
            message = "The operation $name is deprecated",
            before = operations.before,
            after = operations.after,
        )
    }

    private fun identity(name: String, operations: Both<Operation>) {
        val before = operations.before.operationId
        val after = operations.after.operationId
        if (before == null || after == null || before == after) return
        context.report(
            rule = "operation-id-changed",
            impacts = bothBreaking,
            message = "The operation id of $name changes from ${shown(before)} to ${shown(after)}",
            before = operations.before,
            after = operations.after,
        )
    }
}
