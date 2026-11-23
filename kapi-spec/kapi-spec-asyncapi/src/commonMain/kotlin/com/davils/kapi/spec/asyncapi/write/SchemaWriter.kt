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

package com.davils.kapi.spec.asyncapi.write

import com.davils.kaml.core.node.YamlNode
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.core.model.Referable

internal object SchemaWriter {
    fun schema(schema: Schema): YamlNode {
        val format = schema.schemaFormat ?: return Nodes.of(schema.definition)
        val out = Out()
        out.text(key = Keys.SCHEMA_FORMAT, value = format)
        out.node(Keys.SCHEMA, Nodes.of(schema.definition))
        return out.build(schema)
    }

    fun slot(schema: Referable<Schema>?): YamlNode? = schema?.let { written ->
        ReferenceWriter.referable(
            written,
            ::schema,
        )
    }
}
