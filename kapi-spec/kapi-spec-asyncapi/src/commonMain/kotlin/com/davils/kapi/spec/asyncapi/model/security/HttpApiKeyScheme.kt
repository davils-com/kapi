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

package com.davils.kapi.spec.asyncapi.model.security

/**
 * A security scheme of type `httpApiKey`: a key sent in a query parameter, a header or a cookie.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class HttpApiKeyScheme internal constructor(builder: HttpApiKeySchemeBuilder) : SecurityScheme(builder) {
    override val type: SecuritySchemeType get() = SecuritySchemeType.HttpApiKey

    /**
     * A description of the scheme, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public override val description: String? = builder.description

    /**
     * The name of the query parameter, the header or the cookie.
     *
     * @since 1.0.0
     */
    public val name: String = builder.name

    /**
     * Where the key is sent.
     *
     * @since 1.0.0
     */
    public val placement: HttpApiKeyPlacement = builder.placement

    override val identity: List<Any?> get() = listOf(description, name, placement)
}
