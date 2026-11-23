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

package com.davils.kapi.spec.openapi.read.reference

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.content.ContentReader
import com.davils.kapi.spec.openapi.read.content.ResponseReader
import com.davils.kapi.spec.openapi.read.parameter.ParameterReader
import com.davils.kapi.spec.openapi.read.path.PathReader
import com.davils.kapi.spec.openapi.read.schema.SchemaReader
import com.davils.kapi.spec.openapi.read.security.SecuritySchemeReader

internal object TargetReaders {
    private val readers: Map<ReferenceKind, (Cursor) -> Referable<Element>?> = mapOf(
        ReferenceKind.Schema to { cursor -> Referable.Inline(SchemaReader.read(cursor)) },
        ReferenceKind.PathItem to { cursor -> Referable.Inline(PathReader.pathItem(cursor)) },
        ReferenceKind.Response to referable(ReferenceKind.Response, ResponseReader::response),
        ReferenceKind.Parameter to referable(ReferenceKind.Parameter, ParameterReader::parameter),
        ReferenceKind.Example to referable(ReferenceKind.Example, ContentReader::example),
        ReferenceKind.RequestBody to referable(ReferenceKind.RequestBody, ContentReader::requestBody),
        ReferenceKind.Header to referable(ReferenceKind.Header, ParameterReader::header),
        ReferenceKind.SecurityScheme to referable(ReferenceKind.SecurityScheme, SecuritySchemeReader::scheme),
        ReferenceKind.Link to referable(ReferenceKind.Link, ResponseReader::link),
        ReferenceKind.Callback to referable(ReferenceKind.Callback, PathReader::callback),
        ReferenceKind.MediaType to referable(ReferenceKind.MediaType, ContentReader::mediaType),
    )

    fun read(kind: ReferenceKind, cursor: Cursor): Referable<Element>? = readers.getValue(kind).invoke(cursor)

    private fun referable(kind: ReferenceKind, inline: (Cursor) -> Element?): (Cursor) -> Referable<Element>? =
        { cursor -> Referables.read(cursor, kind, inline) }
}
