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
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.schema.Schema

public class MediaType internal constructor(builder: MediaTypeBuilder) :
    Element(builder) {
    public val schema: Schema? = builder.schema

    public val example: Value? = builder.example

    public val examples: Map<String, Referable<Example>> = builder.examples.toMap()

    public val encoding: Map<String, Encoding> = builder.encoding.toMap()

    override val identity: List<Any?> get() = listOf(schema, example, examples, encoding)
}
