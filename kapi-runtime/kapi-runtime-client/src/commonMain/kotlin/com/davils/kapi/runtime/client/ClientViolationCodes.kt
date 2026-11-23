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

package com.davils.kapi.runtime.client

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations the settings of a client, a redirect policy and a channel report.
 *
 * @since 1.0.0
 */
public object ClientViolationCodes {
    /**
     * The name of the client is no lower case words joined by a dot, a dash or an underscore.
     *
     * @since 1.0.0
     */
    public val NAME_INVALID: ViolationCode = ViolationCode("kapi.client.name-invalid")

    /**
     * The base URL is neither empty nor an `http` or `https` URL without user information, query or fragment.
     *
     * @since 1.0.0
     */
    public val BASE_URL_INVALID: ViolationCode = ViolationCode("kapi.client.base-url-invalid")

    /**
     * A limit is not above zero or above its hard ceiling.
     *
     * @since 1.0.0
     */
    public val LIMIT_INVALID: ViolationCode = ViolationCode("kapi.client.limit-invalid")

    /**
     * A part of a timeout is not above zero or not finite.
     *
     * @since 1.0.0
     */
    public val TIMEOUT_INVALID: ViolationCode = ViolationCode("kapi.client.timeout-invalid")

    /**
     * The `HttpClient` follows redirects itself, which would hand credentials to wherever a server points.
     *
     * @since 1.0.0
     */
    public val REDIRECTS_FOLLOWED: ViolationCode = ViolationCode("kapi.client.redirects-followed")

    /**
     * An interceptor has a blank name or a name another one already has.
     *
     * @since 1.0.0
     */
    public val INTERCEPTOR_INVALID: ViolationCode = ViolationCode("kapi.client.interceptor-invalid")

    /**
     * A DPoP key is bound to a scheme twice or to a blank scheme.
     *
     * @since 1.0.0
     */
    public val DPOP_INVALID: ViolationCode = ViolationCode("kapi.client.dpop-invalid")
}
