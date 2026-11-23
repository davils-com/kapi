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

/**
 * An OpenAPI document as it was read, with every element a reference leads to.
 *
 * A document is immutable and safe to share between threads. Use [resolve] to follow a reference.
 *
 * @param builder the builder whose state the document takes over.
 * @since 1.0.0
 */
public class Document internal constructor(builder: DocumentBuilder) :
    Element(builder) {
    /**
     * The version the document declares in its field `openapi`.
     *
     * @since 1.0.0
     */
    public val version: Version = builder.version

    /**
     * The `$self` of the document, the URI reference it names itself by and its relative references resolve
     * against; `null` when the document leaves it out and before OpenAPI 3.2.
     *
     * A reading resolves the references of the document against it, relative to the address the document was read
     * from, and treats a reference to it as a reference to the document itself; every other document a reference
     * leads to is still loaded through the resolver and the reference policy, so `$self` widens no confinement.
     *
     * @since 1.0.0
     */
    public val self: String? = builder.self

    /**
     * The `info` of the document: its title, version and contact.
     *
     * @since 1.0.0
     */
    public val info: Info = builder.info

    /**
     * The `jsonSchemaDialect` of the document, the default `$schema` of its schemas, from OpenAPI 3.1 on; `null` when absent.
     *
     * @since 1.0.0
     */
    public val jsonSchemaDialect: String? = builder.jsonSchemaDialect

    /**
     * The `servers` of the document, in their order.
     *
     * @since 1.0.0
     */
    public val servers: List<Server> = builder.servers.toList()

    /**
     * The `paths` of the document; `null` when absent, which OpenAPI 3.1 allows.
     *
     * @since 1.0.0
     */
    public val paths: Paths? = builder.paths

    /**
     * The `webhooks` of the document by name, from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public val webhooks: Map<String, PathItem> = builder.webhooks.toMap()

    /**
     * The `components` of the document; `null` when absent.
     *
     * @since 1.0.0
     */
    public val components: Components? = builder.components

    /**
     * The `security` requirements of every operation that declares none of its own; one of them must be met.
     *
     * @since 1.0.0
     */
    public val security: List<SecurityRequirement> = builder.security.toList()

    /**
     * The `tags` of the document, in their order.
     *
     * @since 1.0.0
     */
    public val tags: List<Tag> = builder.tags.toList()

    /**
     * The `externalDocs` of the document; `null` when absent.
     *
     * @since 1.0.0
     */
    public val externalDocs: ExternalDocs? = builder.externalDocs

    /**
     * Every element a reference of the document or of a document it references leads to, by its place.
     *
     * @since 1.0.0
     */
    public val targets: Map<Target, Element> = builder.targets.toMap()

    override val identity: List<Any?>
        get() = listOf(
            version,
            self,
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
