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

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of [MessageExample]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class MessageExampleBuilder internal constructor() : ElementBuilder<MessageExample>() {
    override val subject: String get() = "asyncapi message example"

    internal val headers: MutableMap<String, Value> = linkedMapOf()

    /**
     * The payload of the example.
     *
     * @since 1.0.0
     */
    public var payload: Value? = null

    /**
     * A machine-friendly name of the example.
     *
     * @since 1.0.0
     */
    public var name: String? = null

    /**
     * A short summary of what it is about.
     *
     * @since 1.0.0
     */
    public var summary: String? = null

    override fun assemble(): MessageExample = MessageExample(this)
}
