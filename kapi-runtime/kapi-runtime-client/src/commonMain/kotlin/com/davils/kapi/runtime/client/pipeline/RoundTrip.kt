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

package com.davils.kapi.runtime.client.pipeline

import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.internal.Granted
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * A call on its way through the phases of a client and how it ended so far.
 *
 * A round trip is immutable; [withCall] and [withExchange] make changed copies. [toString] names the call, the
 * exchange and the schemes whose secrets it holds, never a secret.
 *
 * @since 1.0.0
 */
public class RoundTrip internal constructor(
    /**
     * The call as it will be sent, with its credentials once the client placed them.
     *
     * @since 1.0.0
     */
    public val call: Call,
    /**
     * How the call ended, or `null` while it was neither sent nor answered by an interceptor.
     *
     * @since 1.0.0
     */
    public val exchange: Exchange? = null,
    internal val granted: Granted = Granted(),
) {
    /**
     * The secret the client placed for each scheme of the call, such as to sign it; empty before
     * [CallPhase.Authorize].
     *
     * @since 1.0.0
     */
    public val secrets: Map<String, Secret> get() = granted.secrets

    /**
     * Returns this round trip going on with [call].
     *
     * @param call the changed call.
     * @return the changed round trip.
     * @since 1.0.0
     */
    public fun withCall(call: Call): RoundTrip = RoundTrip(call = call, exchange = exchange, granted = granted)

    /**
     * Returns this round trip ended with [exchange], which the transport then does not send.
     *
     * @param exchange how the call ended.
     * @return the changed round trip.
     * @since 1.0.0
     */
    public fun withExchange(exchange: Exchange): RoundTrip =
        RoundTrip(call = call, exchange = exchange, granted = granted)

    override fun toString(): String =
        "RoundTrip($call, ${exchange ?: "not sent"}, schemes=${secrets.keys.map(SafeText::text)})"
}
