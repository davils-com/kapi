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

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

/**
 * What a channel of an AsyncAPI document carries: the messages the client sends and receives, the settings of the
 * channel and how requests pair with replies.
 *
 * A spec is immutable and safe to share.
 *
 * @param S the type of the messages the client sends.
 * @param R the type of the messages the client receives.
 * @property sendSerializer the serializer of a sent message.
 * @property receiveDeserializer the deserializer of a received message.
 * @property settings the settings of the channel.
 * @property correlation how requests pair with replies, or `null` for a channel without requests.
 * @since 1.0.0
 */
public class ChannelSpec<S, R : Any>(
    public val sendSerializer: SerializationStrategy<S>,
    public val receiveDeserializer: DeserializationStrategy<R>,
    public val settings: ChannelSettings = ChannelSettings.Default,
    public val correlation: Correlation<S, R>? = null,
) {
    override fun toString(): String = "ChannelSpec($settings)"
}
