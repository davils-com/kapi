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

package com.davils.kapi.runtime.core.destination

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures a [DestinationPolicy]; by default it allows `https` to every public host and resolves nothing.
 *
 * A builder is not thread-safe; the policy it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class DestinationPolicyBuilder internal constructor() : ValidatingBuilder<DestinationPolicy>() {
    override val subject: String get() = "destination policy"

    /**
     * The schemes a destination may have, in lower case.
     *
     * @since 1.0.0
     */
    public var schemes: Set<String> = setOf("https")
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The hosts a destination may have, in lower case, such as `api.example.com`, `*.example.com` for every name
     * below `example.com`, `10.0.0.5` or `::1`; empty for every public host. A listed host is trusted as it is, even
     * in a private network.
     *
     * @since 1.0.0
     */
    public var hosts: Set<String> = emptySet()
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether a destination in a private network is allowed when no hosts are listed; `false` by default.
     *
     * @since 1.0.0
     */
    public var arePrivateNetworksAllowed: Boolean = false
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The resolver that finds the addresses of a public host name; [HostResolver.None] by default.
     *
     * @since 1.0.0
     */
    public var resolver: HostResolver = HostResolver.None
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): DestinationPolicy = DestinationPolicy(this)
}

/**
 * Builds the policy that every URL a client calls, follows or delivers to must pass.
 *
 * @param configure changes the policy from its defaults.
 * @return the policy.
 * @throws com.davils.kore.core.validation.report.ValidationException if a scheme or a host is malformed or no scheme
 * is named.
 * @since 1.0.0
 */
public fun destinationPolicy(configure: DestinationPolicyBuilder.() -> Unit = {}): DestinationPolicy {
    val builder = DestinationPolicyBuilder()
    builder.configure()
    return builder.build()
}
