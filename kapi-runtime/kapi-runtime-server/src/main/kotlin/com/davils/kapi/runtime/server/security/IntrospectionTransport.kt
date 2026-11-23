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

package com.davils.kapi.runtime.server.security

import com.davils.kapi.runtime.core.security.Secret

/**
 * Asks the introspection endpoint of an authorization server about a token, by RFC 7662 §2.1; the caller's
 * transport, such as a Ktor client with the credentials of the resource server.
 *
 * kapi opens no connection itself. The transport posts the token as `token` of a form to the endpoint, authenticated
 * as the resource server, and returns the body of the answer. It runs contained; a failure denies the token.
 *
 * @since 1.0.0
 */
public fun interface IntrospectionTransport {
    /**
     * Introspects [token].
     *
     * @param token the token the request presents.
     * @return the JSON body of the answer of RFC 7662 §2.2, or `null` if the endpoint could not answer.
     * @since 1.0.0
     */
    public suspend fun introspect(token: Secret): String?
}
