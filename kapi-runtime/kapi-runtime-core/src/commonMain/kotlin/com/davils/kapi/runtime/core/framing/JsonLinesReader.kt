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

import com.davils.kapi.runtime.core.framing.internal.Splitter
import com.davils.kapi.runtime.core.internal.Utf8
import com.davils.kore.core.outcome.Outcome
import kotlinx.io.bytestring.ByteString

private const val LINE_FEED: Byte = 0x0A

private const val CARRIAGE_RETURN: Byte = 0x0D

/**
 * Splits a stream of JSON Lines (`application/jsonl`) or newline-delimited JSON (`application/x-ndjson`) into its
 * records as the chunks of the stream arrive.
 *
 * Records end with a line feed; a carriage return before it is dropped, and so are blank lines. The last record may
 * end without a line feed, which [finish] hands out. Each record is the text of one JSON value, still to be decoded by
 * a codec. A record longer than [FramingLimits.maxRecordBytes] or not in UTF-8 fails the stream, and the reader stays
 * failed. A reader holds the state of one stream and is not safe to share between threads.
 *
 * @param limits the limits of the stream.
 * @since 1.0.0
 */
public class JsonLinesReader(limits: FramingLimits = FramingLimits.Default) {
    private val splitter: Splitter = Splitter(setOf(LINE_FEED), limits.maxRecordBytes, isCrLfOne = false)

    private var failure: FramingFailure? = null

    /**
     * Reads the next chunk of the stream.
     *
     * @param chunk the bytes that arrived, which may come from an attacker.
     * @return the records the chunk completed, or the failure of the stream.
     * @since 1.0.0
     */
    public fun feed(chunk: ByteString): Outcome<List<String>, FramingFailure> {
        val records = ArrayList<String>()
        if (failure == null) splitter.feed(chunk.toByteArray()) { bytes, _ -> record(bytes, records) }
        if (splitter.isOverlong) failure = failure ?: FramingFailure.RecordTooLong
        return outcome(records)
    }

    /**
     * Ends the stream.
     *
     * @return the last record if it ended without a line feed, or the failure of the stream.
     * @since 1.0.0
     */
    public fun finish(): Outcome<List<String>, FramingFailure> {
        val records = ArrayList<String>()
        if (failure == null) record(splitter.drain(), records)
        return outcome(records)
    }

    private fun record(bytes: ByteArray, records: MutableList<String>) {
        val content = bytes.copyOf(bytes.size - (1.takeIf { bytes.lastOrNull() == CARRIAGE_RETURN } ?: 0))
        val text = Utf8.decodeOrNull(content)
        if (text == null) failure = failure ?: FramingFailure.NotUtf8
        if (text != null && text.isNotBlank() && failure == null) records += text
    }

    private fun outcome(records: List<String>): Outcome<List<String>, FramingFailure> {
        val failed = failure ?: return Outcome.Success(records)
        return Outcome.Failure(failed)
    }
}
