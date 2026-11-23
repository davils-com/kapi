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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations the settings of a token source report.
 *
 * @since 1.0.0
 */
public object OAuthViolationCodes {
    /**
     * An endpoint or the issuer is not `https`, nor `http` on this machine, or carries a query or a fragment, or the
     * token endpoint is neither set nor discoverable.
     *
     * @since 1.0.0
     */
    public val URL_INVALID: ViolationCode = ViolationCode("kapi.client.oauth.url-invalid")

    /**
     * The client id is blank or carries a control character.
     *
     * @since 1.0.0
     */
    public val CLIENT_ID_INVALID: ViolationCode = ViolationCode("kapi.client.oauth.client-id-invalid")

    /**
     * The client authentication does not fit the grant or its algorithm, such as a public client asking for client
     * credentials or a secret too short for its MAC.
     *
     * @since 1.0.0
     */
    public val AUTHENTICATION_INVALID: ViolationCode = ViolationCode("kapi.client.oauth.authentication-invalid")

    /**
     * The grant cannot be used as configured, such as an authorization code grant without an authorization endpoint
     * or a redirect URI that is no absolute URI.
     *
     * @since 1.0.0
     */
    public val GRANT_INVALID: ViolationCode = ViolationCode("kapi.client.oauth.grant-invalid")

    /**
     * A scope is empty or holds a character outside of RFC 6749 §3.3.
     *
     * @since 1.0.0
     */
    public val SCOPE_INVALID: ViolationCode = ViolationCode("kapi.client.oauth.scope-invalid")

    /**
     * A time is negative, zero where it must not be, or infinite.
     *
     * @since 1.0.0
     */
    public val TIME_INVALID: ViolationCode = ViolationCode("kapi.client.oauth.time-invalid")

    /**
     * The `HttpClient` follows redirects itself, which would hand the client secret to wherever a server points.
     *
     * @since 1.0.0
     */
    public val REDIRECTS_FOLLOWED: ViolationCode = ViolationCode("kapi.client.oauth.redirects-followed")
}
