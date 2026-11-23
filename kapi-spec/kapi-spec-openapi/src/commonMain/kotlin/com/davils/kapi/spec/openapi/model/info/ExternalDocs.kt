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
 * The `externalDocs` object: a reference to documentation outside the document, which a document, an operation, a
 * tag or a schema may name.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class ExternalDocs internal constructor(builder: ExternalDocsBuilder) :
    Element(builder) {
    /**
     * The `url` of the documentation, as it was written and not checked; required by OpenAPI, so a reading reports
     * external documentation that leaves it out and keeps it empty here.
     *
     * @since 1.0.0
     */
    public val url: String = builder.url

    /**
     * The `description` of the documentation, which may use CommonMark; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    override val identity: List<Any?> get() = listOf(url, description)
}
