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

package com.davils.kapi.spec.openapi.write

import com.davils.kaml.core.node.YamlNode
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.model.content.Encoding
import com.davils.kapi.spec.openapi.model.content.Example
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.content.RequestBody
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.read.shape.Keys

internal class ContentWriter(private val schemas: SchemaWriter, family: VersionFamily) {
    private val isCurrent: Boolean = family.isAtLeast(VersionFamily.V32)

    fun parameter(parameter: Parameter): YamlNode {
        val out = Out()
        out.text(Keys.NAME, parameter.name)
        out.text(Keys.IN, parameter.placement.keyword)
        out.text(Keys.DESCRIPTION, parameter.description)
        out.flag(Keys.REQUIRED, parameter.isRequired)
        out.flag(Keys.DEPRECATED, parameter.isDeprecated)
        out.flag(Keys.ALLOW_EMPTY_VALUE, parameter.isEmptyValueAllowed)
        out.text(Keys.STYLE, parameter.style?.keyword)
        out.choice(Keys.EXPLODE, parameter.isExploded)
        out.flag(Keys.ALLOW_RESERVED, parameter.isReservedAllowed)
        out.node(Keys.SCHEMA, parameter.schema?.let(schemas::schema))
        out.map(Keys.CONTENT, parameter.content, ReferenceWriter.referables(::mediaType))
        out.node(Keys.EXAMPLE, parameter.example?.let(Nodes::of))
        out.map(Keys.EXAMPLES, parameter.examples, ReferenceWriter.referables(::example))
        return out.build(parameter)
    }

    fun header(header: Header): YamlNode {
        val out = Out()
        out.text(Keys.DESCRIPTION, header.description)
        out.flag(Keys.REQUIRED, header.isRequired)
        out.flag(Keys.DEPRECATED, header.isDeprecated)
        out.text(Keys.STYLE, header.style?.keyword)
        out.choice(Keys.EXPLODE, header.isExploded)
        out.node(Keys.SCHEMA, header.schema?.let(schemas::schema))
        out.map(Keys.CONTENT, header.content, ReferenceWriter.referables(::mediaType))
        out.node(Keys.EXAMPLE, header.example?.let(Nodes::of))
        out.map(Keys.EXAMPLES, header.examples, ReferenceWriter.referables(::example))
        return out.build(header)
    }

    fun mediaType(media: MediaType): YamlNode {
        val out = Out()
        out.node(Keys.SCHEMA, media.schema?.let(schemas::schema))
        out.node(Keys.ITEM_SCHEMA, media.itemSchema?.takeIf { isCurrent }?.let(schemas::schema))
        out.node(Keys.EXAMPLE, media.example?.let(Nodes::of))
        out.map(Keys.EXAMPLES, media.examples, ReferenceWriter.referables(::example))
        out.map(Keys.ENCODING, media.encoding, ::encoding)
        nested(out, media.prefixEncoding, media.itemEncoding)
        return out.build(media)
    }

    fun requestBody(body: RequestBody): YamlNode {
        val out = Out()
        out.text(Keys.DESCRIPTION, body.description)
        out.node(Keys.CONTENT, Nodes.mapping(body.content, ReferenceWriter.referables(::mediaType)))
        out.flag(Keys.REQUIRED, body.isRequired)
        return out.build(body)
    }

    fun example(example: Example): YamlNode {
        val out = Out()
        out.text(Keys.SUMMARY, example.summary)
        out.text(Keys.DESCRIPTION, example.description)
        out.node(Keys.VALUE, example.value?.let(Nodes::of))
        out.text(Keys.EXTERNAL_VALUE, example.externalValue)
        out.node(Keys.DATA_VALUE, example.dataValue?.takeIf { isCurrent }?.let(Nodes::of))
        out.text(Keys.SERIALIZED_VALUE, example.serializedValue?.takeIf { isCurrent })
        return out.build(example)
    }

    private fun encoding(encoding: Encoding): YamlNode {
        val out = Out()
        out.text(Keys.CONTENT_TYPE, encoding.contentType)
        out.map(Keys.HEADERS, encoding.headers, ReferenceWriter.referables(::header))
        out.text(Keys.STYLE, encoding.style?.keyword)
        out.choice(Keys.EXPLODE, encoding.isExploded)
        out.flag(Keys.ALLOW_RESERVED, encoding.isReservedAllowed)
        out.map(Keys.ENCODING, encoding.nestedEncoding.takeIf { isCurrent }.orEmpty(), ::encoding)
        nested(out, encoding.prefixEncoding, encoding.itemEncoding)
        return out.build(encoding)
    }

    private fun nested(out: Out, prefix: List<Encoding>, item: Encoding?) {
        out.list(Keys.PREFIX_ENCODING, prefix.takeIf { isCurrent }.orEmpty(), ::encoding)
        out.node(Keys.ITEM_ENCODING, item?.takeIf { isCurrent }?.let(::encoding))
    }
}
