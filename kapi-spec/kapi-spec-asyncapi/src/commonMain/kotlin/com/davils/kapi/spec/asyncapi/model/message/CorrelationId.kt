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

package com.davils.kapi.spec.asyncapi.model.message

import com.davils.kapi.spec.core.model.Element

/**
 * Where the id is found that correlates a message with another one.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class CorrelationId internal constructor(builder: CorrelationIdBuilder) : Element(builder) {
    /**
     * A description, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The `location` of the id: the runtime expression that names where in the message the id is found, such as
     * `$message.header#/correlationId`.
     *
     * @since 1.0.0
     */
    public val expression: String = builder.expression

    override val identity: List<Any?> get() = listOf(description, expression)
}
