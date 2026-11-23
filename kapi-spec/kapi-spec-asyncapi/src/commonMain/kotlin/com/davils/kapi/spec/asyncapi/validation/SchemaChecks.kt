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

import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.model.schema.SchemaFormat
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.Value

internal object SchemaChecks {
    const val OBJECT: String = "object"

    private const val TYPE: String = "type"

    private const val PROPERTIES: String = "properties"

    private val typed: Set<SchemaFormat> = setOf(SchemaFormat.AsyncApi, SchemaFormat.JsonSchema, SchemaFormat.OpenApi)

    fun typeOf(scope: RuleScope, referable: Referable<Schema>?): String? {
        val definition = definitionOf(scope, referable) ?: return null
        return (definition[TYPE] as? Value.Text)?.value
    }

    fun isObjectWithProperties(scope: RuleScope, referable: Referable<Schema>?): Boolean {
        val definition = definitionOf(scope, referable) ?: return true
        val type = (definition[TYPE] as? Value.Text)?.value
        return type == OBJECT && definition[PROPERTIES] is Value.Mapping
    }

    private fun definitionOf(scope: RuleScope, referable: Referable<Schema>?): Value.Mapping? {
        val schema = scope.resolve(referable)?.takeIf { resolved -> resolved.format in typed }
        return schema?.definition as? Value.Mapping
    }
}
