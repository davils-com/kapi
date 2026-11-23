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

public class Tag internal constructor(builder: TagBuilder) : Element(builder) {
    public val name: String = builder.name

    /**
     * A short summary of the tag; `null` before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val summary: String? = builder.summary

    public val description: String? = builder.description

    public val externalDocs: ExternalDocs? = builder.externalDocs

    /**
     * The name of the tag this tag is nested in; `null` for a tag at the top and before OpenAPI 3.2.
     *
     * A reading reports a parent that names no tag of the document and parents that lead back to the tag.
     *
     * @since 1.0.0
     */
    public val parent: String? = builder.parent

    /**
     * What the tag is for, such as `nav`, `badge` or `audience`; free text, `null` before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val kind: String? = builder.kind

    override val identity: List<Any?> get() = listOf(name, summary, description, externalDocs, parent, kind)
}
