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

package com.davils.kapi.spec.openapi.model.parameter

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.content.Example
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of a [Parameter], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the parameter it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ParameterBuilder internal constructor() : ElementBuilder<Parameter>() {
    override val subject: String get() = "openapi parameter"

    /**
     * The value of `name`, the name of the parameter; empty until it is set.
     *
     * @since 1.0.0
     */
    public var name: String = ""

    /**
     * The value of `in`, where the parameter is sent; [ParameterPlacement.Query] until it is set.
     *
     * @since 1.0.0
     */
    public var placement: ParameterPlacement = ParameterPlacement.Query

    /**
     * The value of `description`, the description of the parameter.
     *
     * @since 1.0.0
     */
    public var description: String? = null

    /**
     * The value of `required`, whether a request must carry the parameter.
     *
     * @since 1.0.0
     */
    public var isRequired: Boolean = false

    /**
     * The value of `deprecated`, whether the parameter should no longer be used.
     *
     * @since 1.0.0
     */
    public var isDeprecated: Boolean = false

    /**
     * The value of `allowEmptyValue`, whether a query parameter may be sent without a value.
     *
     * @since 1.0.0
     */
    public var isEmptyValueAllowed: Boolean = false

    /**
     * The value of `style`; `null` to take the default of the placement.
     *
     * @since 1.0.0
     */
    public var style: ParameterStyle? = null

    /**
     * The value of `explode`; `null` to take the default of the style.
     *
     * @since 1.0.0
     */
    public var isExploded: Boolean? = null

    /**
     * The value of `allowReserved`, whether reserved characters stay unencoded.
     *
     * @since 1.0.0
     */
    public var isReservedAllowed: Boolean = false

    /**
     * The value of `schema`, the schema of the value.
     *
     * @since 1.0.0
     */
    public var schema: Schema? = null

    internal val content: MutableMap<String, Referable<MediaType>> = linkedMapOf()

    /**
     * The value of `example`, an example of the value.
     *
     * @since 1.0.0
     */
    public var example: Value? = null

    internal val examples: MutableMap<String, Referable<Example>> = linkedMapOf()

    override fun assemble(): Parameter = Parameter(this)
}
