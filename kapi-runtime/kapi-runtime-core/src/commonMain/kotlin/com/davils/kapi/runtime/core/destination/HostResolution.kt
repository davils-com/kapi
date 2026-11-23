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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.core.destination

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * What a [HostResolver] found for a host name.
 *
 * @since 1.0.0
 */
public sealed interface HostResolution {
    /**
     * The resolver does not resolve names; the name is allowed by its form alone.
     *
     * @since 1.0.0
     */
    public object Skipped : HostResolution {
        override fun toString(): String = "Skipped"
    }

    /**
     * The name resolves to [addresses].
     *
     * @param addresses the addresses.
     * @since 1.0.0
     */
    public class Resolved(addresses: List<IpAddress>) : HostResolution {
        /**
         * The addresses.
         *
         * @since 1.0.0
         */
        public val addresses: List<IpAddress> = addresses.toList()

        override fun equals(other: Any?): Boolean = other is Resolved && addresses == other.addresses

        override fun hashCode(): Int = addresses.hashCode()

        override fun toString(): String = "Resolved($addresses)"
    }

    /**
     * The name cannot be resolved.
     *
     * @property reason why, for a person to read.
     * @since 1.0.0
     */
    public class Failed(public val reason: String) : HostResolution {
        override fun equals(other: Any?): Boolean = other is Failed && reason == other.reason

        override fun hashCode(): Int = reason.hashCode()

        override fun toString(): String = "Failed(${SafeText.text(reason)})"
    }
}
