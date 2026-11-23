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

private const val HASH_FACTOR: Int = 31

private const val HTTP_PORT: Int = 80

private const val HTTPS_PORT: Int = 443

private val defaultPorts: Map<String, Int> =
    mapOf("http" to HTTP_PORT, "ws" to HTTP_PORT, "https" to HTTPS_PORT, "wss" to HTTPS_PORT)

/**
 * Where a client is about to send a request: the scheme, the host and the port of a URL.
 *
 * Two destinations share an origin by RFC 6454 when their schemes, hosts and effective ports are equal, without case
 * for the scheme and the host; [isSameOrigin] decides whether credentials of one may follow a redirect to the other.
 * A destination is immutable. [toString] shows its parts escaped.
 *
 * @property scheme the scheme, such as `https`.
 * @property host the host as it was written, a name, an IPv4 address or an IPv6 address in brackets.
 * @property port the port, or `null` for the default port of the scheme.
 * @since 1.0.0
 */
public class Destination(public val scheme: String, public val host: String, public val port: Int? = null) {
    /**
     * The port the connection uses: [port], or the default port of `http`, `https`, `ws` and `wss`, or `null` for
     * another scheme without a port.
     *
     * @since 1.0.0
     */
    public val effectivePort: Int? get() = port ?: defaultPorts[scheme.lowercase()]

    /**
     * Returns whether [other] belongs to the same origin.
     *
     * @param other the other destination.
     * @return `true` if the schemes and the hosts are equal without case and the effective ports are equal.
     * @since 1.0.0
     */
    public fun isSameOrigin(other: Destination): Boolean {
        val isSameScheme = scheme.equals(other.scheme, ignoreCase = true)
        val isSameHost = host.removeSuffix(".").equals(other.host.removeSuffix("."), ignoreCase = true)
        return isSameScheme && isSameHost && effectivePort == other.effectivePort
    }

    override fun equals(other: Any?): Boolean =
        other is Destination && scheme == other.scheme && host == other.host && port == other.port

    override fun hashCode(): Int = (scheme.hashCode() * HASH_FACTOR + host.hashCode()) * HASH_FACTOR + (port ?: 0)

    override fun toString(): String {
        val shownPort = port?.let { number -> ":$number" }.orEmpty()
        return "Destination(${SafeText.text(scheme)}://${SafeText.text(host)}$shownPort)"
    }
}
