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

package com.davils.kapi.spec.openapi.model.schema

/**
 * A schema written as `true` or `false`, which JSON Schema 2020-12 and so OpenAPI 3.1 and 3.2 allow wherever a
 * schema stands.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class FixedSchema internal constructor(builder: FixedSchemaBuilder) :
    Schema(builder) {
    /**
     * Whether the schema is `true`, which accepts every value; `false` accepts none.
     *
     * @since 1.0.0
     */
    public val isAccepting: Boolean = builder.isAccepting

    override val identity: List<Any?> get() = listOf(isAccepting)
}
