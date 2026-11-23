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

package com.davils.kapi.spec.openapi.model.info

import com.davils.kapi.spec.core.model.Element

/**
 * The `info` object of an OpenAPI document: the metadata of the API, which tools may show to readers.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class Info internal constructor(builder: InfoBuilder) : Element(builder) {
    /**
     * The `title` of the API; required by OpenAPI, so a reading reports a document that leaves it out and keeps it
     * empty here.
     *
     * @since 1.0.0
     */
    public val title: String = builder.title

    /**
     * The `version` of the document, such as `1.4.0`, which is not the version of OpenAPI; required by OpenAPI, so a
     * reading reports a document that leaves it out and keeps it empty here.
     *
     * @since 1.0.0
     */
    public val version: String = builder.version

    /**
     * The `summary` of the API in a few words; `null` when the document leaves it out and before OpenAPI 3.1.
     *
     * @since 1.0.0
     */
    public val summary: String? = builder.summary

    /**
     * The `description` of the API, which may use CommonMark; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The `termsOfService` of the API as a URL, as it was written and not checked; `null` when the document leaves it
     * out.
     *
     * @since 1.0.0
     */
    public val termsOfService: String? = builder.termsOfService

    /**
     * The `contact` of the API; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val contact: Contact? = builder.contact

    /**
     * The `license` of the API; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val license: License? = builder.license

    override val identity: List<Any?>
        get() = listOf(
            title,
            version,
            summary,
            description,
            termsOfService,
            contact,
            license,
        )
}
