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

package com.davils.kapi.spec.openapi.read.component

import com.davils.kapi.spec.openapi.model.component.Components
import com.davils.kapi.spec.openapi.model.component.ComponentsBuilder
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.content.ContentReader
import com.davils.kapi.spec.openapi.read.content.ResponseReader
import com.davils.kapi.spec.openapi.read.mapped
import com.davils.kapi.spec.openapi.read.parameter.ParameterReader
import com.davils.kapi.spec.openapi.read.path.PathReader
import com.davils.kapi.spec.openapi.read.read
import com.davils.kapi.spec.openapi.read.referables
import com.davils.kapi.spec.openapi.read.reference.ReferenceKind
import com.davils.kapi.spec.openapi.read.schema.SchemaReader
import com.davils.kapi.spec.openapi.read.security.SecuritySchemeReader
import com.davils.kapi.spec.openapi.read.shape.Fields
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kapi.spec.openapi.read.shape.Shapes

internal object ComponentsReader {
    fun components(cursor: Cursor): Components = cursor.read(Shapes.components, ComponentsBuilder()) { fields ->
        schemas.putAll(fields.entries(Keys.SCHEMAS).mapped(SchemaReader::read))
        pathItems.putAll(fields.entries(Keys.PATH_ITEMS).mapped(PathReader::pathItem))
        messages(this, fields)
        parts(this, fields)
    }

    private fun messages(builder: ComponentsBuilder, fields: Fields) {
        val responses = fields.entries(Keys.RESPONSES)
        builder.responses.putAll(responses.referables(ReferenceKind.Response, ResponseReader::response))
        val requestBodies = fields.entries(Keys.REQUEST_BODIES)
        builder.requestBodies.putAll(requestBodies.referables(ReferenceKind.RequestBody, ContentReader::requestBody))
        val links = fields.entries(Keys.LINKS)
        builder.links.putAll(links.referables(ReferenceKind.Link, ResponseReader::link))
        val callbacks = fields.entries(Keys.CALLBACKS)
        builder.callbacks.putAll(callbacks.referables(ReferenceKind.Callback, PathReader::callback))
    }

    private fun parts(builder: ComponentsBuilder, fields: Fields) {
        val parameters = fields.entries(Keys.PARAMETERS)
        builder.parameters.putAll(parameters.referables(ReferenceKind.Parameter, ParameterReader::parameter))
        val examples = fields.entries(Keys.EXAMPLES)
        builder.examples.putAll(examples.referables(ReferenceKind.Example, ContentReader::example))
        val headers = fields.entries(Keys.HEADERS)
        builder.headers.putAll(headers.referables(ReferenceKind.Header, ParameterReader::header))
        val schemes = fields.entries(Keys.SECURITY_SCHEMES)
        builder.securitySchemes.putAll(schemes.referables(ReferenceKind.SecurityScheme, SecuritySchemeReader::scheme))
    }
}
