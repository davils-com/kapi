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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.asyncapi.model.operation

import com.davils.kapi.spec.asyncapi.model.binding.OperationBindings
import com.davils.kapi.spec.asyncapi.model.info.ExternalDocs
import com.davils.kapi.spec.asyncapi.model.info.Tag
import com.davils.kapi.spec.asyncapi.model.security.SecurityScheme
import com.davils.kapi.spec.core.collection.frozen
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kore.core.annotation.DavilsInternal

/**
 * A trait of an operation: fields an operation takes over unless it declares them itself.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class OperationTrait internal constructor(builder: OperationTraitBuilder) : Element(builder) {
    /**
     * A human-friendly title.
     *
     * @since 1.0.0
     */
    public val title: String? = builder.title

    /**
     * A short summary of what it is about.
     *
     * @since 1.0.0
     */
    public val summary: String? = builder.summary

    /**
     * A description, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The security schemes the trait gives an operation.
     *
     * @since 1.0.0
     */
    public val security: List<Referable<SecurityScheme>> = builder.security.frozen()

    /**
     * The tags, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val tags: List<Referable<Tag>> = builder.tags.frozen()

    /**
     * Further documentation, written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val externalDocs: Referable<ExternalDocs>? = builder.externalDocs

    /**
     * The protocol-specific details the trait gives an operation.
     *
     * @since 1.0.0
     */
    public val bindings: Referable<OperationBindings>? = builder.bindings

    override val identity: List<Any?>
        get() = listOf(
            title,
            summary,
            description,
            security,
            tags,
            externalDocs,
            bindings,
        )
}
