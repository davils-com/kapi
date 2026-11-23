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

package com.davils.kapi.runtime.testing.oauth

/**
 * An endpoint of a [FakeAuthorizationServer].
 *
 * @since 1.0.0
 */
public enum class Endpoint {
    /**
     * The token endpoint of RFC 6749.
     *
     * @since 1.0.0
     */
    Token,

    /**
     * The device authorization endpoint of RFC 8628.
     *
     * @since 1.0.0
     */
    DeviceAuthorization,

    /**
     * The revocation endpoint of RFC 7009.
     *
     * @since 1.0.0
     */
    Revocation,

    /**
     * The metadata of RFC 8414 or of OpenID Connect Discovery.
     *
     * @since 1.0.0
     */
    Metadata,

    /**
     * A URL the server does not serve, answered with `404`.
     *
     * @since 1.0.0
     */
    Unknown,
}
