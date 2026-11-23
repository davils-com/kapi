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

package com.davils.kapi.spec.asyncapi.model.server

import com.davils.kapi.spec.core.collection.frozen
import com.davils.kapi.spec.core.model.Element
import com.davils.kore.core.annotation.DavilsInternal

/**
 * A variable of the host or the path of a server.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class ServerVariable internal constructor(builder: ServerVariableBuilder) : Element(builder) {
    /**
     * The values the variable may take; empty when any value is allowed.
     *
     * @since 1.0.0
     */
    public val enumeration: List<String> = builder.enumeration.frozen()

    /**
     * The value used when a client supplies none.
     *
     * @since 1.0.0
     */
    public val default: String? = builder.default

    /**
     * A description, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * Examples of values of the variable.
     *
     * @since 1.0.0
     */
    public val examples: List<String> = builder.examples.frozen()

    override val identity: List<Any?> get() = listOf(enumeration, default, description, examples)
}
