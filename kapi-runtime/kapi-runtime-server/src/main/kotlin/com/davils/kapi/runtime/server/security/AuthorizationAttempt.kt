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

import com.davils.kapi.runtime.server.request.Request
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * What an [Authorizer] decides on: a credential presented for a scheme, the scopes the operation demands and the
 * request.
 *
 * @property scheme the name of the security scheme.
 * @property credential the credential the request presents.
 * @param scopes the scopes the operation demands for the scheme.
 * @property request the request, whose body has not been read.
 * @since 1.0.0
 */
public class AuthorizationAttempt(
    public val scheme: String,
    public val credential: PresentedCredential,
    scopes: Set<String>,
    public val request: Request,
) {
    /**
     * The scopes the operation demands for the scheme.
     *
     * @since 1.0.0
     */
    public val scopes: Set<String> = scopes.toSet()

    @OptIn(DavilsInternal::class)
    override fun toString(): String =
        "AuthorizationAttempt(${SafeText.text(scheme)}, $credential, ${scopes.map(SafeText::text)})"
}
