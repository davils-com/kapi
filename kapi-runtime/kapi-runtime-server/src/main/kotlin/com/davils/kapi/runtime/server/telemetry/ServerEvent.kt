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

package com.davils.kapi.runtime.server.telemetry

import com.davils.konfetti.core.Event
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.time.Duration

private const val NONE: String = "none"

/**
 * An event of a server on the topic [ServerTopics.EVENTS]: a request served, a handler failed, a channel opened or
 * closed.
 *
 * An event names the operation or channel as the document does and never carries what a request or a message held:
 * no parameter, no header, no body and no message of a failure, only the simple name of its type. Events are
 * immutable.
 *
 * @since 1.0.0
 */
public sealed interface ServerEvent : Event {
    /**
     * A request of an operation was answered, with a reply of its handler or a problem of the server.
     *
     * @property operation the name of the operation.
     * @property method the method of the operation.
     * @property status the status of the answer.
     * @property duration how long the answer took until its body was handed to Ktor.
     * @since 1.0.0
     */
    public class Served internal constructor(
        public val operation: String,
        public val method: String,
        public val status: Int,
        public val duration: Duration,
    ) : ServerEvent {
        @OptIn(DavilsInternal::class)
        override fun toString(): String = "Served(${SafeText.text(operation)}, $method, $status, $duration)"
    }

    /**
     * The handler of an operation or a channel threw; the caller got a problem with status 500 or the close code
     * 1011.
     *
     * @property operation the name of the operation or the channel.
     * @property failureType the simple name of the type of the failure, never its message.
     * @since 1.0.0
     */
    public class Failed internal constructor(public val operation: String, public val failureType: String) :
        ServerEvent {
        @OptIn(DavilsInternal::class)
        override fun toString(): String = "Failed(${SafeText.text(operation)}, ${SafeText.text(failureType)})"
    }

    /**
     * A WebSocket channel was opened.
     *
     * @property channel the name of the channel.
     * @property protocol the negotiated subprotocol, or `null` for none.
     * @since 1.0.0
     */
    public class ChannelOpened internal constructor(public val channel: String, public val protocol: String?) :
        ServerEvent {
        @OptIn(DavilsInternal::class)
        override fun toString(): String =
            "ChannelOpened(${SafeText.text(channel)}, ${protocol?.let(SafeText::text) ?: NONE})"
    }

    /**
     * A WebSocket channel was closed.
     *
     * @property channel the name of the channel.
     * @property code the close code the server sent or would have sent, such as 1000 or 1011.
     * @property duration how long the channel was open.
     * @since 1.0.0
     */
    public class ChannelClosed internal constructor(
        public val channel: String,
        public val code: Int,
        public val duration: Duration,
    ) : ServerEvent {
        @OptIn(DavilsInternal::class)
        override fun toString(): String = "ChannelClosed(${SafeText.text(channel)}, $code, $duration)"
    }
}
