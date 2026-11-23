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

package com.davils.kapi.spec.asyncapi.validation

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.model.binding.WebSocketChannelBinding
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.core.model.Referable

internal object BindingRules {
    fun check(scope: RuleScope) {
        scope.all<WebSocketChannelBinding>().forEach { binding ->
            schema(scope = scope, binding = binding, name = "query", schema = binding.query)
            schema(scope = scope, binding = binding, name = "headers", schema = binding.headers)
        }
    }

    private fun schema(scope: RuleScope, binding: WebSocketChannelBinding, name: String, schema: Referable<Schema>?) {
        if (schema == null || SchemaChecks.isObjectWithProperties(scope, schema)) return
        val message = "The $name of a WebSocket binding is described by a schema of type object with properties"
        scope.report(DiagnosticCodes.BINDING_WEBSOCKET_SCHEMA, message, binding)
    }
}
