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

package com.davils.kapi.spec.openapi.read.schema

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.schema.Discriminator
import com.davils.kapi.spec.openapi.model.schema.DiscriminatorBuilder
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinitionBuilder
import com.davils.kapi.spec.openapi.model.schema.Xml
import com.davils.kapi.spec.openapi.model.schema.XmlBuilder
import com.davils.kapi.spec.openapi.model.schema.XmlNodeType
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.Values
import com.davils.kapi.spec.openapi.read.info.InfoReader
import com.davils.kapi.spec.openapi.read.read
import com.davils.kapi.spec.openapi.read.shape.Fields
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kapi.spec.openapi.read.shape.Shapes

internal object SchemaAnnotations {
    fun fill(builder: SchemaDefinitionBuilder, fields: Fields) {
        builder.title = fields.text(Keys.TITLE)
        builder.description = fields.text(Keys.DESCRIPTION)
        builder.default = fields.value(Keys.DEFAULT)
        builder.examples.addAll(fields.items(Keys.EXAMPLES).map(Values::of))
        builder.examples.addAll(listOfNotNull(fields.value(Keys.EXAMPLE)))
        builder.isDeprecated = fields.flag(Keys.DEPRECATED)
        builder.isReadOnly = fields.flag(Keys.READ_ONLY)
        builder.isWriteOnly = fields.flag(Keys.WRITE_ONLY)
        builder.contentEncoding = fields.text(Keys.CONTENT_ENCODING)
        builder.contentMediaType = fields.text(Keys.CONTENT_MEDIA_TYPE)
        builder.discriminator = fields.cursor(Keys.DISCRIMINATOR)?.let(::discriminator)
        builder.xml = fields.cursor(Keys.XML)?.let(::xml)
        builder.externalDocs = fields.cursor(Keys.EXTERNAL_DOCS)?.let(InfoReader::externalDocs)
    }

    private fun discriminator(cursor: Cursor): Discriminator =
        cursor.read(Shapes.discriminator, DiscriminatorBuilder()) { fields ->
            propertyName = fields.text(Keys.PROPERTY_NAME).orEmpty()
            mapping.putAll(fields.textsByName(Keys.MAPPING))
            defaultMapping = fields.text(Keys.DEFAULT_MAPPING)
        }

    private fun xml(cursor: Cursor): Xml = cursor.read(Shapes.xml, XmlBuilder()) { fields ->
        name = fields.text(Keys.NAME)
        namespace = fields.text(Keys.NAMESPACE)
        prefix = fields.text(Keys.PREFIX)
        isAttribute = fields.flag(Keys.ATTRIBUTE)
        isWrapped = fields.flag(Keys.WRAPPED)
        nodeType = nodeTypeOf(fields)
    }

    private fun nodeTypeOf(fields: Fields): XmlNodeType? {
        val text = fields.text(Keys.NODE_TYPE) ?: return null
        val type = XmlNodeType.fromKeyword(text)
        if (type == null) {
            val message = "'$text' is no node type, which is element, attribute, text, cdata or none"
            fields.report(Keys.NODE_TYPE, DiagnosticCodes.SCHEMA_XML_NODE_TYPE, message)
        }
        val legacy = listOf(Keys.ATTRIBUTE, Keys.WRAPPED).filter { name -> fields.cursor(name) != null }
        legacy.forEach { name ->
            val message = "$name is deprecated and stands beside nodeType, which replaces it"
            fields.report(name, DiagnosticCodes.SCHEMA_XML_DEPRECATED, message)
        }
        return type
    }
}
