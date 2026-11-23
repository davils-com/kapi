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

package com.davils.kapi.spec.openapi.fixture

import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.model.component.Components
import com.davils.kapi.spec.openapi.model.path.Method
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.model.path.PathTemplate
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

internal fun Document.parts(): Components = components.shouldNotBeNull()

internal fun Document.path(template: String): PathItem {
    val declared = paths.shouldNotBeNull()
    return declared.items.getValue(PathTemplate(template))
}

internal fun Document.operation(template: String, method: Method): Operation {
    val item = path(template)
    return item.operations.getValue(method)
}

internal fun Document.schema(name: String): SchemaDefinition {
    val schemas = parts().schemas
    return schemas.getValue(name).defined()
}

internal fun Schema?.defined(): SchemaDefinition = shouldBeInstanceOf<SchemaDefinition>()
