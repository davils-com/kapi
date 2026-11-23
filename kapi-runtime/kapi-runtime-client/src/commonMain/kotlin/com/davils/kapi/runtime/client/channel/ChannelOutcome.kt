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

import com.davils.kapi.runtime.client.call.CallFailure

/**
 * How a channel opened with `channel` ended.
 *
 * @param T the type of the value of the block.
 * @since 1.0.0
 */
public sealed interface ChannelOutcome<out T> {
    /**
     * The channel opened, the block ran to its end and the channel closed.
     *
     * @param T the type of the value.
     * @property value what the block returned.
     * @property close how the channel closed.
     * @since 1.0.0
     */
    public class Completed<out T>(public val value: T, public val close: ChannelClose) : ChannelOutcome<T> {
        override fun equals(other: Any?): Boolean =
            other is Completed<*> && value == other.value && close == other.close

        override fun hashCode(): Int = close.hashCode()

        override fun toString(): String = "Completed($close)"
    }

    /**
     * The channel did not open.
     *
     * @property failure why.
     * @since 1.0.0
     */
    public class Failed(public val failure: CallFailure) : ChannelOutcome<Nothing> {
        override fun equals(other: Any?): Boolean = other is Failed && failure == other.failure

        override fun hashCode(): Int = failure.hashCode()

        override fun toString(): String = "Failed($failure)"
    }
}
