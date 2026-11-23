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

package com.davils.kapi.runtime.core.security

/**
 * Hands out the current credential of a security scheme, such as a bearer token from a vault or an API key from the
 * configuration of the application.
 *
 * A source is called for every request that needs its scheme, from the coroutine of the call, and must be safe to
 * call concurrently.
 *
 * @since 1.0.0
 */
public fun interface TokenSource {
    /**
     * Returns the current credential.
     *
     * @return the credential, or `null` if there is none at the moment.
     * @since 1.0.0
     */
    public suspend fun token(): Secret?
}
