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

package com.davils.kapi.spec.openapi.read.path

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.path.Callback
import com.davils.kapi.spec.openapi.model.path.CallbackBuilder
import com.davils.kapi.spec.openapi.model.path.Method
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.model.path.PathItemBuilder
import com.davils.kapi.spec.openapi.model.path.PathTemplate
import com.davils.kapi.spec.openapi.model.path.Paths
import com.davils.kapi.spec.openapi.model.path.PathsBuilder
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.Entry
import com.davils.kapi.spec.openapi.read.keyed
import com.davils.kapi.spec.openapi.read.mapped
import com.davils.kapi.spec.openapi.read.parameter.ParameterReader
import com.davils.kapi.spec.openapi.read.read
import com.davils.kapi.spec.openapi.read.reference.Referables
import com.davils.kapi.spec.openapi.read.reference.ReferenceKind
import com.davils.kapi.spec.openapi.read.reference.References
import com.davils.kapi.spec.openapi.read.server.ServerReader
import com.davils.kapi.spec.openapi.read.shape.Fields
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kapi.spec.openapi.read.shape.Shapes

internal object PathReader {
    fun paths(cursor: Cursor): Paths = cursor.keyed("The paths", PathsBuilder()) { entry -> path(this, entry) }

    fun pathItem(cursor: Cursor): PathItem = cursor.read(Shapes.pathItem, PathItemBuilder()) { fields ->
        reference = fields.cursor(Keys.REFERENCE)?.let { References.read(cursor, ReferenceKind.PathItem) }
        summary = fields.text(Keys.SUMMARY)
        description = fields.text(Keys.DESCRIPTION)
        operations.putAll(operationsOf(fields))
        additionalOperations.putAll(fields.entries(Keys.ADDITIONAL_OPERATIONS).mapped(OperationReader::operation))
        servers.addAll(fields.items(Keys.SERVERS).map(ServerReader::server))
        parameters.addAll(parametersOf(fields))
    }

    fun callback(cursor: Cursor): Callback = cursor.keyed("A callback", CallbackBuilder()) { entry ->
        expressions[entry.key] = pathItem(entry.value)
    }

    fun parametersOf(fields: Fields) = fields.items(Keys.PARAMETERS).mapNotNull { item ->
        Referables.read(item, ReferenceKind.Parameter, ParameterReader::parameter)
    }

    private fun operationsOf(fields: Fields) = Method.entries.mapNotNull { method ->
        fields.cursor(method.keyword)?.let { operation -> method to OperationReader.operation(operation) }
    }

    private fun path(builder: PathsBuilder, entry: Entry) {
        val template = PathTemplate.parseOrNull(entry.key)
        if (template == null) {
            val message = "The path '${entry.key}' must start with a slash"
            return entry.report(DiagnosticCodes.PATH_NOT_ROOTED, message)
        }
        builder.items[template] = pathItem(entry.value)
    }
}
