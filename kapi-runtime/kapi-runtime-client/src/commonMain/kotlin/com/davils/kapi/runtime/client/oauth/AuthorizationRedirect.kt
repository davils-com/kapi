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
 * The step of the authorization code grant only the application can take: open [AuthorizationRequest.url] in the
 * browser of the user and hand back what the authorization server redirected to.
 *
 * The redirect is foreign code: the token source runs it inside `attempted` of Kore, and a failure or `null` leaves it
 * without a token. It runs in the coroutine that asks for a token, which waits until the user is done.
 *
 * @since 1.0.0
 */
public fun interface AuthorizationRedirect {
    /**
     * Lets the user authorize the client.
     *
     * @param request where to send the user.
     * @return what came back through the redirect URI, or `null` when the user did not authorize the client.
     * @since 1.0.0
     */
    public suspend fun authorize(request: AuthorizationRequest): AuthorizationResponse?
}
