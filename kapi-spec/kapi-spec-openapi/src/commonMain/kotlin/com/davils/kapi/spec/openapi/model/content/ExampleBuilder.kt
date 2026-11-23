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

package com.davils.kapi.spec.openapi.model.content

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of an [Example], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the example it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ExampleBuilder internal constructor() : ElementBuilder<Example>() {
    override val subject: String get() = "openapi example"

    /**
     * The value of `summary`, the summary of the example.
     *
     * @since 1.0.0
     */
    public var summary: String? = null

    /**
     * The value of `description`, the description of the example.
     *
     * @since 1.0.0
     */
    public var description: String? = null

    /**
     * The value of `value`, the example itself.
     *
     * @since 1.0.0
     */
    public var value: Value? = null

    /**
     * The value of `externalValue`, the URL of an example kept elsewhere.
     *
     * @since 1.0.0
     */
    public var externalValue: String? = null

    /**
     * The value of `dataValue`, the example as data that the schema validates, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var dataValue: Value? = null

    /**
     * The value of `serializedValue`, the example as it is serialized on the wire, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var serializedValue: String? = null

    override fun assemble(): Example = Example(this)
}
