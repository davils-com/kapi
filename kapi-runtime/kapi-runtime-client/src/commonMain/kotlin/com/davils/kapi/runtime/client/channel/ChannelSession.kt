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

package com.davils.kapi.runtime.client.channel

import com.davils.kapi.runtime.client.internal.ChannelEngine
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * An open WebSocket channel of an AsyncAPI document: the typed messages the peer sends, and sending, requests and
 * closing.
 *
 * [incoming] holds at most [ChannelSettings.incomingCapacity] messages for its collector; while it is full, the client
 * stops reading, which passes the backpressure on to the peer through the transport. A frame larger than
 * [ChannelSettings.maxFrameBytes] or a message larger than [ChannelSettings.maxMessageBytes] closes the channel with
 * 1009, and a message the codec cannot read with 1007; nothing is cut or skipped silently. A reply to a request goes
 * to the request, not to [incoming]. A session is safe to use from several coroutines inside its block and must not
 * be used after it.
 *
 * @param S the type of the messages the client sends.
 * @param R the type of the messages the client receives.
 * @param engine the engine that reads and writes the frames of the channel.
 * @property subprotocol the subprotocol the server chose from those the client offered, or `null`.
 * @since 1.0.0
 */
public class ChannelSession<S, R : Any> internal constructor(
    private val engine: ChannelEngine<S, R>,
    public val subprotocol: String?,
) {
    /**
     * The messages the peer sends that are no reply to a request, in their order; the flow ends when the channel
     * closes. It can be collected once.
     *
     * @since 1.0.0
     */
    public val incoming: Flow<R> get() = engine.incoming

    /**
     * Sends [message], suspending while the transport cannot take it.
     *
     * @param message the message.
     * @return `false` if the codec cannot write it or the channel is closed.
     * @since 1.0.0
     */
    public suspend fun send(message: S): Boolean = engine.send(message)

    /**
     * Sends [message] and waits for the first message with its correlation id.
     *
     * @param message the request.
     * @param timeout how long to wait for the reply.
     * @return the reply, or why there is none.
     * @since 1.0.0
     */
    public suspend fun request(message: S, timeout: Duration): ChannelReply<R> = engine.request(message, timeout)

    /**
     * Closes the channel with [code] and [reason]; [incoming] ends once the peer answers.
     *
     * @param code the close code: 1000 or one of 3000 to 4999.
     * @param reason the reason, at most 123 bytes of UTF-8.
     * @throws IllegalArgumentException if [code] or [reason] cannot be sent.
     * @since 1.0.0
     */
    public suspend fun close(code: Int = CloseKind.Normal.code, reason: String = "") {
        engine.close(code, reason)
    }

    override fun toString(): String = "ChannelSession(${subprotocol ?: "no subprotocol"})"
}
