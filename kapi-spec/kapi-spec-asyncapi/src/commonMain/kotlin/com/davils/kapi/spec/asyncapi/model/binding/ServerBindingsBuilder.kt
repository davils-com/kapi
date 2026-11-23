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

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of [ServerBindings]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ServerBindingsBuilder internal constructor() : ElementBuilder<ServerBindings>() {
    override val subject: String get() = "asyncapi server bindings"

    /**
     * Whether the server declares the WebSocket binding, which has no fields.
     *
     * @since 1.0.0
     */
    public var hasWebSocket: Boolean = false

    internal val protocols: MutableMap<String, Value> = linkedMapOf()

    override fun assemble(): ServerBindings = ServerBindings(this)
}
