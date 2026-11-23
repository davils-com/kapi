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

private const val LINE: String = "\n"

private const val RECORD_SEPARATOR: Char = '\u001E'

/**
 * Writes JSON Lines and newline-delimited JSON, and JSON text sequences of RFC 7464.
 *
 * A record of JSON Lines is the compact text of one JSON value followed by a line feed, so it cannot hold a line
 * break; a record of a JSON text sequence starts with the record separator `0x1E` and ends with a line feed, so it
 * cannot hold a record separator. Both writers refuse such a record rather than split it. The functions keep no
 * state and are safe to call from any thread.
 *
 * @since 1.0.0
 */
public object JsonLines {
    /**
     * Frames [record] as one line of JSON Lines.
     *
     * @param record the compact text of one JSON value.
     * @return the record with its line feed, in UTF-8.
     * @throws IllegalArgumentException if [record] holds a carriage return or a line feed.
     * @since 1.0.0
     */
    public fun frame(record: String): ByteString {
        require('\n' !in record && '\r' !in record) { "A record of JSON Lines holds no line break" }
        return (record + LINE).encodeToByteString()
    }

    /**
     * Frames [record] as one record of a JSON text sequence of RFC 7464.
     *
     * @param record the text of one JSON value.
     * @return the record between its separator and its line feed, in UTF-8.
     * @throws IllegalArgumentException if [record] holds a record separator.
     * @since 1.0.0
     */
    public fun frameSequence(record: String): ByteString {
        require(RECORD_SEPARATOR !in record) { "A record of a JSON text sequence holds no record separator" }
        return (RECORD_SEPARATOR + record + LINE).encodeToByteString()
    }
}
