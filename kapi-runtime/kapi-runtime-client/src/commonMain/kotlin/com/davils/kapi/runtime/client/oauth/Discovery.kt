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

package com.davils.kapi.runtime.client.oauth

/**
 * Where a token source discovers the metadata of its issuer.
 *
 * @since 1.0.0
 */
public enum class Discovery {
    /**
     * The authorization server metadata of RFC 8414 at `/.well-known/oauth-authorization-server` inserted before the
     * path of the issuer.
     *
     * @since 1.0.0
     */
    OAuth,

    /**
     * The configuration of OpenID Connect Discovery 1.0 at `/.well-known/openid-configuration` after the issuer.
     *
     * @since 1.0.0
     */
    OpenIdConnect,
}
