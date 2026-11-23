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

package com.davils.kapi.runtime.core.framing.internal

import kotlinx.io.bytestring.ByteStringBuilder

private const val CARRIAGE_RETURN: Byte = 0x0D

private const val LINE_FEED: Byte = 0x0A

internal class Splitter(private val delimiters: Set<Byte>, private val maxBytes: Int, private val isCrLfOne: Boolean) {
    private var pending: ByteStringBuilder = ByteStringBuilder()

    private var isAfterCarriageReturn: Boolean = false

    var isOverlong: Boolean = false
        private set

    val hasPending: Boolean get() = pending.size > 0

    fun feed(chunk: ByteArray, onRecord: (ByteArray, Byte) -> Unit) {
        chunk.forEach { byte ->
            val isJoinedLineFeed = isCrLfOne && isAfterCarriageReturn && byte == LINE_FEED
            isAfterCarriageReturn = byte == CARRIAGE_RETURN
            if (!isJoinedLineFeed && !isOverlong) accept(byte, onRecord)
        }
    }

    fun drain(): ByteArray {
        val bytes = pending.toByteString().toByteArray()
        pending = ByteStringBuilder()
        return bytes
    }

    private fun accept(byte: Byte, onRecord: (ByteArray, Byte) -> Unit) {
        if (byte in delimiters) {
            onRecord(drain(), byte)
            return
        }
        pending.append(byte)
        isOverlong = pending.size > maxBytes
    }
}
