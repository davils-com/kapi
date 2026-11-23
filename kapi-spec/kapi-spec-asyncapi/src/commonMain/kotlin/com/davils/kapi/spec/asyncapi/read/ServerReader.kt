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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kapi.spec.asyncapi.model.server.Server
import com.davils.kapi.spec.asyncapi.model.server.ServerBuilder
import com.davils.kapi.spec.asyncapi.model.server.ServerVariable
import com.davils.kapi.spec.asyncapi.model.server.ServerVariableBuilder
import com.davils.kapi.spec.asyncapi.read.reference.ReferenceKind
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.asyncapi.read.shape.Shapes

internal object ServerReader {
    fun server(cursor: Cursor): Server = cursor.read(Shapes.server, ServerBuilder()) { fields ->
        host = fields.text(Keys.HOST).orEmpty()
        protocol = fields.text(Keys.PROTOCOL).orEmpty()
        protocolVersion = fields.text(Keys.PROTOCOL_VERSION)
        pathname = fields.text(Keys.PATHNAME)
        description = fields.text(Keys.DESCRIPTION)
        title = fields.text(Keys.TITLE)
        summary = fields.text(Keys.SUMMARY)
        variables.putAll(fields.entries(Keys.VARIABLES).referables(ReferenceKind.ServerVariable, ::variable))
        security.addAll(SecurityReader.schemesOf(fields))
        tags.addAll(InfoReader.tagsOf(fields))
        externalDocs = InfoReader.docsOf(fields)
        bindings = fields.cursor(Keys.BINDINGS)?.referable(ReferenceKind.ServerBindings, BindingReader::server)
    }

    fun variable(cursor: Cursor): ServerVariable = cursor.read(
        Shapes.serverVariable,
        ServerVariableBuilder(),
    ) { fields ->
        enumeration.addAll(fields.texts(Keys.ENUM))
        default = fields.text(Keys.DEFAULT)
        description = fields.text(Keys.DESCRIPTION)
        examples.addAll(fields.texts(Keys.EXAMPLES))
    }
}
