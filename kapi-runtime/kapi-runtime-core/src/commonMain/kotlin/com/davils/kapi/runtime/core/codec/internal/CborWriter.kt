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

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.ByteStringBuilder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

private val limits: List<Long> = listOf(0x100L, 0x10000L, 0x100000000L)

private val flags: Map<Boolean, Int> = mapOf(false to CborFormat.FALSE, true to CborFormat.TRUE)

internal class CborWriter {
    private val out: ByteStringBuilder = ByteStringBuilder()

    fun write(element: JsonElement): ByteString {
        element(element)
        return out.toByteString()
    }

    private fun element(element: JsonElement) {
        when (element) {
            is JsonNull -> out.append(CborFormat.initial(CborMajor.Simple, CborFormat.NULL))
            is JsonObject -> members(element)
            is JsonArray -> items(element)
            is JsonPrimitive -> primitive(element)
        }
    }

    private fun members(members: JsonObject) {
        head(CborMajor.Members, members.size.toLong())
        members.forEach { (key, value) ->
            text(key)
            element(value)
        }
    }

    private fun items(items: JsonArray) {
        head(CborMajor.Items, items.size.toLong())
        items.forEach(::element)
    }

    private fun primitive(primitive: JsonPrimitive) {
        val literal = primitive.takeUnless(JsonPrimitive::isString)
        literal?.let(::flag) ?: literal?.let(::whole) ?: literal?.let(::real) ?: text(primitive.content)
    }

    private fun flag(literal: JsonPrimitive): Unit? =
        literal.booleanOrNull?.let { flag -> out.append(CborFormat.initial(CborMajor.Simple, flags.getValue(flag))) }

    private fun whole(literal: JsonPrimitive): Unit? = literal.longOrNull?.let { whole ->
        val isNegative = whole < 0
        val major = CborMajor.Negative.takeIf { isNegative } ?: CborMajor.Unsigned
        head(major, (-1 - whole).takeIf { isNegative } ?: whole)
    }

    private fun real(literal: JsonPrimitive): Unit? = literal.doubleOrNull?.takeIf(Double::isFinite)?.let { real ->
        out.append(CborFormat.initial(CborMajor.Simple, CborFormat.DOUBLE))
        bytes(real.toRawBits(), Long.SIZE_BYTES)
    }

    private fun text(text: String) {
        val content = text.encodeToByteArray()
        head(CborMajor.Text, content.size.toLong())
        out.append(content)
    }

    private fun head(major: CborMajor, argument: Long) {
        if (argument < CborFormat.DIRECT) {
            out.append(CborFormat.initial(major, argument.toInt()))
            return
        }
        val position = limits.indexOfFirst { limit -> argument < limit }
        val index = position.takeIf { found -> found >= 0 } ?: limits.size
        out.append(CborFormat.initial(major, CborFormat.DIRECT + index))
        bytes(argument, 1 shl index)
    }

    private fun bytes(value: Long, count: Int) {
        for (index in count - 1 downTo 0) out.append((value ushr index * CborFormat.BYTE_BITS).toByte())
    }
}
