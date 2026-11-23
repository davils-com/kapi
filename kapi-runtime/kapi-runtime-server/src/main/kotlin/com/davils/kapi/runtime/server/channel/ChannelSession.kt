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

package com.davils.kapi.runtime.server.channel

import com.davils.kapi.runtime.server.request.Request
import kotlinx.coroutines.flow.Flow

/**
 * A typed WebSocket session of RFC 6455 of a channel of AsyncAPI 3: the messages a server receives and sends.
 *
 * Messages are read only when the handler asks for them, so a slow handler holds back the client through the flow
 * control of TCP; [send] suspends while the client reads slowly. A message that its codec cannot read closes the
 * channel with 1007 (Invalid Frame Payload Data), one that is too large with 1009 (Message Too Big). The session lives
 * in the coroutine of the connection, which Ktor owns. It is safe to send from several coroutines; receive from one.
 *
 * @param Receive the type of the messages the server receives.
 * @param Send the type of the messages the server sends.
 * @since 1.0.0
 */
public interface ChannelSession<Receive : Any, Send : Any> {
    /**
     * The request that opened the channel, with the principal it was authorized as.
     *
     * @since 1.0.0
     */
    public val request: Request

    /**
     * The subprotocol both sides agreed on, or `null` for none.
     *
     * @since 1.0.0
     */
    public val protocol: String?

    /**
     * The messages as they arrive, until the client closes the channel.
     *
     * @since 1.0.0
     */
    public val incoming: Flow<Receive>

    /**
     * Receives the next message.
     *
     * @return the message, or `null` once the channel is closed.
     * @since 1.0.0
     */
    public suspend fun receive(): Receive?

    /**
     * Sends [message].
     *
     * @param message the message.
     * @throws kotlinx.coroutines.channels.ClosedSendChannelException if the channel is closed.
     * @since 1.0.0
     */
    public suspend fun send(message: Send)

    /**
     * Closes the channel with [code] and [reason].
     *
     * @param code the close code of RFC 6455 §7.4; 1000 (Normal Closure) by default.
     * @param reason the reason, at most 123 bytes of UTF-8 without control characters.
     * @throws IllegalArgumentException if [reason] is too long or holds a control character.
     * @since 1.0.0
     */
    public suspend fun close(code: Short = NORMAL, reason: String = "")

    /**
     * The close codes of RFC 6455 §7.4.1 a server sends.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * 1000, Normal Closure.
         *
         * @since 1.0.0
         */
        public const val NORMAL: Short = 1000

        /**
         * 1001, Going Away.
         *
         * @since 1.0.0
         */
        public const val GOING_AWAY: Short = 1001

        /**
         * 1007, Invalid Frame Payload Data: a message its codec cannot read.
         *
         * @since 1.0.0
         */
        public const val INVALID_PAYLOAD: Short = 1007

        /**
         * 1008, Policy Violation.
         *
         * @since 1.0.0
         */
        public const val POLICY_VIOLATION: Short = 1008

        /**
         * 1009, Message Too Big.
         *
         * @since 1.0.0
         */
        public const val TOO_BIG: Short = 1009

        /**
         * 1011, Internal Error: the handler failed.
         *
         * @since 1.0.0
         */
        public const val INTERNAL_ERROR: Short = 1011
    }
}
