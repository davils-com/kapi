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

package com.davils.kapi.runtime.core.codec.internal

import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.internal.Utf8
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.io.encoding.Base64

private const val BREAK: Byte = -1

private val empty: ByteArray = ByteArray(0)

private val constants: Map<Int, JsonElement> = mapOf(
    CborFormat.FALSE to JsonPrimitive(false),
    CborFormat.TRUE to JsonPrimitive(true),
    CborFormat.NULL to JsonNull,
    CborFormat.UNDEFINED to JsonNull,
)

private val floats: Map<Int, (Long) -> Double> = mapOf(
    CborFormat.HALF to CborFloats::half,
    CborFormat.FLOAT to CborFloats::single,
    CborFormat.DOUBLE to CborFloats::double,
)

internal class CborReader(private val bytes: ByteArray, private val maxDepth: Int = DEFAULT_DEPTH) {
    private var position: Int = 0

    private var failure: String? = null

    fun read(): Decoding<JsonElement> {
        val element = item(0)
        if (position != bytes.size) fail("The CBOR item is followed by more bytes")
        val failed = failure?.let { reason -> Decoding.Failed(reason) }
        return failed ?: Decoding.Decoded(element)
    }

    private fun item(depth: Int): JsonElement {
        if (depth > maxDepth) fail("The CBOR item nests deeper than $maxDepth levels")
        if (failure != null) return JsonNull
        val head = head()
        return when (head.major) {
            CborMajor.Unsigned -> JsonPrimitive(whole(head))
            CborMajor.Negative -> JsonPrimitive(-1 - whole(head))
            CborMajor.Binary -> JsonPrimitive(Base64.Default.encode(binary(head)))
            CborMajor.Text -> JsonPrimitive(text(head))
            CborMajor.Items -> JsonArray(each(head, 1) { item(depth + 1) })
            CborMajor.Members -> members(head, depth)
            CborMajor.Tag -> tagged(head, depth)
            CborMajor.Simple -> simple(head)
        }
    }

    private fun head(): CborHead {
        val initial = next()
        return CborHead(CborMajor.entries[initial ushr CborFormat.MAJOR_SHIFT], initial and CborFormat.INFO_MASK)
    }

    private fun whole(head: CborHead): Long {
        val argument = argument(head.info)
        if (argument < 0) return failed("A CBOR integer does not fit 64 signed bits", 0L)
        return argument
    }

    private fun tagged(head: CborHead, depth: Int): JsonElement {
        argument(head.info)
        return item(depth + 1)
    }

    @OptIn(DavilsInternal::class)
    private fun members(head: CborHead, depth: Int): JsonElement {
        val entries = each(head, 2) { key() to item(depth + 1) }
        val members = OrderedMap<String, JsonElement>()
        entries.forEach { (key, value) ->
            if (key in members) fail("A CBOR map repeats a key")
            members[key] = value
        }
        return JsonObject(members.entries.toMap())
    }

    private fun key(): String {
        val head = head()
        if (head.major != CborMajor.Text) return failed(reason = "A CBOR map key is not a text string", fallback = "")
        return text(head)
    }

    private fun simple(head: CborHead): JsonElement {
        val number = floats[head.info]?.let { read -> finite(read(number(CborFormat.widths.getValue(head.info)))) }
        return constants[head.info] ?: number ?: failed("The CBOR simple value ${head.info} is not supported", JsonNull)
    }

    private fun finite(value: Double): JsonElement {
        if (!value.isFinite()) fail("A CBOR number is not finite")
        return JsonPrimitive(value)
    }

    private fun text(head: CborHead): String =
        Utf8.decodeOrNull(binary(head)) ?: failed(reason = "A CBOR text string is no UTF-8", fallback = "")

    private fun binary(head: CborHead): ByteArray {
        if (head.info != CborFormat.INDEFINITE) return take(length(argument(head.info), 1))
        val chunks = until { chunk(head.major) }
        val all = ByteArray(chunks.sumOf(ByteArray::size))
        var offset = 0
        chunks.forEach { chunk ->
            chunk.copyInto(all, offset)
            offset += chunk.size
        }
        return all
    }

    private fun chunk(major: CborMajor): ByteArray {
        val head = head()
        val isChunk = head.major == major && head.info != CborFormat.INDEFINITE
        if (!isChunk) return failed("A CBOR string of indefinite length has a chunk that is none", empty)
        return take(length(argument(head.info), 1))
    }

    private fun <T> each(head: CborHead, width: Int, item: () -> T): List<T> {
        if (head.info == CborFormat.INDEFINITE) return until(item)
        val count = length(argument(head.info), width)
        val items = ArrayList<T>()
        while (items.size < count && failure == null) items += item()
        return items
    }

    private fun <T> until(item: () -> T): List<T> {
        val items = ArrayList<T>()
        while (failure == null && !isBreak()) items += item()
        position += 1
        return items
    }

    private fun isBreak(): Boolean {
        if (position >= bytes.size) return failed(ENDS_EARLY, false)
        return bytes[position] == BREAK
    }

    private fun length(argument: Long, width: Int): Int {
        val remaining = (bytes.size - position).toLong()
        if (argument !in 0..remaining / width) return failed(ENDS_EARLY, 0)
        return argument.toInt()
    }

    private fun argument(info: Int): Long {
        if (info < CborFormat.DIRECT) return info.toLong()
        val width = CborFormat.widths[info]
        return width?.let(::number) ?: failed("The CBOR argument $info is malformed", 0L)
    }

    private fun number(count: Int): Long =
        take(count).fold(0L) { value, byte -> value shl CborFormat.BYTE_BITS or unsigned(byte).toLong() }

    private fun next(): Int {
        val byte = take(1).firstOrNull() ?: return 0
        return unsigned(byte)
    }

    private fun unsigned(byte: Byte): Int = byte.toInt() and CborFormat.BYTE_MASK

    private fun take(count: Int): ByteArray {
        if (count > bytes.size - position) return failed(ENDS_EARLY, empty)
        val chunk = bytes.copyOfRange(fromIndex = position, toIndex = position + count)
        position += count
        return chunk
    }

    private fun <T> failed(reason: String, fallback: T): T {
        fail(reason)
        return fallback
    }

    private fun fail(reason: String) {
        failure = failure ?: reason
    }

    companion object {
        const val DEFAULT_DEPTH: Int = 256

        private const val ENDS_EARLY: String = "The CBOR item ends early"
    }
}
