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
 * The builder of [OAuthFlows]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class OAuthFlowsBuilder internal constructor() : ElementBuilder<OAuthFlows>() {
    override val subject: String get() = "asyncapi oauth flows"

    /**
     * The implicit flow.
     *
     * @since 1.0.0
     */
    public var implicit: OAuthFlow? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The resource owner password flow.
     *
     * @since 1.0.0
     */
    public var password: OAuthFlow? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The client credentials flow.
     *
     * @since 1.0.0
     */
    public var clientCredentials: OAuthFlow? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The authorization code flow.
     *
     * @since 1.0.0
     */
    public var authorizationCode: OAuthFlow? = null
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): OAuthFlows = OAuthFlows(this)
}
