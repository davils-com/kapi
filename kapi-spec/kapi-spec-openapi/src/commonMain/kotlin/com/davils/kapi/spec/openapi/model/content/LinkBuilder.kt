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

package com.davils.kapi.spec.openapi.model.content

import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.server.Server
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The builder of a [Link], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the link it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class LinkBuilder internal constructor() : ElementBuilder<Link>() {
    override val subject: String get() = "openapi link"

    /**
     * The value of `operationRef`, a URI reference to the operation.
     *
     * @since 1.0.0
     */
    public var operationReference: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `operationId`, the id of the operation.
     *
     * @since 1.0.0
     */
    public var operationId: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    internal val parameters: MutableMap<String, Value> = KeyedMap()

    /**
     * The value of `requestBody`, the body to send to the operation.
     *
     * @since 1.0.0
     */
    public var requestBody: Value? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `description`, the description of the link.
     *
     * @since 1.0.0
     */
    public var description: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `server`, the server the operation is called on.
     *
     * @since 1.0.0
     */
    public var server: Server? = null
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): Link = Link(this)
}
