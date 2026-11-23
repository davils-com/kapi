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

import com.davils.kapi.runtime.core.codec.internal.ByteWindow
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.Utf8
import com.davils.kore.core.outcome.Outcome
import kotlinx.io.bytestring.ByteString

private val lineBreak: ByteArray = byteArrayOf(0x0D, 0x0A)

private val headerEnd: ByteArray = byteArrayOf(0x0D, 0x0A, 0x0D, 0x0A)

private val closing: ByteArray = byteArrayOf(0x2D, 0x2D)

private const val SPACE: Byte = 0x20

private const val TAB: Byte = 0x09

private const val MOST_PADDING: Int = 64

private const val COLON: Char = ':'

/**
 * Reads a multipart body of RFC 2046 §5.1, such as an upload of `multipart/form-data` of RFC 7578, as its chunks
 * arrive, so a server never holds a whole upload in memory.
 *
 * The decoder hands out the start of every part with its header fields, its content in pieces as they become certain
 * not to begin a boundary, its end and the end of the body. It is strict: a boundary is recognised only at the start
 * of a line, and one that is followed by anything other than `--`, blanks and a line break fails the body rather
 * than being read as content; a folded header line, a header name that is no token, a control character in a header
 * value and header octets that are no UTF-8 fail it as well. A part whose content is itself multipart, with a
 * boundary of its own, is handed out as opaque content. Every limit of [MultipartLimits] is checked before more is
 * buffered; the decoder buffers at most a header block, the preamble and the length of a delimiter. Bytes after the
 * closing boundary are ignored. A decoder that failed stays failed. A decoder holds the state of one body and is not
 * safe to share between threads.
 *
 * @param boundary the boundary of the body, from the `boundary` parameter of its media type.
 * @param limits the limits of the body.
 * @since 1.0.0
 */
public class MultipartDecoder(boundary: Boundary, private val limits: MultipartLimits = MultipartLimits.Default) {
    private val delimiter: ByteArray = lineBreak + "--${boundary.value}".encodeToByteArray()

    private val window: ByteWindow = ByteWindow()

    private var stage: Stage = Stage.Preamble

    private var failure: MultipartFailure? = null

    private var parts: Int = 0

    private var partBytes: Long = 0L

    private var bodyBytes: Long = 0L

    private var preambleBytes: Long = -lineBreak.size.toLong()

    init {
        window.append(lineBreak)
    }

    /**
     * Reads the next chunk of the body.
     *
     * @param chunk the bytes that arrived, which may come from an attacker.
     * @return the events the chunk completed, or the failure of the body.
     * @since 1.0.0
     */
    public fun feed(chunk: ByteString): Outcome<List<MultipartEvent>, MultipartFailure> {
        val events = ArrayList<MultipartEvent>()
        bodyBytes += chunk.size
        if (bodyBytes > limits.maxBodyBytes) fail(MultipartFailure.BodyTooLarge)
        if (failure == null && stage != Stage.Epilogue) {
            window.append(chunk.toByteArray())
            process(events)
        }
        return outcome(events)
    }

    /**
     * Ends the body.
     *
     * @return no further events, [MultipartFailure.Truncated] if the closing boundary was not read, or the failure of
     * the body.
     * @since 1.0.0
     */
    public fun finish(): Outcome<List<MultipartEvent>, MultipartFailure> {
        if (stage != Stage.Epilogue) fail(MultipartFailure.Truncated)
        return outcome(emptyList())
    }

    private fun process(events: MutableList<MultipartEvent>) {
        var isProgressing = true
        while (isProgressing && failure == null) {
            isProgressing = when (stage) {
                Stage.Preamble -> preamble()
                Stage.Delimiter -> delimiter(events)
                Stage.Headers -> headers(events)
                Stage.Body -> body(events)
                Stage.Epilogue -> false
            }
        }
    }

    private fun preamble(): Boolean {
        val index = window.indexOf(delimiter)
        val skipped = index.takeIf { found -> found >= 0 } ?: maxOf(a = 0, b = window.size - delimiter.size + 1)
        preambleBytes += skipped
        window.drop(skipped)
        if (preambleBytes > limits.maxPreambleBytes) fail(MultipartFailure.PreambleTooLong)
        if (index < 0) return false
        window.drop(delimiter.size)
        stage = Stage.Delimiter
        return true
    }

