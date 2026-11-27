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

package com.davils.kapi.runtime.server.tls

import com.davils.kapi.runtime.server.PeerAttributes
import com.davils.kore.core.failure.contained
import com.davils.krypt.tls.peer.PeerIdentity
import com.davils.krypt.x509.Certificate
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.createApplicationPlugin
import javax.net.ssl.SSLSession

private const val PLUGIN_NAME: String = "KapiTlsPeers"

private const val UNCONFIGURED: String =
    "TlsPeers needs to know the TLS session of a call; configure it with session { call -> ... }"

private fun verifiedPathOf(call: ApplicationCall, read: (ApplicationCall) -> SSLSession?): List<Certificate> {
    val identity = contained { read(call)?.let(PeerIdentity::of) }.getOrNull()
    return identity?.path.orEmpty()
}

/**
 * The Ktor plugin that hands the client certificate chain krypt verified for a mutual TLS connection to the kapi
 * server: for every call, it puts the path that the trust manager of `krypt-tls` validated, the certificate of the
 * client first and the trust anchor left out, into [PeerAttributes.certificates], so an authorizer of kapi can trust
 * `PresentedCredential.Certificate` and the certificates of the peer of a request.
 *
 * Install it with `install(TlsPeers) { session { call -> ... } }` in an application whose engine terminates TLS with a
 * `TlsContext` of krypt that asks for client certificates. Only an identity krypt recorded in the session reaches
 * the server: the certificates a client merely sent, a session without such an identity and a call that did not
 * come in over TLS leave the call without certificates, and certificates an earlier plugin put into the call are
 * removed, so no unverified chain is ever taken. The plugin never reads a header or anything else the peer sent.
 *
 * @throws IllegalStateException when it is installed without [TlsPeersConfiguration.session].
 * @since 1.2.0
 */
public val TlsPeers: ApplicationPlugin<TlsPeersConfiguration> =
    createApplicationPlugin(PLUGIN_NAME, ::TlsPeersConfiguration) {
        val read = checkNotNull(pluginConfig.reader) { UNCONFIGURED }
        onCall { call ->
            call.attributes.remove(PeerAttributes.certificates)
            val path = verifiedPathOf(call, read)
            if (path.isNotEmpty()) call.attributes.put(PeerAttributes.certificates, path)
        }
    }
