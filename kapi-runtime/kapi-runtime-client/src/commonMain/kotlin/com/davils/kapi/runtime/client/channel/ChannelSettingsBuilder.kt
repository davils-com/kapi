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

import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configures [ChannelSettings]; every limit has a safe default.
 *
 * A builder is not thread-safe; the settings it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ChannelSettingsBuilder internal constructor() : ValidatingBuilder<ChannelSettings>() {
    override val subject: String get() = "channel settings"

    /**
     * The subprotocols the client offers in `Sec-WebSocket-Protocol`, in the order it prefers them.
     *
     * @since 1.0.0
     */
    public var subprotocols: List<String> = emptyList()

    /**
     * The media type of the messages, which picks the codec: text frames for text and JSON, binary frames for
     * anything else; JSON by default.
     *
     * @since 1.0.0
     */
    public var mediaType: MediaType = MediaType.Json

    /**
     * The largest frame the client reads, in bytes; 1 MiB by default.
     *
     * @since 1.0.0
     */
    public var maxFrameBytes: Long = DEFAULT_FRAME_BYTES

    /**
     * The largest message the client assembles from its frames, in bytes; 4 MiB by default.
     *
     * @since 1.0.0
     */
    public var maxMessageBytes: Long = DEFAULT_MESSAGE_BYTES

    /**
     * The time between two pings that keep the channel alive; zero for none; 30 seconds by default.
     *
     * @since 1.0.0
     */
    public var pingInterval: Duration = 30.seconds

    /**
     * How long the client waits for a pong before it closes the channel; 15 seconds by default.
     *
     * @since 1.0.0
     */
    public var pongTimeout: Duration = 15.seconds

    /**
     * How long the client waits for the close frame of the peer after its own; 5 seconds by default.
     *
     * @since 1.0.0
     */
    public var closeTimeout: Duration = 5.seconds

    /**
     * The messages the client holds for a slow collector of [ChannelSession.incoming] before it stops reading, which
     * passes the backpressure on to the peer; 64 by default.
     *
     * @since 1.0.0
     */
    public var incomingCapacity: Int = DEFAULT_CAPACITY

    /**
     * The most requests that wait for their replies at once; 256 by default.
     *
     * @since 1.0.0
     */
    public var maxPendingRequests: Int = DEFAULT_PENDING

    override fun assemble(): ChannelSettings = ChannelSettings(this)

    private companion object {
        const val DEFAULT_FRAME_BYTES: Long = 1_024L * 1_024

        const val DEFAULT_MESSAGE_BYTES: Long = 4L * 1_024 * 1_024

        const val DEFAULT_CAPACITY: Int = 64

        const val DEFAULT_PENDING: Int = 256
    }
}

/**
 * Builds the settings of a channel.
 *
 * @param configure changes the settings from their defaults.
 * @return the settings.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit or a time is out of its bounds or a
 * subprotocol is no token.
 * @since 1.0.0
 */
public fun channelSettings(configure: ChannelSettingsBuilder.() -> Unit = {}): ChannelSettings {
    val builder = ChannelSettingsBuilder()
    builder.configure()
    return builder.build()
}