    private fun delimiter(events: MutableList<MultipartEvent>): Boolean {
        if (window.size < closing.size) return false
        if (window.startsWith(closing)) {
            events += MultipartEvent.Finished
            stage = Stage.Epilogue
            window.drop(window.size)
        }
        return stage != Stage.Epilogue && afterPadding()
    }

    private fun afterPadding(): Boolean {
        var padding = 0
        while (padding < window.size && (window[padding] == SPACE || window[padding] == TAB)) padding += 1
        val isBreak = padding <= MOST_PADDING && window.startsWith(lineBreak, padding)
        val isPending = !isBreak && padding <= MOST_PADDING && isBreakAhead(padding)
        if (!isBreak && !isPending) fail(MultipartFailure.MalformedDelimiter)
        if (!isBreak) return false
        window.drop(padding + lineBreak.size)
        parts += 1
        if (parts > limits.maxParts) fail(MultipartFailure.TooManyParts)
        stage = Stage.Headers
        return true
    }

    private fun isBreakAhead(from: Int): Boolean =
        window.size == from || (window.size == from + 1 && window[from] == lineBreak[0])

    private fun headers(events: MutableList<MultipartEvent>): Boolean {
        val block = headerBlock() ?: return false
        val fields = fieldsOf(block)
        fields?.let { found -> events += MultipartEvent.Started(MultipartPart(found, ByteString())) }
        partBytes = 0L
        stage = Stage.Body
        return fields != null
    }

    private fun headerBlock(): ByteArray? {
        if (window.startsWith(lineBreak)) return window.take(0).also { window.drop(lineBreak.size) }
        val end = window.indexOf(headerEnd)
        val isTooLong = (end < 0 && window.size > limits.maxHeaderBytes) || end > limits.maxHeaderBytes
        if (isTooLong) fail(MultipartFailure.HeadersTooLong)
        val block = window.takeIf { end >= 0 && !isTooLong }?.take(end)
        if (block != null) window.drop(headerEnd.size)
        return block
    }

    private fun fieldsOf(block: ByteArray): List<Pair<String, String>>? {
        val text = Utf8.decodeOrNull(block)
        val nonEmpty = text?.takeIf(String::isNotEmpty)
        val lines = nonEmpty?.split("\r\n").orEmpty()
        val fields = lines.map(::fieldOf)
        val isValid = text != null && fields.all { field -> field != null }
        if (!isValid) fail(MultipartFailure.MalformedHeader)
        if (isValid && fields.size > limits.maxHeaders) fail(MultipartFailure.TooManyHeaders)
        return fields.filterNotNull().takeIf { failure == null }
    }

    private fun fieldOf(line: String): Pair<String, String>? {
        val name = line.substringBefore(COLON, "")
        val value = FieldGrammar.trimmed(line.substringAfter(COLON, ""))
        val isValid = FieldGrammar.isToken(name) && value.none(FieldGrammar::isControl)
        return (name to value).takeIf { isValid }
    }

    private fun body(events: MutableList<MultipartEvent>): Boolean {
        val index = window.indexOf(delimiter)
        val certain = index.takeIf { found -> found >= 0 } ?: maxOf(a = 0, b = window.size - delimiter.size + 1)
        if (certain > 0) content(window.take(certain), events)
        if (index < 0 || failure != null) return false
        window.drop(delimiter.size)
        events += MultipartEvent.Ended
        stage = Stage.Delimiter
        return true
    }

    private fun content(bytes: ByteArray, events: MutableList<MultipartEvent>) {
        partBytes += bytes.size
        if (partBytes > limits.maxPartBytes) fail(MultipartFailure.PartTooLarge)
        if (failure == null) events += MultipartEvent.Content(ByteString(bytes))
    }

    private fun fail(reason: MultipartFailure) {
        failure = failure ?: reason
    }

    private fun outcome(events: List<MultipartEvent>): Outcome<List<MultipartEvent>, MultipartFailure> {
        val failed = failure ?: return Outcome.Success(events)
        return Outcome.Failure(failed)
    }

    private enum class Stage {
        Preamble,
        Delimiter,
        Headers,
        Body,
        Epilogue,
    }
}
