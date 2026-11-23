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

package com.davils.kapi.spec.openapi.model.server

import com.davils.kapi.spec.core.model.Element

/**
 * A server variable object: a value that replaces a variable in the address of a [Server].
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class ServerVariable internal constructor(builder: ServerVariableBuilder) :
    Element(builder) {
    /**
     * The `default` value of the variable, used when no other value is given; required by OpenAPI, so a reading reports
     * a variable that leaves it out and keeps it empty here. A reading also reports a default that is none of
     * [enumeration].
     *
     * @since 1.0.0
     */
    public val default: String = builder.default

    /**
     * The values of `enum` the variable is limited to, in the order of the document; empty when any value is allowed.
     *
     * @since 1.0.0
     */
    public val enumeration: List<String> = builder.enumeration.toList()

    /**
     * The `description` of the variable, which may use CommonMark; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    override val identity: List<Any?> get() = listOf(default, enumeration, description)
}
