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

package com.davils.kapi.runtime.client.internal

import kotlinx.io.Buffer
import kotlinx.io.readByteArray

private const val LINE_FEED: Byte = 0x0A

private const val CARRIAGE_RETURN: String = "\r"

internal class Lines(private val limit: Int) {
    private val pending: Buffer = Buffer()

    var isOverLimit: Boolean = false
        private set

    fun accept(chunk: ByteArray, count: Int = chunk.size): List<String> {
        val complete = mutableListOf<String>()
        var index = 0
        while (index < count && !isOverLimit) {
            val byte = chunk[index]
            if (byte == LINE_FEED) complete += drained()
            if (byte != LINE_FEED) keep(byte)
            index += 1
        }
        return complete
    }

    fun rest(): List<String> {
        if (pending.size == 0L || isOverLimit) return emptyList()
        return listOf(drained())
    }

    private fun keep(byte: Byte) {
        pending.writeByte(byte)
        if (pending.size > limit) isOverLimit = true
    }

    private fun drained(): String {
        val text = pending.readByteArray().decodeToString()
        return text.removeSuffix(CARRIAGE_RETURN)
    }
}
