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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configures [ChannelLimits]; every limit starts at its default.
 *
 * A builder is not thread-safe; the limits it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ChannelLimitsBuilder internal constructor() : ValidatingBuilder<ChannelLimits>() {
    override val subject: String get() = "channel limits"

    /**
     * See [ChannelLimits.maxFrameBytes].
     *
     * @since 1.0.0
     */
    public var maxFrameBytes: Int = DEFAULT_FRAME_BYTES

    /**
     * See [ChannelLimits.maxMessageBytes].
     *
     * @since 1.0.0
     */
    public var maxMessageBytes: Int = DEFAULT_MESSAGE_BYTES

    /**
     * See [ChannelLimits.pingInterval].
     *
     * @since 1.0.0
     */
    public var pingInterval: Duration = DEFAULT_PING_SECONDS.seconds

    /**
     * See [ChannelLimits.timeout].
     *
     * @since 1.0.0
     */
    public var timeout: Duration = DEFAULT_TIMEOUT_SECONDS.seconds

    /**
     * See [ChannelLimits.maxHeldMessages].
     *
     * @since 1.0.0
     */
    public var maxHeldMessages: Int = DEFAULT_HELD_MESSAGES

    override fun assemble(): ChannelLimits = ChannelLimits(this)

    private companion object {
        const val DEFAULT_FRAME_BYTES: Int = 64 * 1_024
        const val DEFAULT_MESSAGE_BYTES: Int = 1_024 * 1_024
        const val DEFAULT_PING_SECONDS: Int = 30
        const val DEFAULT_TIMEOUT_SECONDS: Int = 15
        const val DEFAULT_HELD_MESSAGES: Int = 64
    }
}

/**
 * Builds the limits of a WebSocket channel.
 *
 * @param configure changes the limits from their defaults.
 * @return the limits.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit is out of range.
 * @since 1.0.0
 */
public fun channelLimits(configure: ChannelLimitsBuilder.() -> Unit = {}): ChannelLimits {
    val builder = ChannelLimitsBuilder()
    builder.configure()
    return builder.build()
}
