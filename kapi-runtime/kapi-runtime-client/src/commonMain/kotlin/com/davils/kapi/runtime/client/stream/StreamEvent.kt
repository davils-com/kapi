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

package com.davils.kapi.runtime.client.stream

import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val HASH_FACTOR: Int = 31

private const val NONE: String = "none"

private const val NULL: String = "null"

/**
 * One element of a stream: an item the server sent, an item that is not what the document declares, and at the end
 * how the call ended.
 *
 * @param T the type of the items.
 * @since 1.0.0
 */
public sealed interface StreamEvent<out T> {
    /**
     * An item of the stream.
     *
     * @param T the type of the item.
     * @property value the item.
     * @property id the ID of a server-sent event, or `null`.
     * @property type the type of a server-sent event, or `null`.
     * @since 1.0.0
     */
    public class Item<out T>(public val value: T, public val id: String? = null, public val type: String? = null) :
        StreamEvent<T> {
        override fun equals(other: Any?): Boolean =
            other is Item<*> && value == other.value && id == other.id && type == other.type

        override fun hashCode(): Int = (value.hashCode() * HASH_FACTOR + id.hashCode()) * HASH_FACTOR + type.hashCode()

        override fun toString(): String =
            "Item(${SafeText.text(value?.toString() ?: NULL)}, id=${id?.let(SafeText::text) ?: NONE}, " +
                "type=${type?.let(SafeText::text) ?: NONE})"
    }

    /**
     * An item the client could not read as the type the document declares; the stream goes on.
     *
     * @property reason why, without the content of the item.
     * @since 1.0.0
     */
    public class Malformed(public val reason: String) : StreamEvent<Nothing> {
        override fun equals(other: Any?): Boolean = other is Malformed && reason == other.reason

        override fun hashCode(): Int = reason.hashCode()

        override fun toString(): String = "Malformed(${SafeText.text(reason)})"
    }

    /**
     * The end of the stream: the answer of the server with an empty content once the stream was read, the answer of
     * a server that refused with its content, or why there was none.
     *
     * @property exchange how the call ended.
     * @since 1.0.0
     */
    public class Ended(public val exchange: Exchange) : StreamEvent<Nothing> {
        override fun equals(other: Any?): Boolean = other is Ended && exchange == other.exchange

        override fun hashCode(): Int = exchange.hashCode()

        override fun toString(): String = "Ended($exchange)"
    }
}
