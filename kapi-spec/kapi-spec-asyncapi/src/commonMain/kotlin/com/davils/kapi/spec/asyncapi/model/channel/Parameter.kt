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

package com.davils.kapi.spec.asyncapi.model.channel

import com.davils.kapi.spec.core.collection.frozen
import com.davils.kapi.spec.core.model.Element
import com.davils.kore.core.annotation.DavilsInternal

/**
 * A parameter of the address of a channel.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class Parameter internal constructor(builder: ParameterBuilder) : Element(builder) {
    /**
     * The values the parameter may take; empty when any value is allowed.
     *
     * @since 1.0.0
     */
    public val enumeration: List<String> = builder.enumeration.frozen()

    /**
     * The value used when none is supplied.
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
     * Examples of values of the parameter.
     *
     * @since 1.0.0
     */
    public val examples: List<String> = builder.examples.frozen()

    /**
     * The `location` of the parameter: the runtime expression that names where in a message the value is found, such as
     * `$message.payload#/user/id`.
     *
     * @since 1.0.0
     */
    public val expression: String? = builder.expression

    override val identity: List<Any?> get() = listOf(enumeration, default, description, examples, expression)
}
