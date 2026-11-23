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

package com.davils.kapi.runtime.client.call

import kotlin.time.Duration

private const val HASH_FACTOR: Int = 31

private const val NONE: String = "none"

/**
 * The time a call may take: the whole request, the connection and the pause between two reads.
 *
 * A part that is `null` is left to the client, then to the engine. The time of the whole request does not apply to a
 * stream, which may stay open as long as the server sends; the time of a read does.
 *
 * @property request the time of the whole request, its redirects included.
 * @property connect the time to connect, handed to the engine.
 * @property socket the longest pause between two reads or writes, handed to the engine.
 * @since 1.0.0
 */
public class Timeout(
    public val request: Duration? = null,
    public val connect: Duration? = null,
    public val socket: Duration? = null,
) {
    internal val parts: List<Duration> get() = listOfNotNull(request, connect, socket)

    internal fun under(fallback: Timeout): Timeout = Timeout(
        request = request ?: fallback.request,
        connect = connect ?: fallback.connect,
        socket = socket ?: fallback.socket,
    )

    override fun equals(other: Any?): Boolean =
        other is Timeout && request == other.request && connect == other.connect && socket == other.socket

    override fun hashCode(): Int =
        (request.hashCode() * HASH_FACTOR + connect.hashCode()) * HASH_FACTOR + socket.hashCode()

    override fun toString(): String =
        "Timeout(request=${request ?: NONE}, connect=${connect ?: NONE}, socket=${socket ?: NONE})"
}
