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

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of [OAuthFlow]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class OAuthFlowBuilder internal constructor() : ElementBuilder<OAuthFlow>() {
    override val subject: String get() = "asyncapi oauth flow"

    /**
     * The address of the authorization endpoint, for implicit and authorization code.
     *
     * @since 1.0.0
     */
    public var authorizationUrl: String? = null

    /**
     * The address of the token endpoint, for every flow but the implicit one.
     *
     * @since 1.0.0
     */
    public var tokenUrl: String? = null

    /**
     * The address to refresh tokens at.
     *
     * @since 1.0.0
     */
    public var refreshUrl: String? = null

    internal val availableScopes: MutableMap<String, String> = linkedMapOf()

    override fun assemble(): OAuthFlow = OAuthFlow(this)
}
