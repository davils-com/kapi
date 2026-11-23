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

package com.davils.kapi.runtime.server.request

import com.davils.kapi.runtime.core.destination.IpAddress
import com.davils.krypt.x509.Certificate

private const val NONE: String = "none"

/**
 * What a server knows of the peer of a request: its address and what the transport verified of it.
 *
 * The address is the one of the connection, never one of a forwarding header, which a client could forge. The
 * certificates and the identity come only from the transport through
 * [PeerAttributes][com.davils.kapi.runtime.server.PeerAttributes]; a peer is immutable.
 *
 * @property address the address of the connection, or `null` if it is no IP address, such as a Unix domain socket.
 * @param certificates the verified client certificate chain, the leaf first; empty without mutual TLS.
 * @property identity the identity the transport verified, or `null`.
 * @since 1.0.0
 */
public class Peer(
    public val address: IpAddress? = null,
    certificates: List<Certificate> = emptyList(),
    public val identity: PeerIdentity? = null,
) {
    /**
     * The verified client certificate chain, the leaf first; empty without mutual TLS.
     *
     * @since 1.0.0
     */
    public val certificates: List<Certificate> = certificates.toList()

    private val parts: List<Any?> get() = listOf(address, certificates, identity)

    override fun equals(other: Any?): Boolean = other is Peer && parts == other.parts

    override fun hashCode(): Int = parts.hashCode()

    override fun toString(): String =
        "Peer(address=${address ?: NONE}, certificates=${certificates.size}, identity=${identity ?: NONE})"
}
