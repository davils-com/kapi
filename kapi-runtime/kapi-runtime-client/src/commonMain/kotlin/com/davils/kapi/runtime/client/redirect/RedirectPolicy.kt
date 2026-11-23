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

package com.davils.kapi.runtime.client.redirect

import com.davils.kapi.runtime.client.ClientViolationCodes
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The redirects a client follows itself, by RFC 9110 §15.4.
 *
 * A client with a policy follows `301`, `302`, `303`, `307` and `308` with a `Location` field. `303` turns every
 * method but `HEAD` into `GET` without content, as do `301` and `302` for `POST`; `307` and `308` keep the method and
 * the content, and are not followed for a call whose content is streamed and cannot be sent again. Every hop passes
 * the destination policy of the client. The credentials, cookies and DPoP proofs of a call go only to its own origin
 * of RFC 6454: a hop to another origin goes without them, or is not followed when [isCrossOriginAllowed] is `false`.
 * A call that is redirected more than [maxHops] times fails with
 * [com.davils.kapi.runtime.client.call.CallFailure.TooManyRedirects]. A policy is immutable and safe to share.
 *
 * @since 1.0.0
 */
public class RedirectPolicy internal constructor(builder: RedirectPolicyBuilder) : Validatable {
    /**
     * The most hops a call follows.
     *
     * @since 1.0.0
     */
    public val maxHops: Int = builder.maxHops

    /**
     * Whether a hop to another origin is followed, without credentials.
     *
     * @since 1.0.0
     */
    public val isCrossOriginAllowed: Boolean = builder.isCrossOriginAllowed

    override fun ValidationScope.validate() {
        if (maxHops !in 1..HARD_HOPS) {
            fail(::maxHops, "must be from 1 to $HARD_HOPS", ClientViolationCodes.LIMIT_INVALID)
        }
    }

    override fun toString(): String = "RedirectPolicy(maxHops=$maxHops, isCrossOriginAllowed=$isCrossOriginAllowed)"

    /**
     * The hard ceiling of a redirect policy.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The most hops any client follows.
         *
         * @since 1.0.0
         */
        public const val HARD_HOPS: Int = 20
    }
}
