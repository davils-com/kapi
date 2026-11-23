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

/**
 * Decides whether the credential a request presents for one security scheme grants an operation; the service
 * provider interface of authorization, one per scheme of a server.
 *
 * The server reads the credential from where the scheme places it and asks the authorizer of the scheme; an operation
 * is granted by the first of its alternatives of OpenAPI 3.2 §4.26 whose every scheme grants. A scheme without an
 * authorizer denies. An authorizer is code of the application: it runs contained, and a failure denies the request.
 * It must be safe to call from any coroutine at once, and should compare secrets in constant time.
 *
 * @since 1.0.0
 */
public fun interface Authorizer {
    /**
     * Decides [attempt].
     *
     * @param attempt the scheme, the credential, the scopes the operation demands and the request.
     * @return [Authorization.Granted] with the caller, [Authorization.Denied] or [Authorization.Forbidden].
     * @since 1.0.0
     */
    public suspend fun authorize(attempt: AuthorizationAttempt): Authorization

    /**
     * The authorizers every server can use.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The authorizer that denies every credential.
         *
         * @since 1.0.0
         */
        public val DenyAll: Authorizer = Authorizer { Authorization.Unknown }
    }
}
