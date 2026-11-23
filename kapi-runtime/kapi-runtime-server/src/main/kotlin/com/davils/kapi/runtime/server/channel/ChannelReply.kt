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

package com.davils.kapi.runtime.server.channel

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * How a request the server sent with [ChannelSession.request] ended.
 *
 * @param R the type of the reply.
 * @since 1.0.0
 */
public sealed interface ChannelReply<out R : Any> {
    /**
     * The client replied.
     *
     * @param R the type of the reply.
     * @property message the reply, the first message with the correlation id of the request.
     * @since 1.0.0
     */
    public class Received<out R : Any>(public val message: R) : ChannelReply<R> {
        override fun equals(other: Any?): Boolean = other is Received<*> && message == other.message

        override fun hashCode(): Int = message.hashCode()

        override fun toString(): String = "Received(${SafeText.text(message.toString())})"
    }

    /**
     * No reply came in time.
     *
     * @since 1.0.0
     */
    public object TimedOut : ChannelReply<Nothing> {
        override fun toString(): String = "TimedOut"
    }

    /**
     * The channel closed before a reply came, or the request could not be sent.
     *
     * @since 1.0.0
     */
    public object Closed : ChannelReply<Nothing> {
        override fun toString(): String = "Closed"
    }

    /**
     * The request was not sent: the endpoint has no correlation or the request has no correlation id.
     *
     * @since 1.0.0
     */
    public object Refused : ChannelReply<Nothing> {
        override fun toString(): String = "Refused"
    }
}
