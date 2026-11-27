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

package com.davils.kapi.runtime.server.ipc

import com.davils.kapi.runtime.server.PeerAttributes
import com.davils.konnect.ipc.server.http.IpcHttpCallAttributes
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.createApplicationPlugin

private const val PLUGIN_NAME: String = "KapiIpcPeers"

/**
 * The Ktor plugin that hands the peer of a local connection of Konnect to the kapi server: for every call served by
 * the HTTP engine of `konnect-ipc-server`, it puts the identity of the process on the other side of the socket or pipe
 * into [PeerAttributes.identity], so an authorizer of kapi can trust `PresentedCredential.Peer`.
 *
 * Install it with `install(IpcPeers)` in the application of an `IpcHttpServer`. A call that did not come in over a
 * local connection, or whose peer has no user, is left as it is. The plugin never trusts a header or anything the
 * peer sent; the identity comes from the operating system through the IPC server, which checked the peer against its
 * access before it accepted the connection.
 *
 * @since 1.1.0
 */
public val IpcPeers: ApplicationPlugin<Unit> = createApplicationPlugin(PLUGIN_NAME) {
    onCall { call ->
        val identity = call.attributes.getOrNull(IpcHttpCallAttributes.peer)?.toPeerIdentity()
        identity?.let { found -> call.attributes.put(PeerAttributes.identity, found) }
    }
}
