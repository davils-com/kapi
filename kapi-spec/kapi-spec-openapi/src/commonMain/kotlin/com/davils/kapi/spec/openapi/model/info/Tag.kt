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
 * A `tag` object of a document: a label that groups operations, which name it in their `tags`.
 *
 * The element is immutable and safe to share between threads. A reading reports a tag whose name is declared twice.
 *
 * @since 1.0.0
 */
public class Tag internal constructor(builder: TagBuilder) : Element(builder) {
    /**
     * The `name` of the tag, which operations refer to; required by OpenAPI, so a reading reports a tag that leaves it
     * out and keeps it empty here.
     *
     * @since 1.0.0
     */
    public val name: String = builder.name

    /**
     * The `summary` of the tag in a few words; `null` when the document leaves it out and before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val summary: String? = builder.summary

    /**
     * The `description` of the tag, which may use CommonMark; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The `externalDocs` of the tag; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val externalDocs: ExternalDocs? = builder.externalDocs

    /**
     * The `parent` of the tag: the name of the tag it is nested in; `null` for a tag at the top and before
     * OpenAPI 3.2.
     *
     * A reading reports a parent that names no tag of the document and parents that lead back to the tag.
     *
     * @since 1.0.0
     */
    public val parent: String? = builder.parent

    /**
     * The `kind` of the tag: what it is for, such as `nav`, `badge` or `audience`; free text, `null` when the
     * document leaves it out and before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val kind: String? = builder.kind

    override val identity: List<Any?> get() = listOf(name, summary, description, externalDocs, parent, kind)
}
