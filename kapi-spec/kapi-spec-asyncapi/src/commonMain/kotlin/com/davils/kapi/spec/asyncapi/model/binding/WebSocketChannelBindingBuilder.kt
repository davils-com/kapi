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

package com.davils.kapi.spec.asyncapi.model.binding

import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of [WebSocketChannelBinding]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class WebSocketChannelBindingBuilder internal constructor() : ElementBuilder<WebSocketChannelBinding>() {
    override val subject: String get() = "asyncapi websocket channel binding"

    /**
     * The HTTP method of the request that opens the connection.
     *
     * @since 1.0.0
     */
    public var method: WebSocketMethod? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The schema of the query parameters, which describes an object.
     *
     * @since 1.0.0
     */
    public var query: Referable<Schema>? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The schema of the headers, which describes an object.
     *
     * @since 1.0.0
     */
    public var headers: Referable<Schema>? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The version of the binding, such as `0.1.0`.
     *
     * @since 1.0.0
     */
    public var bindingVersion: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): WebSocketChannelBinding = WebSocketChannelBinding(this)
}
