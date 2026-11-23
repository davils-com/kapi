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

package com.davils.kapi.runtime.core.framing

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val MESSAGE: String = "message"

private const val LINE: String = "\n"

/**
 * Writes a `text/event-stream` of the WHATWG HTML standard §9.2.
 *
 * Every field is one line ended by a line feed, an event ends with an empty line, and data with line breaks is
 * written as one `data` line per line, so the reader of [ServerSentEventReader] gets it back as it was. Nothing a
 * caller passes can end an event early or inject a field: the type and the ID of a [ServerSentEvent] hold no line
 * break, and a comment with one is refused. The functions keep no state and are safe to call from any thread.
 *
 * @since 1.0.0
 */
public object ServerSentEvents {
    /**
     * Writes [event] as text.
     *
     * @param event the event; its type is written unless it is `message`, its ID and reconnection time when set.
     * @return the lines of the event with the empty line that dispatches it.
     * @since 1.0.0
     */
    public fun encode(event: ServerSentEvent): String {
        val out = StringBuilder()
        if (event.type != MESSAGE) out.append("event: ${event.type}$LINE")
        event.id?.let { id -> out.append("id: $id$LINE") }
        event.retry?.let { milliseconds -> out.append("retry: $milliseconds$LINE") }
        event.data.split("\r\n", "\r", "\n").forEach { line -> out.append("data: $line$LINE") }
        out.append(LINE)
        return out.toString()
    }

    /**
     * Writes [event] as UTF-8.
     *
     * @param event the event.
     * @return the bytes of [encode].
     * @since 1.0.0
     */
    public fun frame(event: ServerSentEvent): ByteString = encode(event).encodeToByteString()

    /**
     * Writes a comment, such as a heartbeat that keeps a connection open.
     *
     * @param text the text of the comment.
     * @return the comment line.
     * @throws IllegalArgumentException if [text] holds a carriage return or a line feed.
     * @since 1.0.0
     */
    public fun comment(text: String = ""): String {
        require(ServerSentEvent.isLine(text)) { "A comment holds no line break" }
        return ":$text$LINE"
    }
}
