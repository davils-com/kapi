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

package com.davils.kapi.runtime.core.codec.multipart

import kotlinx.io.bytestring.ByteString

/**
 * What a [MultipartDecoder] read from a multipart body: the start of a part, a piece of its content, its end, and the
 * end of the body.
 *
 * @since 1.0.0
 */
public sealed interface MultipartEvent {
    /**
     * A part starts; its content follows in [Content] events.
     *
     * @property part the part with its header fields and an empty content.
     * @since 1.0.0
     */
    public class Started(public val part: MultipartPart) : MultipartEvent {
        override fun equals(other: Any?): Boolean = other is Started && part == other.part

        override fun hashCode(): Int = part.hashCode()

        override fun toString(): String = "Started($part)"
    }

    /**
     * A piece of the content of the current part.
     *
     * @property bytes the bytes of the piece.
     * @since 1.0.0
     */
    public class Content(public val bytes: ByteString) : MultipartEvent {
        override fun equals(other: Any?): Boolean = other is Content && bytes == other.bytes

        override fun hashCode(): Int = bytes.hashCode()

        override fun toString(): String = "Content(${bytes.size} bytes)"
    }

    /**
     * The current part ends.
     *
     * @since 1.0.0
     */
    public object Ended : MultipartEvent {
        override fun toString(): String = "Ended"
    }

    /**
     * The closing boundary was read; whatever follows it is ignored.
     *
     * @since 1.0.0
     */
    public object Finished : MultipartEvent {
        override fun toString(): String = "Finished"
    }
}
