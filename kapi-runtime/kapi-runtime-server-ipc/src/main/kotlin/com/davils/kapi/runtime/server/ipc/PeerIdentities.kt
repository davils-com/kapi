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

import com.davils.kapi.runtime.server.request.PeerIdentity
import com.davils.konnect.ipc.core.access.IpcPeer

/**
 * Returns the identity of this peer for the kapi server: the name of its Unix user or its Windows security identifier
 * as the user, and its process id, both as the operating system reported them.
 *
 * @receiver the peer a local server of Konnect checked.
 * @return the identity, or `null` for a peer whose user the operating system did not report, which kapi then treats
 *   as unverified.
 * @since 1.1.0
 */
public fun IpcPeer.toPeerIdentity(): PeerIdentity? {
    val known = user ?: return null
    return PeerIdentity(user = known.toString(), process = processId)
}
