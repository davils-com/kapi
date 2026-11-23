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

import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.path.OperationBuilder
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.content.ContentReader
import com.davils.kapi.spec.openapi.read.content.ResponseReader
import com.davils.kapi.spec.openapi.read.info.InfoReader
import com.davils.kapi.spec.openapi.read.read
import com.davils.kapi.spec.openapi.read.referables
import com.davils.kapi.spec.openapi.read.reference.Referables
import com.davils.kapi.spec.openapi.read.reference.ReferenceKind
import com.davils.kapi.spec.openapi.read.security.SecurityReader
import com.davils.kapi.spec.openapi.read.server.ServerReader
import com.davils.kapi.spec.openapi.read.shape.Fields
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kapi.spec.openapi.read.shape.Shapes

internal object OperationReader {
    fun operation(cursor: Cursor): Operation = cursor.read(Shapes.operation, OperationBuilder()) { fields ->
        operationId = fields.text(Keys.OPERATION_ID)
        summary = fields.text(Keys.SUMMARY)
        description = fields.text(Keys.DESCRIPTION)
        tags.addAll(fields.texts(Keys.TAGS))
        externalDocs = fields.cursor(Keys.EXTERNAL_DOCS)?.let(InfoReader::externalDocs)
        parameters.addAll(PathReader.parametersOf(fields))
        isDeprecated = fields.flag(Keys.DEPRECATED)
        servers.addAll(fields.items(Keys.SERVERS).map(ServerReader::server))
        exchange(this, fields)
    }

    private fun exchange(builder: OperationBuilder, fields: Fields) {
        val body = fields.cursor(Keys.REQUEST_BODY)
        builder.requestBody = body?.let { Referables.read(it, ReferenceKind.RequestBody, ContentReader::requestBody) }
        fields.cursor(Keys.RESPONSES)?.let { declared -> builder.responses = ResponseReader.responses(declared) }
        val callbacks = fields.entries(Keys.CALLBACKS)
        builder.callbacks.putAll(callbacks.referables(ReferenceKind.Callback, PathReader::callback))
        val security = fields.cursor(Keys.SECURITY)
        builder.security = security?.let { fields.items(Keys.SECURITY).map(SecurityReader::requirement) }
    }
}
