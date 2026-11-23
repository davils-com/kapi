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

/**
 * Resolves a host name to its addresses, so the [DestinationPolicy] can refuse a public name that leads into a
 * private network.
 *
 * kapi opens no connection of its own: the application implements the resolver, such as with the resolver of its
 * platform. The policy calls it from the coroutine of the call, contains whatever it throws as a failed resolution and
 * must be safe to call concurrently.
 *
 * @since 1.0.0
 */
public fun interface HostResolver {
    /**
     * Resolves [host].
     *
     * @param host the host name in lower case, without a trailing dot.
     * @return what the resolver found.
     * @since 1.0.0
     */
    public suspend fun resolve(host: String): HostResolution

    /**
     * The resolvers kapi brings along.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Resolves nothing and allows a name by its form alone.
         *
         * @since 1.0.0
         */
        public val None: HostResolver = HostResolver { HostResolution.Skipped }
    }
}
