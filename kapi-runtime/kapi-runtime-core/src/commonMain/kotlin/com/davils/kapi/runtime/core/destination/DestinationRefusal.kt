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
 * Why the [DestinationPolicy] refused a destination.
 *
 * @since 1.0.0
 */
public enum class DestinationRefusal {
    /**
     * The scheme is none the policy allows.
     *
     * @since 1.0.0
     */
    Scheme,

    /**
     * The host is no host name or address the policy can read, such as one with a space, a non-ASCII character or a
     * numeric form other than a dotted quad.
     *
     * @since 1.0.0
     */
    Host,

    /**
     * The policy names the hosts it allows, and this host is none of them.
     *
     * @since 1.0.0
     */
    HostNotAllowed,

    /**
     * The host is an address or a name of a network that is not public, such as `localhost`, `10.0.0.1` or `::1`.
     *
     * @since 1.0.0
     */
    NotPublic,

    /**
     * The resolver of the policy found no address for the host or failed.
     *
     * @since 1.0.0
     */
    Unresolved,

    /**
     * The host resolves to an address that is not public.
     *
     * @since 1.0.0
     */
    ResolvesToPrivate,

    /**
     * The text is no absolute URL with a host, or its port is no number from 0 to 65535.
     *
     * @since 1.0.0
     */
    Url,

    /**
     * The URL carries user information before its host, such as `https://admin@example.com`, which hides where it
     * leads and would send credentials in the clear.
     *
     * @since 1.0.0
     */
    UserInfo,
}
