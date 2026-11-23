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

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kore.core.annotation.DavilsDsl

@DavilsDsl
public class OAuthFlowBuilder internal constructor() : ElementBuilder<OAuthFlow>() {
    override val subject: String get() = "openapi oauth flow"

    public var authorizationUrl: String? = null

    /**
     * The device authorization endpoint of the device authorization flow, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var deviceAuthorizationUrl: String? = null

    public var tokenUrl: String? = null

    public var refreshUrl: String? = null

    internal val scopes: MutableMap<String, String> = linkedMapOf()

    override fun assemble(): OAuthFlow = OAuthFlow(this)
}
