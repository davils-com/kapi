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

import com.davils.kapi.runtime.client.ClientViolationCodes
import com.davils.kapi.runtime.client.internal.FieldText
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * The settings of a WebSocket channel of RFC 6455.
 *
 * Settings are refused when a subprotocol is no token, a limit is not above zero or above its hard ceiling, or a time
 * is negative, infinite or above an hour. Settings are immutable and safe to share.
 *
 * @since 1.0.0
 */
public class ChannelSettings internal constructor(builder: ChannelSettingsBuilder) : Validatable {
    /**
     * The subprotocols the client offers.
     *
     * @since 1.0.0
     */
    public val subprotocols: List<String> = builder.subprotocols.toList()

    /**
     * The media type of the messages.
     *
     * @since 1.0.0
     */
    public val mediaType: MediaType = builder.mediaType

    /**
     * The largest frame the client reads.
     *
     * @since 1.0.0
     */
    public val maxFrameBytes: Long = builder.maxFrameBytes

    /**
     * The largest message the client assembles.
     *
     * @since 1.0.0
     */
    public val maxMessageBytes: Long = builder.maxMessageBytes

    /**
     * The time between two pings, or zero for none.
     *
     * @since 1.0.0
     */
    public val pingInterval: Duration = builder.pingInterval

    /**
     * How long the client waits for a pong.
     *
     * @since 1.0.0
     */
    public val pongTimeout: Duration = builder.pongTimeout

    /**
     * How long the client waits for the close frame of the peer.
     *
     * @since 1.0.0
     */
    public val closeTimeout: Duration = builder.closeTimeout

    /**
     * The messages the client holds for a slow collector.
     *
     * @since 1.0.0
     */
    public val incomingCapacity: Int = builder.incomingCapacity

    /**
     * The most requests that wait at once.
     *
     * @since 1.0.0
     */
    public val maxPendingRequests: Int = builder.maxPendingRequests

    override fun ValidationScope.validate() {
        if (subprotocols.any { protocol -> !FieldText.isToken(protocol) }) {
            fail(::subprotocols, "must each be a token", ClientViolationCodes.LIMIT_INVALID)
        }
        if (maxFrameBytes !in 1..HARD_FRAME_BYTES || maxMessageBytes !in 1..HARD_MESSAGE_BYTES) {
            fail(::maxFrameBytes, "must be above zero and within the hard ceilings", ClientViolationCodes.LIMIT_INVALID)
        }
        if (incomingCapacity !in 1..HARD_COUNT || maxPendingRequests !in 1..HARD_COUNT) {
            fail(::incomingCapacity, "must be from 1 to $HARD_COUNT", ClientViolationCodes.LIMIT_INVALID)
        }
        val times = listOf(pingInterval, pongTimeout, closeTimeout)
        val isTimeValid = times.all { time -> !time.isNegative() && time <= HARD_TIME } && pongTimeout.isPositive()
        if (!isTimeValid || !closeTimeout.isPositive()) {
            fail(::pingInterval, "must not be negative nor above an hour", ClientViolationCodes.TIMEOUT_INVALID)
        }
    }

    override fun toString(): String = "ChannelSettings(subprotocols=${subprotocols.map(SafeText::text)}, " +
        "mediaType=$mediaType, maxFrameBytes=$maxFrameBytes, maxMessageBytes=$maxMessageBytes)"

    /**
     * The defaults and hard ceilings of the settings.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The largest frame any channel reads: 64 MiB.
         *
         * @since 1.0.0
         */
        public const val HARD_FRAME_BYTES: Long = 64L * 1_024 * 1_024

        /**
         * The largest message any channel assembles: 256 MiB.
         *
         * @since 1.0.0
         */
        public const val HARD_MESSAGE_BYTES: Long = 256L * 1_024 * 1_024

        /**
         * The most messages held and requests waiting in any channel.
         *
         * @since 1.0.0
         */
        public const val HARD_COUNT: Int = 65_536

        /**
         * The longest time of any channel: one hour.
         *
         * @since 1.0.0
         */
        public val HARD_TIME: Duration = 1.hours

        /**
         * The settings of a channel the application does not configure.
         *
         * @since 1.0.0
         */
        public val Default: ChannelSettings = ChannelSettingsBuilder().build()
    }
}
