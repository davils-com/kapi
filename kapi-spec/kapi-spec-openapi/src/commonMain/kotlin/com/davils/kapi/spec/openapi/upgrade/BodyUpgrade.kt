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

package com.davils.kapi.spec.openapi.upgrade

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.core.node.yamlScalar
import com.davils.kapi.spec.openapi.read.shape.Keys

private val formTypes: Set<String> = setOf(SwaggerKeys.MULTIPART, SwaggerKeys.FORM)

private val descriptionKeys: Set<String> = setOf(Keys.DESCRIPTION)

internal class BodyUpgrade(
    private val schemas: SimpleSchemaUpgrade,
    private val styles: CollectionStyles,
) {
    fun fromBody(parameter: YamlNode, consumes: List<String>): YamlNode {
        val schema = parameter[Keys.SCHEMA]?.let(SchemaUpgrade::upgraded)
        val media = Trees.mappingOf(Trees.entries(Keys.SCHEMA to schema))
        val content = Trees.mappingOf(consumes.map { type -> Trees.entry(type, media) })
        val isRequired = Trees.isTrue(parameter[Keys.REQUIRED])
        val required = Trees.entries(Keys.REQUIRED to yamlScalar(true).takeIf { isRequired })
        val described = Trees.only(parameter, descriptionKeys)
        return Trees.mappingAt(
            parameter,
            buildList {
                addAll(described)
                addAll(required)
                add(Trees.entry(Keys.CONTENT, content))
                addAll(Trees.extensions(parameter))
            },
        )
    }

    fun fromForm(fields: List<Located>, consumes: List<String>): YamlNode {
        val properties = Trees.mappingOf(fields.mapNotNull(::propertyOf))
        val names = fields.filter { field -> Trees.isTrue(field.node[Keys.REQUIRED]) }
        val required = names.mapNotNull { field -> field.node[Keys.NAME] }
        val schema = Trees.mappingOf(
            Trees.entries(
                Keys.TYPE to Trees.textOf(SwaggerKeys.OBJECT),
                Keys.PROPERTIES to properties,
                Keys.REQUIRED to Trees.sequenceOf(required).takeIf { required.isNotEmpty() },
            ),
        )
        val content = typesOf(fields, consumes).map { type -> Trees.entry(type, mediaOf(type, schema, fields)) }
        val requirement = yamlScalar(true).takeIf { required.isNotEmpty() }
        return Trees.mappingOf(Trees.entries(Keys.REQUIRED to requirement, Keys.CONTENT to Trees.mappingOf(content)))
    }

    private fun propertyOf(field: Located): YamlEntry? {
        val name = field.node[Keys.NAME].string() ?: return null
        val schema = schemas.schemaOf(field) ?: Trees.mappingOf(emptyList())
        val described = Trees.only(field.node, descriptionKeys)
        return Trees.entry(name, Trees.mappingOf(Trees.entriesOf(schema) + described))
    }

    private fun typesOf(fields: List<Located>, consumes: List<String>): List<String> {
        val declared = consumes.filter { type -> type in formTypes }
        val hasFile = fields.any { field -> field.node[Keys.TYPE].string() == SwaggerKeys.FILE }
        val fallback = SwaggerKeys.MULTIPART.takeIf { hasFile } ?: SwaggerKeys.FORM
        return declared.ifEmpty { listOf(fallback) }
    }

    private fun mediaOf(type: String, schema: YamlNode, fields: List<Located>): YamlNode {
        val encoded = fields.filter { _ -> type == SwaggerKeys.FORM }
        val encoding = Trees.mappingOrNull(encoded.mapNotNull(::encodingOf))
        return Trees.mappingOf(Trees.entries(Keys.SCHEMA to schema, Keys.ENCODING to encoding))
    }

    private fun encodingOf(field: Located): YamlEntry? {
        val name = field.node[Keys.NAME].string() ?: return null
        return Trees.mappingOrNull(styles.styleOf(field))?.let { style -> Trees.entry(name, style) }
    }
}
