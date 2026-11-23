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

package com.davils.kapi.spec.openapi.read.content

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.content.Link
import com.davils.kapi.spec.openapi.model.content.LinkBuilder
import com.davils.kapi.spec.openapi.model.content.Response
import com.davils.kapi.spec.openapi.model.content.ResponseBuilder
import com.davils.kapi.spec.openapi.model.content.ResponseStatus
import com.davils.kapi.spec.openapi.model.content.Responses
import com.davils.kapi.spec.openapi.model.content.ResponsesBuilder
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.Entry
import com.davils.kapi.spec.openapi.read.Values
import com.davils.kapi.spec.openapi.read.keyed
import com.davils.kapi.spec.openapi.read.mapped
import com.davils.kapi.spec.openapi.read.parameter.ParameterReader
import com.davils.kapi.spec.openapi.read.read
import com.davils.kapi.spec.openapi.read.referables
import com.davils.kapi.spec.openapi.read.reference.Referables
import com.davils.kapi.spec.openapi.read.reference.ReferenceKind
import com.davils.kapi.spec.openapi.read.server.ServerReader
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kapi.spec.openapi.read.shape.Shapes

internal object ResponseReader {
    fun responses(cursor: Cursor): Responses =
        cursor.keyed("The responses of an operation", ResponsesBuilder()) { entry -> status(this, entry) }

    fun response(cursor: Cursor): Response = cursor.read(Shapes.response, ResponseBuilder()) { fields ->
        description = fields.text(Keys.DESCRIPTION).orEmpty()
        headers.putAll(fields.entries(Keys.HEADERS).referables(ReferenceKind.Header, ParameterReader::header))
        content.putAll(fields.entries(Keys.CONTENT).mapped(ContentReader::mediaType))
        links.putAll(fields.entries(Keys.LINKS).referables(ReferenceKind.Link, ::link))
    }

    fun link(cursor: Cursor): Link = cursor.read(Shapes.link, LinkBuilder()) { fields ->
        operationReference = fields.text(Keys.OPERATION_REF)
        operationId = fields.text(Keys.OPERATION_ID)
        parameters.putAll(fields.entries(Keys.PARAMETERS).mapped(Values::of))
        requestBody = fields.value(Keys.REQUEST_BODY)
        description = fields.text(Keys.DESCRIPTION)
        server = fields.cursor(Keys.SERVER)?.let(ServerReader::server)
    }

    private fun status(builder: ResponsesBuilder, entry: Entry) {
        val status = ResponseStatus.parseOrNull(entry.key)
        if (status == null) {
            val message = "'${entry.key}' is no response status, which is a code, a range such as 4XX or default"
            return entry.report(DiagnosticCodes.RESPONSE_STATUS, message)
        }
        val response = Referables.read(entry.value, ReferenceKind.Response, ::response)
        response?.let { read -> builder.entries[status] = read }
    }
}
