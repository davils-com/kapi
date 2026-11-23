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
 * A tag that groups servers, channels, operations or messages.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class Tag internal constructor(builder: TagBuilder) : Element(builder) {
    /**
     * The name of the tag.
     *
     * @since 1.0.0
     */
    public val name: String = builder.name

    /**
     * A description, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * Further documentation, written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val externalDocs: Referable<ExternalDocs>? = builder.externalDocs

    override val identity: List<Any?> get() = listOf(name, description, externalDocs)
}
