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

package com.davils.kapi.spec.openapi.model.security

import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.collection.frozen
import com.davils.kapi.spec.core.model.Element
import com.davils.kore.core.annotation.DavilsInternal

private fun frozenLists(source: Map<String, List<String>>): Map<String, List<String>> {
    val copy = KeyedMap<String, List<String>>()
    source.forEach { (key, value) -> copy[key] = value.frozen() }
    return copy.frozen()
}

/**
 * A security requirement object: the security schemes a request must satisfy together. Of the requirements of a
 * document or an operation, a request meets one.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class SecurityRequirement internal constructor(builder: SecurityRequirementBuilder) :
    Element(builder) {
    /**
     * The names of the security schemes of the components, each with the scopes or roles it needs, in the order of the
     * document; empty for the requirement `{}`, which lets a request through without security.
     *
     * A reading reports a name that is no security scheme of the components and, before OpenAPI 3.1, scopes for a
     * scheme other than `oauth2` and `openIdConnect`.
     *
     * @since 1.0.0
     */
    public val schemes: Map<String, List<String>> = frozenLists(builder.schemes)

    override val identity: List<Any?> get() = listOf(schemes)
}
