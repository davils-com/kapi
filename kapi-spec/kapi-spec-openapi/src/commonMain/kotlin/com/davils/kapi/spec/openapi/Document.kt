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

package com.davils.kapi.spec.openapi

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Target
import com.davils.kapi.spec.openapi.model.component.Components
import com.davils.kapi.spec.openapi.model.info.ExternalDocs
import com.davils.kapi.spec.openapi.model.info.Info
import com.davils.kapi.spec.openapi.model.info.Tag
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.model.path.Paths
import com.davils.kapi.spec.openapi.model.security.SecurityRequirement
import com.davils.kapi.spec.openapi.model.server.Server

public class Document internal constructor(builder: DocumentBuilder) :
    Element(builder) {
    public val version: Version = builder.version

    public val info: Info = builder.info

    public val jsonSchemaDialect: String? = builder.jsonSchemaDialect

    public val servers: List<Server> = builder.servers.toList()

    public val paths: Paths? = builder.paths

    public val webhooks: Map<String, PathItem> = builder.webhooks.toMap()

    public val components: Components? = builder.components

    public val security: List<SecurityRequirement> = builder.security.toList()

    public val tags: List<Tag> = builder.tags.toList()

    public val externalDocs: ExternalDocs? = builder.externalDocs

    public val targets: Map<Target, Element> = builder.targets.toMap()

    override val identity: List<Any?>
        get() = listOf(
            version,
            info,
            jsonSchemaDialect,
            servers,
            paths,
            webhooks,
            components,
            security,
            tags,
            externalDocs,
            targets,
        )
}
