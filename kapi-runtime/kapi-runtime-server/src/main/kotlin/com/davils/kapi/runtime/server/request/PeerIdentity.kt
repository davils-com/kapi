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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val NONE: String = "none"

/**
 * The identity of a peer that the transport verified by other means than a certificate, such as the credentials of
 * a Unix domain socket, handed over in [com.davils.kapi.runtime.server.PeerAttributes.identity].
 *
 * It stands in for a credential a request does not present, and only the
 * [Authorizer][com.davils.kapi.runtime.server.security.Authorizer] of a scheme decides what it may do. A transport
 * sets it only for a peer it has verified.
 *
 * @property user the name of the user of the peer.
 * @property process the id of the process of the peer, or `null` if the transport does not know it.
 * @since 1.0.0
 */
public class PeerIdentity(public val user: String, public val process: Long? = null) {
    private val parts: List<Any?> get() = listOf(user, process)

    override fun equals(other: Any?): Boolean = other is PeerIdentity && parts == other.parts

    override fun hashCode(): Int = parts.hashCode()

    @OptIn(DavilsInternal::class)
    override fun toString(): String = "PeerIdentity(${SafeText.text(user)}, ${process ?: NONE})"
}
