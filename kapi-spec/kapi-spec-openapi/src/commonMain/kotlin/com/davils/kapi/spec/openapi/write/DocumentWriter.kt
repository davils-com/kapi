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
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.model.component.Components
import com.davils.kapi.spec.openapi.read.shape.Keys

internal class DocumentWriter(target: Document) {
    private val family: VersionFamily = target.version.family

    private val isCurrent: Boolean = family.isAtLeast(VersionFamily.V32)

    private val info: InfoWriter = InfoWriter(family)

    private val security: SecurityWriter = SecurityWriter(family)

    private val schemas: SchemaWriter = SchemaWriter(family, info)

    private val content: ContentWriter = ContentWriter(schemas, target)

    private val responses: ResponseWriter = ResponseWriter(content, info, family)

    private val paths: PathWriter = PathWriter(
        content = content,
        responses = responses,
        info = info,
        security = security,
        family = family,
    )

    fun document(document: Document): YamlNode {
        val out = Out()
        out[Keys.OPENAPI] = document.version.literal
        out[Keys.SELF] = document.self?.takeIf { isCurrent }
        out.node(Keys.INFO, info.info(document.info))
        out[Keys.JSON_SCHEMA_DIALECT] = document.jsonSchemaDialect
        out.list(Keys.SERVERS, document.servers, info::server)
        out.node(Keys.PATHS, document.paths?.let(paths::paths))
        out.map(Keys.WEBHOOKS, document.webhooks, paths::pathItem)
        out.node(Keys.COMPONENTS, document.components?.let(::components))
        out.list(Keys.SECURITY, document.security, security::requirement)
        out.list(Keys.TAGS, document.tags, info::tag)
        out.node(Keys.EXTERNAL_DOCS, document.externalDocs?.let(info::externalDocs))
        return out.build(document)
    }

    private fun components(components: Components): YamlNode {
        val out = Out()
        out.map(Keys.SCHEMAS, components.schemas, schemas::schema)
        out.map(Keys.RESPONSES, components.responses, ReferenceWriter.referables(responses::response))
        val parameters = content.parameters(components.parameters)
        out.map(Keys.PARAMETERS, parameters, ReferenceWriter.referables(content::parameter))
        out.map(Keys.EXAMPLES, components.examples, ReferenceWriter.referables(content::example))
        out.map(Keys.REQUEST_BODIES, components.requestBodies, ReferenceWriter.referables(content::requestBody))
        out.map(Keys.HEADERS, components.headers, ReferenceWriter.referables(content::header))
        out.map(Keys.SECURITY_SCHEMES, components.securitySchemes, ReferenceWriter.referables(security::scheme))
        out.map(Keys.LINKS, components.links, ReferenceWriter.referables(responses::link))
        out.map(Keys.CALLBACKS, components.callbacks, ReferenceWriter.referables(paths::callback))
        out.map(Keys.PATH_ITEMS, components.pathItems, paths::pathItem)
        val media = components.mediaTypes.takeIf { isCurrent }.orEmpty()
        out.map(Keys.MEDIA_TYPES, media, ReferenceWriter.referables(content::mediaType))
        return out.build(components)
    }
}
