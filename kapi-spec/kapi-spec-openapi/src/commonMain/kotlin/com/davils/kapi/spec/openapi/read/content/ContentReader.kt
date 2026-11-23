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

import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.model.content.Encoding
import com.davils.kapi.spec.openapi.model.content.EncodingBuilder
import com.davils.kapi.spec.openapi.model.content.Example
import com.davils.kapi.spec.openapi.model.content.ExampleBuilder
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.content.MediaTypeBuilder
import com.davils.kapi.spec.openapi.model.content.RequestBody
import com.davils.kapi.spec.openapi.model.content.RequestBodyBuilder
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.mapped
import com.davils.kapi.spec.openapi.read.parameter.ParameterReader
import com.davils.kapi.spec.openapi.read.read
import com.davils.kapi.spec.openapi.read.referables
import com.davils.kapi.spec.openapi.read.reference.ReferenceKind
import com.davils.kapi.spec.openapi.read.schema.SchemaReader
import com.davils.kapi.spec.openapi.read.shape.Fields
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kapi.spec.openapi.read.shape.Shapes

internal object ContentReader {
    fun mediaType(cursor: Cursor): MediaType = cursor.read(Shapes.mediaType, MediaTypeBuilder()) { fields ->
        schema = fields.cursor(Keys.SCHEMA)?.let(SchemaReader::read)
        itemSchema = fields.cursor(Keys.ITEM_SCHEMA)?.let(SchemaReader::read)
        example = fields.value(Keys.EXAMPLE)
        examples.putAll(examples(fields))
        encoding.putAll(fields.entries(Keys.ENCODING).mapped(::encoding))
        prefixEncoding.addAll(fields.items(Keys.PREFIX_ENCODING).map(::encoding))
        itemEncoding = fields.cursor(Keys.ITEM_ENCODING)?.let(::encoding)
    }

    fun content(fields: Fields): Map<String, Referable<MediaType>> {
        val entries = fields.entries(Keys.CONTENT)
        val isReferable = fields.cursor.scope.family
            .isAtLeast(VersionFamily.V32)
        if (isReferable) return entries.referables(ReferenceKind.MediaType, ::mediaType)
        return entries.mapped { cursor -> Referable.Inline(mediaType(cursor)) }
    }

    fun requestBody(cursor: Cursor): RequestBody = cursor.read(Shapes.requestBody, RequestBodyBuilder()) { fields ->
        description = fields.text(Keys.DESCRIPTION)
        content.putAll(content(fields))
        isRequired = fields.flag(Keys.REQUIRED)
    }

    fun example(cursor: Cursor): Example = cursor.read(Shapes.example, ExampleBuilder()) { fields ->
        summary = fields.text(Keys.SUMMARY)
        description = fields.text(Keys.DESCRIPTION)
        value = fields.value(Keys.VALUE)
        externalValue = fields.text(Keys.EXTERNAL_VALUE)
        dataValue = fields.value(Keys.DATA_VALUE)
        serializedValue = fields.text(Keys.SERIALIZED_VALUE)
    }

    fun examples(fields: Fields): Map<String, Referable<Example>> =
        fields.entries(Keys.EXAMPLES).referables(ReferenceKind.Example, ::example)

    private fun encoding(cursor: Cursor): Encoding = cursor.read(Shapes.encoding, EncodingBuilder()) { fields ->
        contentType = fields.text(Keys.CONTENT_TYPE)
        headers.putAll(fields.entries(Keys.HEADERS).referables(ReferenceKind.Header, ParameterReader::header))
        style = ParameterReader.styleOf(fields)
        isExploded = fields.flagOrNull(Keys.EXPLODE)
        isReservedAllowed = fields.flag(Keys.ALLOW_RESERVED)
        encoding.putAll(fields.entries(Keys.ENCODING).mapped(::encoding))
        prefixEncoding.addAll(fields.items(Keys.PREFIX_ENCODING).map(::encoding))
        itemEncoding = fields.cursor(Keys.ITEM_ENCODING)?.let(::encoding)
    }
}
