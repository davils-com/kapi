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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.server.ServerLimits
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readBuffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteArray
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val CHUNK: Int = 16_384

internal class BodySource(
    private val open: suspend () -> ByteReadChannel,
    val declaredLength: Long?,
    val isPresent: Boolean,
    private val coding: ContentCoding,
    private val limits: ServerLimits,
) {
    private val taken = AtomicBoolean(false)

    suspend fun bytes(limit: Long): BodyRead<ByteString> {
        take()
        if (coding == ContentCoding.Identity) return identity(limit)
        return inflated(limit)
    }

    suspend fun stream(limit: Long, consume: suspend (ByteString) -> Unit): BodyRead<Long> {
        take()
        if (coding != ContentCoding.Identity) return streamInflated(limit, consume)
        return streamIdentity(limit, consume)
    }

    private suspend fun streamInflated(limit: Long, consume: suspend (ByteString) -> Unit): BodyRead<Long> =
        when (val inflated = inflated(limit)) {
            is BodyRead.Refused -> {
                inflated
            }

            is BodyRead.Read -> {
                if (inflated.value.size > 0) consume(inflated.value)
                BodyRead.Read(inflated.value.size.toLong())
            }
        }

    private suspend fun streamIdentity(limit: Long, consume: suspend (ByteString) -> Unit): BodyRead<Long> {
        val isDeclaredTooLarge = (declaredLength ?: 0L) > limit
        if (isDeclaredTooLarge) return BodyRead.Refused(BodyRefusal.TooLarge)
        val channel = open()
        val buffer = ByteArray(CHUNK)
        var total = 0L
        var count = channel.readAvailable(buffer = buffer, offset = 0, length = buffer.size)
        while (count >= 0 && total + count <= limit) {
            total += count
            consume(ByteString(data = buffer, startIndex = 0, endIndex = count))
            count = channel.readAvailable(buffer = buffer, offset = 0, length = buffer.size)
        }
        return BodyRead.Read(total).takeIf { count < 0 } ?: BodyRead.Refused(BodyRefusal.TooLarge)
    }

    private suspend fun identity(limit: Long): BodyRead<ByteString> {
        val isDeclaredTooLarge = (declaredLength ?: 0L) > limit
        val bytes = limit.takeUnless { isDeclaredTooLarge }?.let { bound -> raw(bound) }
        val content = bytes?.takeIf { read -> read.size <= limit } ?: return BodyRead.Refused(BodyRefusal.TooLarge)
        return BodyRead.Read(ByteString(content))
    }

    private suspend fun inflated(limit: Long): BodyRead<ByteString> {
        if (coding == ContentCoding.Unsupported) return BodyRead.Refused(BodyRefusal.UnsupportedEncoding)
        return when (val compressed = identity(limits.maxRequestBytes)) {
            is BodyRead.Refused -> compressed
            is BodyRead.Read -> coding.inflate(compressed.value.toByteArray(), minOf(a = limit, b = maxInflated()))
        }
    }

    private fun maxInflated(): Long = limits.maxDecompressedBytes

    private suspend fun raw(limit: Long): ByteArray {
        val channel = open()
        return channel.readBuffer(limit + 1).use { packet -> packet.readByteArray() }
    }

    private fun take() {
        check(taken.compareAndSet(expectedValue = false, newValue = true)) { "The body of a request is read once" }
    }

    companion object {
        fun buffered(content: ByteString, limits: ServerLimits): BodySource = BodySource(
            open = { ByteReadChannel(content.toByteArray()) },
            declaredLength = content.size.toLong(),
            isPresent = content.size > 0,
            coding = ContentCoding.Identity,
            limits = limits,
        )
    }
}
