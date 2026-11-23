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
 * What the [DestinationPolicy] decided about a destination.
 *
 * @since 1.0.0
 */
public sealed interface DestinationDecision {
    /**
     * The destination may be called.
     *
     * A client that connects to [addresses] rather than resolving the host again closes the window in which a name
     * could resolve to another address than the one checked.
     *
     * @param addresses the addresses that were checked: the address of the host, the addresses the resolver found,
     * or none when the host was not resolved.
     * @since 1.0.0
     */
    public class Allowed(addresses: List<IpAddress> = emptyList()) : DestinationDecision {
        /**
         * The addresses that were checked.
         *
         * @since 1.0.0
         */
        public val addresses: List<IpAddress> = addresses.toList()

        override fun equals(other: Any?): Boolean = other is Allowed && addresses == other.addresses

        override fun hashCode(): Int = addresses.hashCode()

        override fun toString(): String = "Allowed($addresses)"
    }

    /**
     * The destination must not be called.
     *
     * @property refusal why.
     * @since 1.0.0
     */
    public class Refused(public val refusal: DestinationRefusal) : DestinationDecision {
        override fun equals(other: Any?): Boolean = other is Refused && refusal == other.refusal

        override fun hashCode(): Int = refusal.hashCode()

        override fun toString(): String = "Refused($refusal)"
    }
}
