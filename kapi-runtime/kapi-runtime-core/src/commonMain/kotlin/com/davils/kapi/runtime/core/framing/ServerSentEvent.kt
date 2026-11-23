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

package com.davils.kapi.runtime.core.framing

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val MESSAGE: String = "message"

/**
 * An event of a `text/event-stream` of the WHATWG HTML standard §9.2.
 *
 * The [type] and the [id] are written on one line each and can hold no line break; the [id] can hold no NUL either,
 * which a reader would ignore. The [data] may hold line breaks, which the writer splits into several `data` lines. An
 * event is immutable; [toString] shows its texts escaped.
 *
 * @property data the data of the event.
 * @property type the event type; `message` unless the stream names another.
 * @property id the last event ID of the stream when the event was dispatched, or `null` if there is none.
 * @property retry the reconnection time in milliseconds the stream set before the event, or `null`.
 * @throws IllegalArgumentException if [type] or [id] holds a carriage return or a line feed, [id] holds a NUL, or
 * [retry] is negative.
 * @since 1.0.0
 */
public class ServerSentEvent(
    public val data: String,
    public val type: String = MESSAGE,
    public val id: String? = null,
    public val retry: Long? = null,
) {
    init {
        require(isLine(type) && (id == null || (isLine(id) && '\u0000' !in id))) {
            "The type and the ID of an event hold no line break and the ID no NUL"
        }
        require(retry == null || retry >= 0) { "A reconnection time is not negative" }
    }

    override fun equals(other: Any?): Boolean = other is ServerSentEvent && data == other.data &&
        type == other.type && id == other.id && retry == other.retry

    override fun hashCode(): Int = listOf(data, type, id, retry).hashCode()

    override fun toString(): String {
        val shownId = id?.let(SafeText::text)
        return "ServerSentEvent(type=${SafeText.text(type)}, id=$shownId, retry=$retry, data=${SafeText.text(data)})"
    }

    internal companion object {
        fun isLine(text: String): Boolean = '\r' !in text && '\n' !in text
    }
}
