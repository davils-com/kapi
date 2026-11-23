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

package com.davils.kapi.codegen.core.model

import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.model.schema.SchemaType

private val checks: Map<String, (SchemaDefinition) -> Boolean> = linkedMapOf(
    "prefixItems" to { schema -> schema.prefixItems.isNotEmpty() },
    "patternProperties" to { schema -> schema.patternProperties.isNotEmpty() },
    "\$dynamicRef" to { schema -> schema.dynamicReference != null },
    "additionalProperties with a schema next to properties" to { schema ->
        schema.properties.isNotEmpty() && schema.additionalProperties is SchemaDefinition
    },
    "a reference next to properties" to { schema -> schema.reference != null && schema.properties.isNotEmpty() },
    "more than one type" to { schema -> SchemaSupport.typesOf(schema).size > 1 },
)

private val unchecked: Map<String, (SchemaDefinition) -> Boolean> = linkedMapOf(
    "not" to { schema -> schema.not != null },
    "if" to { schema -> schema.condition != null },
    "then" to { schema -> schema.consequence != null },
    "else" to { schema -> schema.alternative != null },
    "contains" to { schema -> schema.contains != null },
    "propertyNames" to { schema -> schema.propertyNames != null },
    "dependentSchemas" to { schema -> schema.dependentSchemas.isNotEmpty() },
    "unevaluatedItems" to { schema -> schema.unevaluatedItems != null },
    "unevaluatedProperties" to { schema -> schema.unevaluatedProperties != null },
)

internal object SchemaSupport {
    fun audit(scope: PlanScope, definition: SchemaDefinition) {
        val found = checks.filterValues { isPresent -> isPresent(definition) }
        found.keys.forEach { keyword -> scope.unsupported(definition, keyword) }
        val loose = unchecked.filterValues { isPresent -> isPresent(definition) }
        loose.keys.forEach { keyword -> scope.unchecked(definition, keyword) }
    }

    fun typesOf(definition: SchemaDefinition): Set<SchemaType> = definition.types - SchemaType.Null
}
