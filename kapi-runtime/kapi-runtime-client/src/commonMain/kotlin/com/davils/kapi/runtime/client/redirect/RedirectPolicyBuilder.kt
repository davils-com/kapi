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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures a [RedirectPolicy]; by default it follows five hops, to other origins as well, without credentials.
 *
 * A builder is not thread-safe; the policy it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class RedirectPolicyBuilder internal constructor() : ValidatingBuilder<RedirectPolicy>() {
    override val subject: String get() = "redirect policy"

    /**
     * The most hops a call follows, from 1 to [RedirectPolicy.HARD_HOPS]; 5 by default.
     *
     * @since 1.0.0
     */
    public var maxHops: Int = DEFAULT_HOPS

    /**
     * Whether a hop to another origin is followed, without the credentials and cookies of the call; `true` by
     * default.
     *
     * @since 1.0.0
     */
    public var isCrossOriginAllowed: Boolean = true

    override fun assemble(): RedirectPolicy = RedirectPolicy(this)

    private companion object {
        const val DEFAULT_HOPS: Int = 5
    }
}

/**
 * Builds the policy of the redirects a client follows itself.
 *
 * @param configure changes the policy from its defaults.
 * @return the policy.
 * @throws com.davils.kore.core.validation.report.ValidationException if [RedirectPolicyBuilder.maxHops] is outside
 * of 1 to [RedirectPolicy.HARD_HOPS].
 * @since 1.0.0
 */
public fun redirectPolicy(configure: RedirectPolicyBuilder.() -> Unit = {}): RedirectPolicy {
    val builder = RedirectPolicyBuilder()
    builder.configure()
    return builder.build()
}
