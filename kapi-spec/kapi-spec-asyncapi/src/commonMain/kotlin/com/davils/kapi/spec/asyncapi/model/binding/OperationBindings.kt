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

package com.davils.kapi.spec.asyncapi.model.binding

import com.davils.kapi.spec.core.collection.frozen
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The protocol-specific details of an operation, by the name of the protocol.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class OperationBindings internal constructor(builder: OperationBindingsBuilder) : Element(builder) {
    /**
     * Whether the operation declares the WebSocket binding, which has no fields.
     *
     * @since 1.0.0
     */
    public val hasWebSocket: Boolean = builder.hasWebSocket

    /**
     * The bindings of every protocol but WebSocket, by the name of the protocol, as they were written.
     *
     * @since 1.0.0
     */
    public val protocols: Map<String, Value> = builder.protocols.frozen()

    override val identity: List<Any?> get() = listOf(hasWebSocket, protocols)
}
