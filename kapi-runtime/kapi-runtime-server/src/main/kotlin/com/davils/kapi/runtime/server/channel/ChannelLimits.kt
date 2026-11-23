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

import com.davils.kapi.runtime.server.ServerViolationCodes
import com.davils.kapi.runtime.server.internal.bounded
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.time.Duration

/**
 * The limits of a WebSocket channel of RFC 6455.
 *
 * A frame larger than [maxFrameBytes] closes the channel with the code 1009 (Message Too Big) before it is buffered,
 * and so does a message whose frames together exceed [maxMessageBytes]. The server pings the client every
 * [pingInterval] and closes a channel whose client does not answer within [timeout]. Every limit has a hard ceiling.
 * The limits are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class ChannelLimits internal constructor(builder: ChannelLimitsBuilder) : Validatable {
    /**
     * The most bytes of one frame, at most [HARD_FRAME_BYTES].
     *
     * @since 1.0.0
     */
    public val maxFrameBytes: Int = builder.maxFrameBytes

    /**
     * The most bytes of one message, its frames together, at most [HARD_MESSAGE_BYTES].
     *
     * @since 1.0.0
     */
    public val maxMessageBytes: Int = builder.maxMessageBytes

    /**
     * The time between two pings of the server.
     *
     * @since 1.0.0
     */
    public val pingInterval: Duration = builder.pingInterval

    /**
     * How long the server waits for a pong or a close before it gives up on the client.
     *
     * @since 1.0.0
     */
    public val timeout: Duration = builder.timeout

    override fun ValidationScope.validate() {
        bounded(::maxFrameBytes, HARD_FRAME_BYTES.toLong())
        bounded(::maxMessageBytes, HARD_MESSAGE_BYTES.toLong())
        if (!pingInterval.isPositive()) fail(::pingInterval, POSITIVE, ServerViolationCodes.DURATION_INVALID)
        if (!timeout.isPositive()) fail(::timeout, POSITIVE, ServerViolationCodes.DURATION_INVALID)
    }

    override fun toString(): String = "ChannelLimits(frameBytes=$maxFrameBytes, messageBytes=$maxMessageBytes, " +
        "pingInterval=$pingInterval, timeout=$timeout)"

    /**
     * The default limits and the hard ceilings.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val POSITIVE: String = "must be positive"

        /**
         * The hard ceiling of [maxFrameBytes], 16 MiB.
         *
         * @since 1.0.0
         */
        public const val HARD_FRAME_BYTES: Int = 16 * 1_024 * 1_024

        /**
         * The hard ceiling of [maxMessageBytes], 64 MiB.
         *
         * @since 1.0.0
         */
        public const val HARD_MESSAGE_BYTES: Int = 64 * 1_024 * 1_024

        /**
         * The default limits: 64 KiB per frame, 1 MiB per message, a ping every 30 seconds and a timeout of 15.
         *
         * @since 1.0.0
         */
        public val Default: ChannelLimits = ChannelLimitsBuilder().build()
    }
}
