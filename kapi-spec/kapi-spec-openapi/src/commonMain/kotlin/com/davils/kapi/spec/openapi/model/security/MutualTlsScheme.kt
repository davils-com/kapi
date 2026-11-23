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

package com.davils.kapi.spec.openapi.model.security

/**
 * A security scheme of `type` `mutualTLS`: the client proves who it is by a certificate in TLS. OpenAPI defines it
 * from 3.1 on, and a reading of an older document reports it and leaves it out.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class MutualTlsScheme internal constructor(builder: MutualTlsSchemeBuilder) :
    SecurityScheme(builder) {
    public override val description: String? = builder.description

    /**
     * Whether the scheme is `deprecated` and should no longer be used; `false` when the document leaves it out and
     * before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public override val isDeprecated: Boolean = builder.isDeprecated

    override val identity: List<Any?> get() = listOf(description, isDeprecated)
}
