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

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kapi.spec.openapi.model.parameter.ParameterStyle

public class Encoding internal constructor(builder: EncodingBuilder) :
    Element(builder) {
    public val contentType: String? = builder.contentType

    public val headers: Map<String, Referable<Header>> = builder.headers.toMap()

    public val style: ParameterStyle? = builder.style

    public val isExploded: Boolean? = builder.isExploded

    public val isReservedAllowed: Boolean = builder.isReservedAllowed

    override val identity: List<Any?> get() = listOf(contentType, headers, style, isExploded, isReservedAllowed)
}
