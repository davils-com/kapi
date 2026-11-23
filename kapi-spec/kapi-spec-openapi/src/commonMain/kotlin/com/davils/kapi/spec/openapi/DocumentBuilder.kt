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
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Target
import com.davils.kapi.spec.openapi.model.component.Components
import com.davils.kapi.spec.openapi.model.info.ExternalDocs
import com.davils.kapi.spec.openapi.model.info.Info
import com.davils.kapi.spec.openapi.model.info.InfoBuilder
import com.davils.kapi.spec.openapi.model.info.Tag
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.model.path.Paths
import com.davils.kapi.spec.openapi.model.security.SecurityRequirement
import com.davils.kapi.spec.openapi.model.server.Server
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Builds a [Document]; the readers of kapi use it, and code may use it to build a document by hand.
 *
 * A builder is not safe to share between threads.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class DocumentBuilder internal constructor() : ElementBuilder<Document>() {
    override val subject: String get() = "openapi document"

    /**
     * The version of the document; [Version.Latest] by default.
     *
     * @since 1.0.0
     */
    public var version: Version = Version.Latest

    /**
     * The URI reference the document names itself by, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var self: String? = null

    /**
     * The `info` of the document.
     *
     * @since 1.0.0
     */
    public var info: Info = InfoBuilder().build()

    /**
     * The `jsonSchemaDialect` of the document, from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public var jsonSchemaDialect: String? = null

    internal val servers: MutableList<Server> = mutableListOf()

    /**
     * The `paths` of the document.
     *
     * @since 1.0.0
     */
    public var paths: Paths? = null

    internal val webhooks: MutableMap<String, PathItem> = linkedMapOf()

    /**
     * The `components` of the document.
     *
     * @since 1.0.0
     */
    public var components: Components? = null

    internal val security: MutableList<SecurityRequirement> = mutableListOf()

    internal val tags: MutableList<Tag> = mutableListOf()

    /**
     * The `externalDocs` of the document.
     *
     * @since 1.0.0
     */
    public var externalDocs: ExternalDocs? = null

    internal val targets: MutableMap<Target, Element> = linkedMapOf()

    override fun assemble(): Document = Document(this)
}
