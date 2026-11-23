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

package com.davils.kapi.spec.asyncapi.model.security

import com.davils.kapi.spec.core.collection.frozen
import com.davils.kore.core.annotation.DavilsInternal

/**
 * A security scheme of type `openIdConnect`: OpenID Connect discovered from its configuration.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class OpenIdConnectScheme internal constructor(builder: OpenIdConnectSchemeBuilder) : SecurityScheme(builder) {
    override val type: SecuritySchemeType get() = SecuritySchemeType.OpenIdConnect

    /**
     * A description of the scheme, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public override val description: String? = builder.description

    /**
     * The address of the OpenID Connect configuration; it is not checked when it is read, and a client fetches it only
     * through its destination policy.
     *
     * @since 1.0.0
     */
    public val openIdConnectUrl: String = builder.openIdConnectUrl

    /**
     * The scopes a client needs.
     *
     * @since 1.0.0
     */
    public val scopes: List<String> = builder.scopes.frozen()

    override val identity: List<Any?> get() = listOf(description, openIdConnectUrl, scopes)
}
