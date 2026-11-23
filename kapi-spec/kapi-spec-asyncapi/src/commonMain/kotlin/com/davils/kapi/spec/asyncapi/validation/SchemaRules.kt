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
import com.davils.kapi.spec.asyncapi.diagnostic.shown
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.model.schema.SchemaFormat

internal object SchemaRules {
    fun check(scope: RuleScope) {
        val unknown = scope.all<Schema>().filter { schema -> schema.format == SchemaFormat.Unknown }
        unknown.forEach { schema ->
            val format = schema.schemaFormat.orEmpty()
            val message = "The schema format ${shown(format)} is unknown to kapi, the schema is kept as it was written"
            scope.report(DiagnosticCodes.SCHEMA_FORMAT_UNKNOWN, message, schema)
        }
    }
}
