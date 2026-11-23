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

package com.davils.kapi.runtime.server

import com.davils.kapi.runtime.server.request.PeerIdentity
import com.davils.krypt.x509.Certificate
import io.ktor.util.AttributeKey

/**
 * The attributes of a Ktor call through which the transport of the application hands over what it verified of the
 * peer, such as a TLS termination with mutual TLS.
 *
 * Only the transport sets them, such as a plugin of the application that reads the verified chain of the TLS session;
 * nothing a request carries can. The server reads them into the [Peer][com.davils.kapi.runtime.server.request.Peer]
 * of every request.
 *
 * @since 1.0.0
 */
public object PeerAttributes {
    /**
     * The client certificate chain the transport verified, the leaf first.
     *
     * @since 1.0.0
     */
    public val certificates: AttributeKey<List<Certificate>> = AttributeKey("kapi.peer.certificates")

    /**
     * The identity of the peer the transport verified by other means, such as the credentials of a Unix domain
     * socket.
     *
     * @since 1.0.0
     */
    public val identity: AttributeKey<PeerIdentity> = AttributeKey("kapi.peer.identity")
}
