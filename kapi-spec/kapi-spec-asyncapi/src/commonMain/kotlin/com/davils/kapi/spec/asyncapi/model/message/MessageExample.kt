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
import com.davils.kapi.spec.core.value.Value

/**
 * An example of a message, with its headers, its payload or both.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class MessageExample internal constructor(builder: MessageExampleBuilder) : Element(builder) {
    /**
     * The headers of the example, by name.
     *
     * @since 1.0.0
     */
    public val headers: Map<String, Value> = builder.headers.toMap()

    /**
     * The payload of the example.
     *
     * @since 1.0.0
     */
    public val payload: Value? = builder.payload

    /**
     * A machine-friendly name of the example.
     *
     * @since 1.0.0
     */
    public val name: String? = builder.name

    /**
     * A short summary of what it is about.
     *
     * @since 1.0.0
     */
    public val summary: String? = builder.summary

    override val identity: List<Any?> get() = listOf(headers, payload, name, summary)
}
