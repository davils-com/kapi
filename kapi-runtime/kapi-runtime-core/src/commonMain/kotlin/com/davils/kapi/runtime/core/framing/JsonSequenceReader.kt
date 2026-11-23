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

private const val RECORD_SEPARATOR: Byte = 0x1E

private const val LINE_FEED: Byte = 0x0A

/**
 * Splits a JSON text sequence of RFC 7464 (`application/json-seq`) into its records as the chunks of the stream
 * arrive.
 *
 * Every record starts with the record separator `0x1E` and ends with a line feed. Bytes before the first separator
 * and empty records are skipped. A record that does not end with a line feed was cut off by its writer: RFC 7464 §2.3
 * forbids reading it as complete, so it is skipped, counted in [truncatedRecords], and the records after it are read
 * on. A record longer than [FramingLimits.maxRecordBytes] or not in UTF-8 fails the stream, and the reader stays
 * failed. A reader holds the state of one stream and is not safe to share between threads.
 *
 * @param limits the limits of the stream.
 * @since 1.0.0
 */
public class JsonSequenceReader(limits: FramingLimits = FramingLimits.Default) {
    private val splitter: Splitter = Splitter(setOf(RECORD_SEPARATOR), limits.maxRecordBytes, isCrLfOne = false)

    private var failure: FramingFailure? = null

    private var isStarted: Boolean = false

    /**
     * How many records were skipped because they did not end with a line feed.
     *
     * @since 1.0.0
     */
    public var truncatedRecords: Int = 0
        private set

    /**
     * Reads the next chunk of the stream.
     *
     * @param chunk the bytes that arrived, which may come from an attacker.
     * @return the records the chunk completed, without separator and line feed, or the failure of the stream.
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
     * @return the last record, or the failure of the stream.
     * @since 1.0.0
     */
    public fun finish(): Outcome<List<String>, FramingFailure> {
        val records = ArrayList<String>()
        if (failure == null) record(splitter.drain(), records)
        return outcome(records)
    }

    private fun record(bytes: ByteArray, records: MutableList<String>) {
        val hasStarted = isStarted
        isStarted = true
        if (!hasStarted || bytes.isEmpty()) return
        val isComplete = bytes.last() == LINE_FEED
        if (!isComplete) truncatedRecords += 1
        val text = Utf8.decodeOrNull(bytes.copyOf(bytes.size - 1)).takeIf { isComplete }
        if (isComplete && text == null) failure = failure ?: FramingFailure.NotUtf8
        text?.let(records::add)
    }

    private fun outcome(records: List<String>): Outcome<List<String>, FramingFailure> {
        val failed = failure ?: return Outcome.Success(records)
        return Outcome.Failure(failed)
    }
}
