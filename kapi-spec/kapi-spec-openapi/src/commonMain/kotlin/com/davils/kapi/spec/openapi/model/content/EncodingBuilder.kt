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
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kapi.spec.openapi.model.parameter.ParameterStyle
import com.davils.kore.core.annotation.DavilsDsl

@DavilsDsl
public class EncodingBuilder internal constructor() : ElementBuilder<Encoding>() {
    override val subject: String get() = "openapi encoding"

    public var contentType: String? = null

    internal val headers: MutableMap<String, Referable<Header>> = linkedMapOf()

    public var style: ParameterStyle? = null

    public var isExploded: Boolean? = null

    public var isReservedAllowed: Boolean = false

    override fun assemble(): Encoding = Encoding(this)
}
