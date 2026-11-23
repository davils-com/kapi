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
 * Authorizes an `http` `basic` scheme of RFC 7617 with the [BasicVerifier] of the caller.
 *
 * Only a user and a password are checked; every other kind of credential is denied. A missing or malformed
 * `Authorization: Basic` is answered with the challenge `Basic realm="…", charset="UTF-8"`.
 *
 * @param verifier checks the user and the password in constant time.
 * @since 1.0.0
 */
public class BasicAuthorizer(private val verifier: BasicVerifier) : Authorizer {
    override suspend fun authorize(attempt: AuthorizationAttempt): Authorization {
        val basic = attempt.credential as? PresentedCredential.Basic
        val principal = basic?.let { credential -> verifier.verify(credential.user, credential.password) }
        return principal?.let(Authorization::Granted) ?: Authorization.Unknown
    }

    override fun toString(): String = "BasicAuthorizer"
}
