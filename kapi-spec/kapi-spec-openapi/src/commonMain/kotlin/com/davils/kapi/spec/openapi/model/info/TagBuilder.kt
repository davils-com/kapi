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

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of a [Tag], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the tag it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class TagBuilder internal constructor() : ElementBuilder<Tag>() {
    override val subject: String get() = "openapi tag"

    /**
     * The value of `name`, the name of the tag; empty until it is set.
     *
     * @since 1.0.0
     */
    public var name: String = ""

    /**
     * The value of `summary`, a short summary of the tag, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var summary: String? = null

    /**
     * The value of `description`, the description of the tag.
     *
     * @since 1.0.0
     */
    public var description: String? = null

    /**
     * The value of `externalDocs`, the external documentation of the tag.
     *
     * @since 1.0.0
     */
    public var externalDocs: ExternalDocs? = null

    /**
     * The value of `parent`, the name of the tag this tag is nested in, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var parent: String? = null

    /**
     * The value of `kind`, what the tag is for, such as `nav`, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var kind: String? = null

    override fun assemble(): Tag = Tag(this)
}
