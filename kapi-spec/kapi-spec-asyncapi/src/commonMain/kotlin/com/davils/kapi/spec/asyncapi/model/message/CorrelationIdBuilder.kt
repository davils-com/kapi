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
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of [CorrelationId]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class CorrelationIdBuilder internal constructor() : ElementBuilder<CorrelationId>() {
    override val subject: String get() = "asyncapi correlation id"

    /**
     * A description, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public var description: String? = null

    /**
     * The `location` of the id: the runtime expression that names where in the message the id is found, such as
     * `$message.header#/correlationId`.
     *
     * @since 1.0.0
     */
    public var expression: String = ""

    override fun assemble(): CorrelationId = CorrelationId(this)
}
