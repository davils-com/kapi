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

package com.davils.kapi.spec.openapi.model.path

import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.server.Server
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The builder of a [PathItem], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the path item it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class PathItemBuilder internal constructor() : ElementBuilder<PathItem>() {
    override val subject: String get() = "openapi path item"

    /**
     * The value of `$ref`, the path item this one refers to.
     *
     * @since 1.0.0
     */
    public var reference: Reference? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `summary`, the summary of the path item.
     *
     * @since 1.0.0
     */
    public var summary: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `description`, the description of the path item.
     *
     * @since 1.0.0
     */
    public var description: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    internal val operations: MutableMap<Method, Operation> = KeyedMap()

    internal val additionalOperations: MutableMap<String, Operation> = KeyedMap()

    internal val servers: MutableList<Server> = mutableListOf()

    internal val parameters: MutableList<Referable<Parameter>> = mutableListOf()

    override fun assemble(): PathItem = PathItem(this)
}
