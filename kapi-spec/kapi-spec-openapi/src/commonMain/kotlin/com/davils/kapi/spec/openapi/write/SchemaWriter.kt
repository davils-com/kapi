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
import com.davils.kaml.core.node.yamlScalar
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.model.schema.Discriminator
import com.davils.kapi.spec.openapi.model.schema.FixedSchema
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.model.schema.Xml
import com.davils.kapi.spec.openapi.read.shape.Keys

internal class SchemaWriter(family: VersionFamily) {
    private val keywords: SchemaKeywordWriter = SchemaKeywordWriter(family)

    fun schema(schema: Schema): YamlNode = when (schema) {
        is FixedSchema -> yamlScalar(schema.isAccepting)
        is SchemaDefinition -> definition(schema)
    }

    private fun definition(schema: SchemaDefinition): YamlNode {
        val out = Out()
        out.text(Keys.REFERENCE, schema.reference?.text)
        identity(out, schema)
        keywords.typing(out, schema)
        keywords.constraints(out, schema)
        structure(out, schema)
        composition(out, schema)
        annotations(out, schema)
        schema.unknownKeywords.forEach { (name, value) -> out.node(name, Nodes.of(value)) }
        return out.build(schema)
    }

    private fun identity(out: Out, schema: SchemaDefinition) {
        out.text(Keys.SCHEMA_ID, schema.id)
        out.text(Keys.DIALECT, schema.dialect)
        out.text(Keys.ANCHOR, schema.anchor)
        out.text(Keys.DYNAMIC_ANCHOR, schema.dynamicAnchor)
        out.text(Keys.DYNAMIC_REFERENCE, schema.dynamicReference)
        out.text(Keys.COMMENT, schema.comment)
        out.map(Keys.DEFINITIONS, schema.definitions, ::schema)
        out.text(Keys.TITLE, schema.title)
        out.text(Keys.DESCRIPTION, schema.description)
    }

    private fun structure(out: Out, schema: SchemaDefinition) {
        out.node(Keys.ITEMS, schema.items?.let(::schema))
        out.list(Keys.PREFIX_ITEMS, schema.prefixItems, ::schema)
        out.node(Keys.CONTAINS, schema.contains?.let(::schema))
        out.node(Keys.UNEVALUATED_ITEMS, schema.unevaluatedItems?.let(::schema))
        out.map(Keys.PROPERTIES, schema.properties, ::schema)
        out.map(Keys.PATTERN_PROPERTIES, schema.patternProperties, ::schema)
        out.node(Keys.ADDITIONAL_PROPERTIES, schema.additionalProperties?.let(::schema))
        out.node(Keys.PROPERTY_NAMES, schema.propertyNames?.let(::schema))
        out.map(Keys.DEPENDENT_SCHEMAS, schema.dependentSchemas, ::schema)
        out.node(Keys.UNEVALUATED_PROPERTIES, schema.unevaluatedProperties?.let(::schema))
    }

    private fun composition(out: Out, schema: SchemaDefinition) {
        out.list(Keys.ALL_OF, schema.allOf, ::schema)
        out.list(Keys.ANY_OF, schema.anyOf, ::schema)
        out.list(Keys.ONE_OF, schema.oneOf, ::schema)
        out.node(Keys.NOT, schema.not?.let(::schema))
        out.node(Keys.IF, schema.condition?.let(::schema))
        out.node(Keys.THEN, schema.consequence?.let(::schema))
        out.node(Keys.ELSE, schema.alternative?.let(::schema))
        out.node(Keys.CONTENT_SCHEMA, schema.contentSchema?.let(::schema))
    }

    private fun annotations(out: Out, schema: SchemaDefinition) {
        out.node(Keys.DEFAULT, schema.default?.let(Nodes::of))
        out.flag(Keys.DEPRECATED, schema.isDeprecated)
        out.flag(Keys.READ_ONLY, schema.isReadOnly)
        out.flag(Keys.WRITE_ONLY, schema.isWriteOnly)
        out.text(Keys.CONTENT_ENCODING, schema.contentEncoding)
        out.text(Keys.CONTENT_MEDIA_TYPE, schema.contentMediaType)
        out.node(Keys.DISCRIMINATOR, schema.discriminator?.let(::discriminator))
        out.node(Keys.XML, schema.xml?.let(::xml))
        out.node(Keys.EXTERNAL_DOCS, schema.externalDocs?.let(InfoWriter::externalDocs))
    }

    private fun discriminator(discriminator: Discriminator): YamlNode {
        val out = Out()
        out.text(Keys.PROPERTY_NAME, discriminator.propertyName)
        out.map(Keys.MAPPING, discriminator.mapping) { target -> yamlScalar(target) }
        return out.build(discriminator)
    }

    private fun xml(xml: Xml): YamlNode {
        val out = Out()
        out.text(Keys.NAME, xml.name)
        out.text(Keys.NAMESPACE, xml.namespace)
        out.text(Keys.PREFIX, xml.prefix)
        out.flag(Keys.ATTRIBUTE, xml.isAttribute)
        out.flag(Keys.WRAPPED, xml.isWrapped)
        return out.build(xml)
    }
}
