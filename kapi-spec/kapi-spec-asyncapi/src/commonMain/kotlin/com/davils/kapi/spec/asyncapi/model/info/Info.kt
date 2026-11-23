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

package com.davils.kapi.spec.asyncapi.model.info

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable

/**
 * The info of a document: its title, its version and what else describes the application.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class Info internal constructor(builder: InfoBuilder) : Element(builder) {
    /**
     * The title of the application.
     *
     * @since 1.0.0
     */
    public val title: String = builder.title

    /**
     * The version of the application, not of AsyncAPI.
     *
     * @since 1.0.0
     */
    public val version: String = builder.version

    /**
     * A description, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The address of the terms of service.
     *
     * @since 1.0.0
     */
    public val termsOfService: String? = builder.termsOfService

    /**
     * Who to contact about the application.
     *
     * @since 1.0.0
     */
    public val contact: Contact? = builder.contact

    /**
     * The license of the application.
     *
     * @since 1.0.0
     */
    public val license: License? = builder.license

    /**
     * The tags of the application, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val tags: List<Referable<Tag>> = builder.tags.toList()

    /**
     * Further documentation, written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val externalDocs: Referable<ExternalDocs>? = builder.externalDocs

    override val identity: List<Any?>
        get() = listOf(
            title,
            version,
            description,
            termsOfService,
            contact,
            license,
            tags,
            externalDocs,
        )
}
