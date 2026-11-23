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

import com.davils.kapi.runtime.core.framing.ServerSentEvent
import com.davils.kapi.runtime.core.framing.ServerSentEvents
import com.davils.kapi.runtime.server.reply.EventEmitter
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Duration

internal object StreamWriters {
    suspend fun chunks(channel: ByteWriteChannel, chunks: Flow<ByteString>) {
        chunks.collect { chunk ->
            channel.writeFully(chunk.toByteArray())
            channel.flush()
        }
    }

    suspend fun events(channel: ByteWriteChannel, heartbeat: Duration?, produce: suspend EventEmitter.() -> Unit) {
        val emitter = ChannelEmitter(channel)
        coroutineScope {
            val beat = heartbeat?.let { interval ->
                launch {
                    while (true) {
                        delay(interval)
                        emitter.comment()
                    }
                }
            }
            emitter.produce()
            beat?.cancel()
        }
    }

    private class ChannelEmitter(private val channel: ByteWriteChannel) : EventEmitter {
        private val lock = Mutex()

        override suspend fun send(event: ServerSentEvent) {
            write(ServerSentEvents.frame(event))
        }

        override suspend fun comment(text: String) {
            val line = ServerSentEvents.comment(text)
            write(line.encodeToByteString())
        }

        private suspend fun write(bytes: ByteString) {
            lock.withLock {
                channel.writeFully(bytes.toByteArray())
                channel.flush()
            }
        }
    }
}
