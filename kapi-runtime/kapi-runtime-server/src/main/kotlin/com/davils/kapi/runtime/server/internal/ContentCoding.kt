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

package com.davils.kapi.runtime.server.internal

import com.davils.kore.core.failure.contained
import kotlinx.io.bytestring.ByteString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

private const val CHUNK: Int = 8_192

internal enum class ContentCoding {
    Identity,
    Gzip,
    Deflate,
    Unsupported,
    ;

    fun inflate(compressed: ByteArray, limit: Long): BodyRead<ByteString> {
        val inflated = contained { streamOf(compressed).use { input -> readBounded(input, limit) } }
        val bytes = inflated.getOrNull() ?: return BodyRead.Refused(BodyRefusal.MalformedEncoding)
        return bytes.takeIf { content -> content.size <= limit }?.let { content -> BodyRead.Read(ByteString(content)) }
            ?: BodyRead.Refused(BodyRefusal.TooLarge)
    }

    private fun streamOf(compressed: ByteArray): InputStream {
        val source = ByteArrayInputStream(compressed)
        if (this == Gzip) return GZIPInputStream(source)
        return InflaterInputStream(source)
    }

    private fun readBounded(input: InputStream, limit: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(CHUNK)
        var count = input.read(buffer)
        while (count >= 0 && out.size() <= limit) {
            out.write(buffer, 0, count)
            count = input.read(buffer)
        }
        return out.toByteArray()
    }

    companion object {
        private val codings: Map<String, ContentCoding> = mapOf(
            "" to Identity,
            "identity" to Identity,
            "gzip" to Gzip,
            "x-gzip" to Gzip,
            "deflate" to Deflate,
        )

        fun of(header: String): ContentCoding {
            val coding = header.trim().lowercase()
            return codings[coding] ?: Unsupported
        }
    }
}
