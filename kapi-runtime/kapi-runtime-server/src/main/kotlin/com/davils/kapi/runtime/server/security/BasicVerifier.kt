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
import com.davils.kore.core.principal.Principal
import com.davils.krypt.core.ConstantTime

/**
 * Checks the user and the password of `Authorization: Basic` of RFC 7617; the caller's store of passwords.
 *
 * A verifier should compare in constant time, such as through the password hasher of krypt, so its timing reveals
 * nothing of a stored password. It must be safe to call from any coroutine at once.
 *
 * @since 1.0.0
 */
public fun interface BasicVerifier {
    /**
     * Checks [user] and [password].
     *
     * @param user the user id, which may come from an attacker.
     * @param password the password.
     * @return the caller, or `null` if the user is unknown or the password is wrong.
     * @since 1.0.0
     */
    public suspend fun verify(user: String, password: Secret): Principal?

    /**
     * Makes verifiers.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns a verifier of one user with one password, such as a technical account, which compares both in
         * constant time with `ConstantTime` of krypt; the time reveals at most the lengths.
         *
         * @param user the user id.
         * @param password the password.
         * @param principal the caller the user is.
         * @return the verifier.
         * @since 1.0.0
         */
        public fun of(user: String, password: Secret, principal: Principal): BasicVerifier {
            val expectedUser = user.encodeToByteArray()
            val expectedPassword = password.reveal().encodeToByteArray()
            return BasicVerifier { presentedUser, presentedPassword ->
                val isUser = ConstantTime.equals(first = expectedUser, second = presentedUser.encodeToByteArray())
                val presented = presentedPassword.reveal().encodeToByteArray()
                val isPassword = ConstantTime.equals(first = expectedPassword, second = presented)
                principal.takeIf { isUser and isPassword }
            }
        }
    }
}
