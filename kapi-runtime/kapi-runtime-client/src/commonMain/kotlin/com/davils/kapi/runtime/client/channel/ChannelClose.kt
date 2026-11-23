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

package com.davils.kapi.runtime.client.channel

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val HASH_FACTOR: Int = 31

/**
 * How a channel closed: the code and the reason of the close frame, or 1006 when the connection ended without one.
 *
 * A close is immutable. [toString] shows the reason, which may come from the peer, through `SafeText`.
 *
 * @property code the close code.
 * @property reason the reason, empty when there is none.
 * @property isLocal whether the client closed the channel, such as after its block or for a message too large.
 * @since 1.0.0
 */
public class ChannelClose(public val code: Int, public val reason: String = "", public val isLocal: Boolean = false) {
    /**
     * The kind of [code].
     *
     * @since 1.0.0
     */
    public val kind: CloseKind get() = CloseKind.of(code)

    override fun equals(other: Any?): Boolean =
        other is ChannelClose && code == other.code && reason == other.reason && isLocal == other.isLocal

    override fun hashCode(): Int = (code * HASH_FACTOR + reason.hashCode()) * HASH_FACTOR + isLocal.hashCode()

    override fun toString(): String = "ChannelClose($code, $kind, ${SafeText.text(reason)}, isLocal=$isLocal)"
}
